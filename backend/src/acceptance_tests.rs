use std::collections::BTreeMap;
use std::sync::Arc;
use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use axum::body::Body;
use axum::http::header::{CONTENT_TYPE, ORIGIN};
use axum::http::{Request, StatusCode};
use domain::{
    Album, CollectionCatalogId, CollectionProposalKey, MatchSource, ProposalDisposition, SlotStatus,
};
use http_body_util::BodyExt;
use numista::{
    ClientConfig, CollectedItem, HttpRequest, HttpResponse, HttpTransport, ItemType, NumistaClient,
    TransportError,
};
use serde_json::json;
use sqlx::PgPool;
use sqlx::postgres::PgPoolOptions;
use tower::ServiceExt;

use crate::repository::{PostgresCallPolicy, Repository};
use crate::seeds::{load_collection_catalogs, load_series};
use crate::sync::{NumistaApi, SyncService};
use crate::{AppConfig, AppState, build_router};

const USER_KEY: &str = "jose";
const USER_ID: u64 = 8_000_000_001;
const ITEM_ID: u64 = 8_000_000_002;
const TYPE_ID: u32 = 2_000_000_001;
const ORIGIN_URL: &str = "http://localhost:8000";
const DESTRUCTIVE_TEST_ACK: &str = "COINDEX_ALLOW_DESTRUCTIVE_DB_TEST";

struct FakeNumistaTransport {
    metadata_calls: AtomicUsize,
}

#[async_trait]
impl HttpTransport for FakeNumistaTransport {
    async fn execute(&self, request: HttpRequest) -> Result<HttpResponse, TransportError> {
        let body = match request.path() {
            "/oauth_token" => json!({
                "access_token": "acceptance-token",
                "expires_in": 7_200
            }),
            path if path == format!("/users/{USER_ID}/collected_items") => json!({
                "items": [{
                    "id": ITEM_ID,
                    "quantity": 1,
                    "type": {
                        "id": TYPE_ID,
                        "title": "Acceptance unmatched item"
                    }
                }]
            }),
            path if path == format!("/types/{TYPE_ID}") => {
                self.metadata_calls.fetch_add(1, Ordering::SeqCst);
                json!({
                    "id": TYPE_ID,
                    "title": "Acceptance metadata"
                })
            }
            path => panic!("unexpected fake Numista request: {path}"),
        };
        Ok(HttpResponse {
            status: 200,
            body: serde_json::to_vec(&body).unwrap(),
        })
    }
}

/// Run with:
/// `COINDEX_ALLOW_DESTRUCTIVE_DB_TEST=1
/// TEST_DATABASE_URL=postgres://.../coindex_test_acceptance cargo test -p coindex-backend
/// postgres_override_and_metadata_cache_survive_consecutive_syncs -- --ignored`
///
/// The guard accepts only an explicitly acknowledged loopback database named
/// `coindex_verify` or prefixed `coindex_test_`. CI must provide an isolated,
/// migrated Postgres database and opt into ignored tests.
#[tokio::test]
#[ignore = "requires disposable Postgres in TEST_DATABASE_URL; see test doc comment"]
async fn postgres_override_and_metadata_cache_survive_consecutive_syncs() {
    let database_url = std::env::var("TEST_DATABASE_URL").expect("TEST_DATABASE_URL must be set");
    assert_disposable_database_url(&database_url);
    let pool = PgPoolOptions::new()
        .max_connections(4)
        .connect(&database_url)
        .await
        .unwrap();
    sqlx::migrate!("./migrations").run(&pool).await.unwrap();

    clean_acceptance_rows(&pool).await;
    let repository = Repository::new(pool.clone());
    repository
        .store_sync(
            USER_KEY,
            &[CollectedItem {
                id: Some(ITEM_ID),
                quantity: Some(1),
                item_type: Some(ItemType {
                    id: Some(TYPE_ID),
                    title: Some("Acceptance unmatched item".to_owned()),
                    ..ItemType::default()
                }),
                ..CollectedItem::default()
            }],
            None,
        )
        .await
        .unwrap();

    let policy = Arc::new(PostgresCallPolicy::new(pool.clone(), 100));
    let transport = Arc::new(FakeNumistaTransport {
        metadata_calls: AtomicUsize::new(0),
    });
    let client = Arc::new(
        NumistaClient::with_transport(
            ClientConfig::new("fake-api-key"),
            transport.clone(),
            policy.clone(),
            policy,
        )
        .unwrap(),
    );
    let mut clients: BTreeMap<String, Arc<dyn NumistaApi>> = BTreeMap::new();
    clients.insert(USER_KEY.to_owned(), client);

    let config = AppConfig::parse(
        &format!("{USER_KEY}:{USER_ID}:fake-api-key,padre:2:other-key"),
        Some("100"),
        ORIGIN_URL,
    )
    .unwrap();
    let series = Arc::new(load_series().unwrap());
    let slot_id = series[0].slots[0].id.to_string();
    let app = build_router(AppState {
        config,
        repository: repository.clone(),
        series,
        collection_catalogs: Arc::new(Vec::new()),
        sync: SyncService::new(repository.clone(), clients),
        image_client: reqwest::Client::new(),
    });

    let before_override = get_album(app.clone()).await;
    assert_eq!(before_override.unmatched[0].item_id, ITEM_ID);

    let override_response = app
        .clone()
        .oneshot(
            Request::post(format!("/u/{USER_KEY}/override"))
                .header(ORIGIN, ORIGIN_URL)
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(Body::from(format!("item_id={ITEM_ID}&slot_id={slot_id}")))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(override_response.status(), StatusCode::SEE_OTHER);

    let first_sync = post_sync(app.clone()).await;
    assert_eq!(first_sync.status(), StatusCode::SEE_OTHER);
    assert_eq!(first_sync.headers()["location"], "/#proposals-jose");
    assert_eq!(transport.metadata_calls.load(Ordering::SeqCst), 1);
    assert_eq!(api_log_count(&pool, &format!("/types/{TYPE_ID}")).await, 1);
    assert_eq!(
        api_log_count(&pool, &format!("/users/{USER_ID}/collected_items")).await,
        1
    );

    let second_sync = post_sync(app.clone()).await;
    assert_eq!(second_sync.status(), StatusCode::SEE_OTHER);
    assert_eq!(second_sync.headers()["location"], "/#proposals-jose");
    assert_eq!(transport.metadata_calls.load(Ordering::SeqCst), 1);
    assert_eq!(api_log_count(&pool, &format!("/types/{TYPE_ID}")).await, 1);
    assert_eq!(
        api_log_count(&pool, &format!("/users/{USER_ID}/collected_items")).await,
        2
    );

    let album = get_album(app).await;
    let assigned = album
        .series
        .iter()
        .flat_map(|series| &series.slots)
        .find(|slot| slot.slot.id.to_string() == slot_id)
        .expect("assigned slot must remain in the album");
    let SlotStatus::Owned { items, .. } = &assigned.status else {
        panic!("manual assignment must survive the replacement snapshot");
    };
    assert_eq!(items[0].item_id, ITEM_ID);
    assert_eq!(items[0].match_source, Some(MatchSource::ManualOverride));

    clean_acceptance_rows(&pool).await;
}

#[tokio::test]
#[ignore = "requires disposable Postgres in TEST_DATABASE_URL; see test doc comment"]
async fn proposal_preferences_are_idempotent_user_scoped_and_survive_sync() {
    let database_url = std::env::var("TEST_DATABASE_URL").expect("TEST_DATABASE_URL must be set");
    assert_disposable_database_url(&database_url);
    let pool = PgPoolOptions::new()
        .max_connections(2)
        .connect(&database_url)
        .await
        .unwrap();
    sqlx::migrate!("./migrations").run(&pool).await.unwrap();
    clean_acceptance_rows(&pool).await;
    let repository = Repository::new(pool.clone());
    let key = CollectionProposalKey::from_canonical_parts("Lunar ounce", 1_000, "proof_coloured")
        .unwrap();

    repository
        .upsert_collection_proposal_preference(USER_KEY, &key, ProposalDisposition::Followed)
        .await
        .unwrap();
    repository
        .upsert_collection_proposal_preference(USER_KEY, &key, ProposalDisposition::Followed)
        .await
        .unwrap();
    repository
        .upsert_collection_proposal_preference("padre", &key, ProposalDisposition::Ignored)
        .await
        .unwrap();
    repository.store_sync(USER_KEY, &[], None).await.unwrap();

    let preferences = repository
        .load_collection_proposal_preferences(USER_KEY)
        .await
        .unwrap();
    assert_eq!(preferences.len(), 1);
    assert_eq!(preferences[0].disposition, ProposalDisposition::Followed);
    assert_eq!(
        repository
            .load_collection_proposal_preferences("padre")
            .await
            .unwrap()[0]
            .disposition,
        ProposalDisposition::Ignored
    );

    let app = build_router(AppState {
        config: AppConfig::parse("jose:1:key-one,padre:2:key-two", Some("1500"), ORIGIN_URL)
            .unwrap(),
        repository: repository.clone(),
        series: Arc::new(Vec::new()),
        collection_catalogs: Arc::new(Vec::new()),
        sync: SyncService::new(repository.clone(), BTreeMap::new()),
        image_client: reqwest::Client::new(),
    });
    let fabricated = app
        .clone()
        .oneshot(
            Request::post(format!("/u/{USER_KEY}/collection-proposal-preference"))
                .header(ORIGIN, ORIGIN_URL)
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(Body::from(
                    "family=Fabricated&weight_millioz=1000&finish=unknown&action=follow",
                ))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(fabricated.status(), StatusCode::BAD_REQUEST);

    let restored = app
        .oneshot(
            Request::post(format!("/u/{USER_KEY}/collection-proposal-preference"))
                .header(ORIGIN, ORIGIN_URL)
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(Body::from(
                    "family=Lunar+ounce&weight_millioz=1000&finish=proof_coloured&action=restore",
                ))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(restored.status(), StatusCode::SEE_OTHER);
    assert_eq!(restored.headers()["location"], "/#proposals-jose");

    repository
        .delete_collection_proposal_preference(USER_KEY, &key)
        .await
        .unwrap();
    assert!(
        repository
            .load_collection_proposal_preferences(USER_KEY)
            .await
            .unwrap()
            .is_empty()
    );
    assert_eq!(
        repository
            .load_collection_proposal_preferences("padre")
            .await
            .unwrap()
            .len(),
        1
    );

    clean_acceptance_rows(&pool).await;
}

#[tokio::test]
#[ignore = "requires disposable Postgres in TEST_DATABASE_URL; see test doc comment"]
async fn followed_collection_route_requires_current_followed_exact_catalog_evidence() {
    let database_url = std::env::var("TEST_DATABASE_URL").expect("TEST_DATABASE_URL must be set");
    assert_disposable_database_url(&database_url);
    let pool = PgPoolOptions::new()
        .max_connections(2)
        .connect(&database_url)
        .await
        .unwrap();
    sqlx::migrate!("./migrations").run(&pool).await.unwrap();
    clean_acceptance_rows(&pool).await;
    let repository = Repository::new(pool.clone());
    let official_type_id = 195_591;
    let unrelated_same_key_type_id = 999_998;
    for (type_id, title) in [
        (official_type_id, "1 Dinar - X-Rays"),
        (unrelated_same_key_type_id, "1 Dollar - Nikola Tesla"),
    ] {
        repository
            .claim_type_fetch(type_id)
            .await
            .unwrap()
            .unwrap()
            .cache(&numista::NumistaType {
                id: Some(type_id),
                title: Some(title.to_owned()),
                weight: Some(31.103_476_8),
                series: Some("Nikola Tesla".to_owned()),
                ..numista::NumistaType::default()
            })
            .await
            .unwrap();
    }
    let collected_item = |id, type_id| CollectedItem {
        id: Some(id),
        quantity: Some(1),
        item_type: Some(ItemType {
            id: Some(type_id),
            title: Some("Nikola Tesla holding".to_owned()),
            ..ItemType::default()
        }),
        ..CollectedItem::default()
    };
    repository
        .store_sync(USER_KEY, &[collected_item(90_001, official_type_id)], None)
        .await
        .unwrap();
    repository
        .store_sync("padre", &[collected_item(90_002, official_type_id)], None)
        .await
        .unwrap();

    let mut catalogs = load_collection_catalogs().unwrap();
    let followed_key = catalogs[0].key();
    repository
        .upsert_collection_proposal_preference(
            USER_KEY,
            &followed_key,
            ProposalDisposition::Followed,
        )
        .await
        .unwrap();
    let mut wrong_variant = catalogs[0].clone();
    wrong_variant.id = CollectionCatalogId::new("nikola-tesla-serbia-2oz");
    wrong_variant.weight_millioz = 2_000;
    catalogs.push(wrong_variant);

    let app = build_router(AppState {
        config: AppConfig::parse("jose:1:key-one,padre:2:key-two", Some("1500"), ORIGIN_URL)
            .unwrap(),
        repository: repository.clone(),
        series: Arc::new(Vec::new()),
        collection_catalogs: Arc::new(catalogs),
        sync: SyncService::new(repository.clone(), BTreeMap::new()),
        image_client: reqwest::Client::new(),
    });

    let jose = get_followed_collection(app.clone(), USER_KEY, "nikola-tesla-serbia-1oz").await;
    assert_eq!(jose.status(), StatusCode::OK);
    let body = jose.into_body().collect().await.unwrap().to_bytes();
    let html = String::from_utf8(body.to_vec()).unwrap();
    assert!(html.contains("1 / 12 emisiones"));
    assert_eq!(html.matches("class=\"slot ").count(), 12);

    assert_eq!(
        get_followed_collection(app.clone(), "padre", "nikola-tesla-serbia-1oz")
            .await
            .status(),
        StatusCode::NOT_FOUND
    );
    assert_eq!(
        get_followed_collection(app.clone(), USER_KEY, "not-a-catalog")
            .await
            .status(),
        StatusCode::NOT_FOUND
    );
    assert_eq!(
        get_followed_collection(app.clone(), USER_KEY, "nikola-tesla-serbia-2oz")
            .await
            .status(),
        StatusCode::NOT_FOUND
    );

    repository
        .upsert_collection_proposal_preference(
            "padre",
            &followed_key,
            ProposalDisposition::Followed,
        )
        .await
        .unwrap();
    repository
        .store_sync(
            "padre",
            &[collected_item(90_003, unrelated_same_key_type_id)],
            None,
        )
        .await
        .unwrap();
    assert_eq!(
        get_followed_collection(app.clone(), "padre", "nikola-tesla-serbia-1oz")
            .await
            .status(),
        StatusCode::NOT_FOUND
    );

    repository.store_sync(USER_KEY, &[], None).await.unwrap();
    assert_eq!(
        get_followed_collection(app, USER_KEY, "nikola-tesla-serbia-1oz")
            .await
            .status(),
        StatusCode::NOT_FOUND
    );

    clean_acceptance_rows(&pool).await;
}

fn assert_disposable_database_url(database_url: &str) {
    let parsed = reqwest::Url::parse(database_url)
        .expect("TEST_DATABASE_URL must be a valid PostgreSQL URL");
    let loopback = matches!(parsed.host_str(), Some("localhost" | "127.0.0.1" | "::1"));
    let database_name = parsed.path().trim_start_matches('/');
    let dedicated_name =
        database_name == "coindex_verify" || database_name.starts_with("coindex_test_");
    let acknowledged = std::env::var(DESTRUCTIVE_TEST_ACK).as_deref() == Ok("1");
    assert!(
        loopback && dedicated_name && acknowledged,
        "refusing destructive acceptance test: use a loopback database named \
         `coindex_verify` or `coindex_test_*` and set {DESTRUCTIVE_TEST_ACK}=1"
    );
}

async fn post_sync(app: axum::Router) -> axum::response::Response {
    app.oneshot(
        Request::post(format!("/u/{USER_KEY}/sync"))
            .header(ORIGIN, ORIGIN_URL)
            .body(Body::empty())
            .unwrap(),
    )
    .await
    .unwrap()
}

async fn get_album(app: axum::Router) -> Album {
    let response = app
        .oneshot(
            Request::get(format!("/api/album/{USER_KEY}"))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    let body = response.into_body().collect().await.unwrap().to_bytes();
    serde_json::from_slice(&body).unwrap()
}

async fn get_followed_collection(
    app: axum::Router,
    user: &str,
    catalog_id: &str,
) -> axum::response::Response {
    app.oneshot(
        Request::get(format!("/u/{user}/followed-collections/{catalog_id}"))
            .body(Body::empty())
            .unwrap(),
    )
    .await
    .unwrap()
}

async fn api_log_count(pool: &PgPool, endpoint: &str) -> i64 {
    sqlx::query_scalar!(
        "SELECT count(*) FROM api_call_log WHERE endpoint = $1",
        endpoint
    )
    .fetch_one(pool)
    .await
    .unwrap()
    .unwrap_or_default()
}

async fn clean_acceptance_rows(pool: &PgPool) {
    sqlx::query!(
        "DELETE FROM collection_proposal_preferences
         WHERE user_key = $1 OR user_key = 'padre'",
        USER_KEY
    )
    .execute(pool)
    .await
    .unwrap();
    sqlx::query!("DELETE FROM manual_overrides WHERE user_key = $1", USER_KEY)
        .execute(pool)
        .await
        .unwrap();
    for user_key in [USER_KEY, "padre"] {
        sqlx::query!("DELETE FROM collected_items WHERE user_key = $1", user_key)
            .execute(pool)
            .await
            .unwrap();
    }
    for type_id in [TYPE_ID, 195_591, 999_998] {
        sqlx::query!(
            "DELETE FROM type_meta WHERE type_id = $1",
            i32::try_from(type_id).unwrap()
        )
        .execute(pool)
        .await
        .unwrap();
    }
    sqlx::query!(
        "DELETE FROM api_call_log
         WHERE endpoint = $1 OR endpoint = $2",
        format!("/types/{TYPE_ID}"),
        format!("/users/{USER_ID}/collected_items")
    )
    .execute(pool)
    .await
    .unwrap();
}

#[test]
fn destructive_database_guard_rejects_remote_and_nondedicated_targets() {
    for unsafe_url in [
        "postgres://postgres:secret@db.example/coindex_test_acceptance",
        "postgres://postgres:secret@127.0.0.1/coindex",
        "not-a-url",
    ] {
        let rejected = std::panic::catch_unwind(|| assert_disposable_database_url(unsafe_url));
        assert!(rejected.is_err(), "guard accepted `{unsafe_url}`");
    }
}
