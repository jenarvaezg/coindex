use std::collections::{BTreeMap, BTreeSet};
use std::sync::Arc;

use async_trait::async_trait;
use domain::CollectedItem as DomainItem;
use numista::{CollectedItemsResponse, NumistaClient, NumistaType, SyncCallEstimate, SyncPlan};
use serde::Serialize;
use thiserror::Error;

use crate::config::UserConfig;
use crate::repository::{Repository, RepositoryError};

#[derive(Clone)]
pub struct SyncService {
    repository: Arc<dyn SyncRepository>,
    clients: BTreeMap<String, Arc<dyn NumistaApi>>,
}

#[derive(Clone, Debug, Serialize)]
pub struct SyncReport {
    pub dry_run: bool,
    pub collection_items: usize,
    pub missing_type_ids: Vec<u32>,
    pub calls: SyncCallProjection,
}

#[derive(Clone, Copy, Debug, Serialize)]
#[serde(tag = "precision", rename_all = "snake_case")]
pub enum SyncCallProjection {
    LowerBound {
        oauth_token: u32,
        collected_items: u32,
        local_snapshot_missing_type_metadata: u32,
        minimum_total: u32,
        unknown_remote_type_metadata: bool,
    },
    Estimated {
        oauth_token: u32,
        collected_items: u32,
        type_metadata: u32,
        total: u32,
    },
}

#[derive(Debug, Error)]
pub enum SyncError {
    #[error("Numista client is not configured for `{0}`")]
    MissingClient(String),
    #[error(transparent)]
    Repository(#[from] RepositoryError),
    #[error("Numista request failed: {0}")]
    Numista(#[from] numista::ClientError),
    #[error("Numista collection response omitted the required `items` field")]
    MissingCollectionItems,
}

impl SyncCallProjection {
    fn lower_bound(value: SyncCallEstimate, local_snapshot_missing: usize) -> Self {
        Self::LowerBound {
            oauth_token: value.oauth_token,
            collected_items: value.collected_items,
            local_snapshot_missing_type_metadata: local_snapshot_missing as u32,
            minimum_total: value.total,
            unknown_remote_type_metadata: true,
        }
    }
}

impl SyncService {
    pub fn new(repository: Repository, clients: BTreeMap<String, Arc<dyn NumistaApi>>) -> Self {
        Self {
            repository: Arc::new(repository),
            clients,
        }
    }

    pub async fn run(&self, user: &UserConfig, dry_run: bool) -> Result<SyncReport, SyncError> {
        let client = self
            .clients
            .get(&user.key)
            .ok_or_else(|| SyncError::MissingClient(user.key.clone()))?;

        if dry_run {
            let items = self.repository.load_items(&user.key).await?;
            let missing_type_ids = self
                .repository
                .missing_type_ids(items.iter().map(|item| item.type_id).collect())
                .await?;
            let estimate = client
                .estimate_sync_calls(&SyncPlan {
                    fetch_collection: true,
                    missing_type_ids: Vec::new(),
                })
                .await;
            let local_snapshot_missing = missing_type_ids.len();
            return Ok(SyncReport {
                dry_run: true,
                collection_items: items.len(),
                missing_type_ids,
                calls: SyncCallProjection::lower_bound(estimate, local_snapshot_missing),
            });
        }

        let collection_estimate = client
            .estimate_sync_calls(&SyncPlan {
                fetch_collection: true,
                missing_type_ids: Vec::new(),
            })
            .await;
        let response = client.fetch_collected_items(user.numista_user_id).await?;
        let raw_items = response
            .raw_json()
            .and_then(|raw| raw.get("items"))
            .and_then(serde_json::Value::as_array)
            .cloned();
        let items = response.items.ok_or(SyncError::MissingCollectionItems)?;
        let type_ids = items
            .iter()
            .filter_map(|item| item.item_type.as_ref().and_then(|item_type| item_type.id))
            .collect::<BTreeSet<_>>();
        let mut fetched_type_ids = Vec::new();
        for type_id in type_ids {
            if self
                .repository
                .fetch_and_cache_type(type_id, client.as_ref())
                .await?
            {
                fetched_type_ids.push(type_id);
            }
        }
        self.repository
            .store_sync(&user.key, &items, raw_items.as_deref())
            .await?;

        Ok(SyncReport {
            dry_run: false,
            collection_items: items.len(),
            missing_type_ids: fetched_type_ids.clone(),
            calls: SyncCallProjection::Estimated {
                oauth_token: collection_estimate.oauth_token,
                collected_items: 1,
                type_metadata: fetched_type_ids.len() as u32,
                total: collection_estimate.oauth_token + 1 + fetched_type_ids.len() as u32,
            },
        })
    }
}

#[async_trait]
pub(crate) trait NumistaApi: Send + Sync {
    async fn estimate_sync_calls(&self, plan: &SyncPlan) -> SyncCallEstimate;
    async fn fetch_collected_items(
        &self,
        user_id: u64,
    ) -> Result<CollectedItemsResponse, numista::ClientError>;
    async fn fetch_type_metadata(&self, type_id: u32) -> Result<NumistaType, numista::ClientError>;
}

#[async_trait]
impl NumistaApi for NumistaClient {
    async fn estimate_sync_calls(&self, plan: &SyncPlan) -> SyncCallEstimate {
        NumistaClient::estimate_sync_calls(self, plan).await
    }

    async fn fetch_collected_items(
        &self,
        user_id: u64,
    ) -> Result<CollectedItemsResponse, numista::ClientError> {
        NumistaClient::fetch_collected_items(self, user_id).await
    }

    async fn fetch_type_metadata(&self, type_id: u32) -> Result<NumistaType, numista::ClientError> {
        NumistaClient::fetch_type_metadata(self, type_id).await
    }
}

#[async_trait]
trait SyncRepository: Send + Sync {
    async fn load_items(&self, user_key: &str) -> Result<Vec<DomainItem>, RepositoryError>;
    async fn missing_type_ids(&self, type_ids: Vec<u32>) -> Result<Vec<u32>, RepositoryError>;
    async fn fetch_and_cache_type(
        &self,
        type_id: u32,
        client: &dyn NumistaApi,
    ) -> Result<bool, SyncError>;
    async fn store_sync(
        &self,
        user_key: &str,
        items: &[numista::CollectedItem],
        raw_items: Option<&[serde_json::Value]>,
    ) -> Result<(), RepositoryError>;
}

#[async_trait]
impl SyncRepository for Repository {
    async fn load_items(&self, user_key: &str) -> Result<Vec<DomainItem>, RepositoryError> {
        Repository::load_items(self, user_key).await
    }

    async fn missing_type_ids(&self, type_ids: Vec<u32>) -> Result<Vec<u32>, RepositoryError> {
        Repository::missing_type_ids(self, type_ids).await
    }

    async fn fetch_and_cache_type(
        &self,
        type_id: u32,
        client: &dyn NumistaApi,
    ) -> Result<bool, SyncError> {
        let Some(claim) = self.claim_type_fetch(type_id).await? else {
            return Ok(false);
        };
        let metadata = client.fetch_type_metadata(type_id).await?;
        claim.cache(&metadata).await?;
        Ok(true)
    }

    async fn store_sync(
        &self,
        user_key: &str,
        items: &[numista::CollectedItem],
        raw_items: Option<&[serde_json::Value]>,
    ) -> Result<(), RepositoryError> {
        Repository::store_sync(self, user_key, items, raw_items).await
    }
}

#[cfg(test)]
mod tests {
    use std::collections::{BTreeMap, BTreeSet};
    use std::sync::Arc;
    use std::sync::atomic::{AtomicUsize, Ordering};

    use async_trait::async_trait;
    use numista::{
        CollectedItem, CollectedItemsResponse, ItemType, NumistaType, SyncCallEstimate, SyncPlan,
    };
    use tokio::sync::Mutex;

    use super::{NumistaApi, SyncRepository, SyncService};
    use crate::config::UserConfig;
    use crate::repository::RepositoryError;

    struct MemoryStore {
        items: Mutex<Vec<domain::CollectedItem>>,
        cached_types: Mutex<BTreeSet<u32>>,
    }

    struct FakeNumista {
        metadata_calls: AtomicUsize,
        collection_calls: AtomicUsize,
        type_ids: Vec<u32>,
        omit_items: bool,
        fail_type: Option<u32>,
    }

    #[async_trait]
    impl SyncRepository for MemoryStore {
        async fn load_items(
            &self,
            _user_key: &str,
        ) -> Result<Vec<domain::CollectedItem>, RepositoryError> {
            Ok(self.items.lock().await.clone())
        }

        async fn missing_type_ids(&self, type_ids: Vec<u32>) -> Result<Vec<u32>, RepositoryError> {
            let cached = self.cached_types.lock().await;
            Ok(type_ids
                .into_iter()
                .collect::<BTreeSet<_>>()
                .into_iter()
                .filter(|id| !cached.contains(id))
                .collect())
        }

        async fn fetch_and_cache_type(
            &self,
            type_id: u32,
            client: &dyn NumistaApi,
        ) -> Result<bool, super::SyncError> {
            let mut cached = self.cached_types.lock().await;
            if cached.contains(&type_id) {
                return Ok(false);
            }
            let metadata = client.fetch_type_metadata(type_id).await?;
            cached.insert(metadata.id.unwrap());
            Ok(true)
        }

        async fn store_sync(
            &self,
            _user_key: &str,
            items: &[CollectedItem],
            _raw_items: Option<&[serde_json::Value]>,
        ) -> Result<(), RepositoryError> {
            *self.items.lock().await = items
                .iter()
                .map(|item| domain::CollectedItem {
                    id: item.id.unwrap(),
                    quantity: item.quantity.unwrap(),
                    type_id: item.item_type.as_ref().unwrap().id.unwrap(),
                    title: None,
                    issuer_code: None,
                    issue_year: None,
                    gregorian_year: None,
                    grade: None,
                    price: None,
                    for_swap: None,
                    collection_name: None,
                })
                .collect();
            Ok(())
        }
    }

    #[async_trait]
    impl NumistaApi for FakeNumista {
        async fn estimate_sync_calls(&self, plan: &SyncPlan) -> SyncCallEstimate {
            SyncCallEstimate {
                oauth_token: 0,
                collected_items: u32::from(plan.fetch_collection),
                type_metadata: plan.missing_type_ids.len() as u32,
                total: u32::from(plan.fetch_collection) + plan.missing_type_ids.len() as u32,
            }
        }

        async fn fetch_collected_items(
            &self,
            _user_id: u64,
        ) -> Result<CollectedItemsResponse, numista::ClientError> {
            self.collection_calls.fetch_add(1, Ordering::SeqCst);
            Ok(CollectedItemsResponse {
                items: (!self.omit_items).then(|| {
                    self.type_ids
                        .iter()
                        .enumerate()
                        .map(|(index, type_id)| CollectedItem {
                            id: Some(index as u64 + 7),
                            quantity: Some(1),
                            item_type: Some(ItemType {
                                id: Some(*type_id),
                                ..ItemType::default()
                            }),
                            ..CollectedItem::default()
                        })
                        .collect()
                }),
                ..CollectedItemsResponse::default()
            })
        }

        async fn fetch_type_metadata(
            &self,
            type_id: u32,
        ) -> Result<NumistaType, numista::ClientError> {
            self.metadata_calls.fetch_add(1, Ordering::SeqCst);
            if self.fail_type == Some(type_id) {
                return Err(numista::ClientError::EmptyApiKey);
            }
            Ok(NumistaType {
                id: Some(type_id),
                ..NumistaType::default()
            })
        }
    }

    fn setup(
        type_ids: Vec<u32>,
        omit_items: bool,
        fail_type: Option<u32>,
    ) -> (SyncService, Arc<MemoryStore>, Arc<FakeNumista>, UserConfig) {
        let store = Arc::new(MemoryStore {
            items: Mutex::new(Vec::new()),
            cached_types: Mutex::new(BTreeSet::new()),
        });
        let client = Arc::new(FakeNumista {
            metadata_calls: AtomicUsize::new(0),
            collection_calls: AtomicUsize::new(0),
            type_ids,
            omit_items,
            fail_type,
        });
        let mut clients: BTreeMap<String, Arc<dyn NumistaApi>> = BTreeMap::new();
        clients.insert("jose".to_owned(), client.clone());
        let service = SyncService {
            repository: store.clone(),
            clients,
        };
        let user = UserConfig {
            key: "jose".to_owned(),
            numista_user_id: 123,
            api_key: "unused".to_owned(),
        };
        (service, store, client, user)
    }

    #[tokio::test]
    async fn second_sync_uses_cached_type_metadata_without_another_call() {
        let (service, _store, client, user) = setup(vec![42], false, None);

        let first = service.run(&user, false).await.unwrap();
        let second = service.run(&user, false).await.unwrap();

        assert!(matches!(
            first.calls,
            super::SyncCallProjection::Estimated {
                type_metadata: 1,
                ..
            }
        ));
        assert!(matches!(
            second.calls,
            super::SyncCallProjection::Estimated {
                type_metadata: 0,
                ..
            }
        ));
        assert_eq!(client.metadata_calls.load(Ordering::SeqCst), 1);
    }

    #[tokio::test]
    async fn concurrent_syncs_claim_each_type_only_once() {
        let (service, _store, client, user) = setup(vec![42], false, None);

        let (left, right) = tokio::join!(service.run(&user, false), service.run(&user, false));

        left.unwrap();
        right.unwrap();
        assert_eq!(client.metadata_calls.load(Ordering::SeqCst), 1);
    }

    #[tokio::test]
    async fn missing_items_field_never_clears_existing_snapshot() {
        let (service, store, _client, user) = setup(Vec::new(), true, None);
        store.items.lock().await.push(domain::CollectedItem {
            id: 9,
            quantity: 1,
            type_id: 90,
            title: None,
            issuer_code: None,
            issue_year: None,
            gregorian_year: None,
            grade: None,
            price: None,
            for_swap: None,
            collection_name: None,
        });

        let error = service.run(&user, false).await.unwrap_err();

        assert!(matches!(error, super::SyncError::MissingCollectionItems));
        assert_eq!(store.items.lock().await[0].id, 9);
    }

    #[tokio::test]
    async fn successful_metadata_is_cached_before_a_later_type_fails() {
        let (service, store, _client, user) = setup(vec![41, 42], false, Some(42));

        assert!(service.run(&user, false).await.is_err());

        let cached = store.cached_types.lock().await;
        assert!(cached.contains(&41));
        assert!(!cached.contains(&42));
    }

    #[tokio::test]
    async fn dry_run_json_is_an_explicit_lower_bound_without_exact_total() {
        let (service, store, client, user) = setup(Vec::new(), false, None);
        store.items.lock().await.push(domain::CollectedItem {
            id: 9,
            quantity: 1,
            type_id: 90,
            title: None,
            issuer_code: None,
            issue_year: None,
            gregorian_year: None,
            grade: None,
            price: None,
            for_swap: None,
            collection_name: None,
        });

        let report = service.run(&user, true).await.unwrap();
        let json = serde_json::to_value(report).unwrap();

        assert_eq!(json["calls"]["precision"], "lower_bound");
        assert_eq!(json["calls"]["unknown_remote_type_metadata"], true);
        assert!(json["calls"].get("minimum_total").is_some());
        assert!(json["calls"].get("total").is_none());
        assert_eq!(client.collection_calls.load(Ordering::SeqCst), 0);
    }
}
