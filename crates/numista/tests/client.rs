use async_trait::async_trait;
use numista::{
    ApiCall, ApiCallKind, BudgetGate, CallRecorder, ClientConfig, CollectedItemsResponse,
    HttpRequest, HttpResponse, HttpTransport, NumistaClient, NumistaType, OAuthTokenResponse,
    PolicyError, SyncCallEstimate, SyncPlan, TransportError,
};
use std::collections::VecDeque;
use std::sync::Arc;
use tokio::sync::Mutex;

const TOKEN_FIXTURE: &[u8] = include_bytes!("../../../fixtures/numista/oauth_token.json");
const COLLECTION_FIXTURE: &[u8] = include_bytes!("../../../fixtures/numista/collected_items.json");
const TYPE_FIXTURE: &[u8] = include_bytes!("../../../fixtures/numista/type_420_es.json");

#[derive(Default)]
struct FixtureTransport {
    responses: Mutex<VecDeque<Vec<u8>>>,
    requests: Mutex<Vec<HttpRequest>>,
}

impl FixtureTransport {
    fn with_responses(responses: impl IntoIterator<Item = &'static [u8]>) -> Self {
        Self {
            responses: Mutex::new(responses.into_iter().map(<[u8]>::to_vec).collect()),
            requests: Mutex::new(Vec::new()),
        }
    }
}

#[async_trait]
impl HttpTransport for FixtureTransport {
    async fn execute(&self, request: HttpRequest) -> Result<HttpResponse, TransportError> {
        self.requests.lock().await.push(request);
        let body = self
            .responses
            .lock()
            .await
            .pop_front()
            .expect("fixture response for every request");
        Ok(HttpResponse { status: 200, body })
    }
}

#[derive(Default)]
struct RecordingPolicy {
    preflighted: Mutex<Vec<ApiCall>>,
    recorded: Mutex<Vec<ApiCall>>,
}

#[async_trait]
impl BudgetGate for RecordingPolicy {
    async fn preflight(&self, call: &ApiCall) -> Result<(), PolicyError> {
        self.preflighted.lock().await.push(call.clone());
        Ok(())
    }
}

#[async_trait]
impl CallRecorder for RecordingPolicy {
    async fn record(&self, call: &ApiCall) -> Result<(), PolicyError> {
        self.recorded.lock().await.push(call.clone());
        Ok(())
    }
}

struct RejectingGate;

#[async_trait]
impl BudgetGate for RejectingGate {
    async fn preflight(&self, _call: &ApiCall) -> Result<(), PolicyError> {
        Err(PolicyError::new("monthly budget exhausted"))
    }
}

struct FailingRecorder;

#[async_trait]
impl CallRecorder for FailingRecorder {
    async fn record(&self, _call: &ApiCall) -> Result<(), PolicyError> {
        Err(PolicyError::new("database unavailable"))
    }
}

fn config() -> ClientConfig {
    ClientConfig::new("fixture-api-key")
}

#[tokio::test]
async fn collection_uses_required_auth_and_reuses_cached_token() {
    let transport = Arc::new(FixtureTransport::with_responses([
        TOKEN_FIXTURE,
        COLLECTION_FIXTURE,
        COLLECTION_FIXTURE,
    ]));
    let policy = Arc::new(RecordingPolicy::default());
    let client =
        NumistaClient::with_transport(config(), transport.clone(), policy.clone(), policy.clone())
            .unwrap();

    let first = client.fetch_collected_items(2104).await.unwrap();
    let second = client.fetch_collected_items(2104).await.unwrap();

    assert_eq!(first.items.as_ref().map(Vec::len), Some(2));
    assert_eq!(second.item_count, Some(2));
    assert_eq!(
        first
            .raw
            .as_ref()
            .and_then(|raw| raw.get("item_count"))
            .and_then(serde_json::Value::as_u64),
        Some(2)
    );

    let requests = transport.requests.lock().await;
    assert_eq!(requests.len(), 3);
    assert_eq!(requests[0].path(), "/oauth_token");
    assert!(
        requests[0]
            .query()
            .contains(&("grant_type".to_owned(), "client_credentials".to_owned()))
    );
    assert!(
        requests[0]
            .query()
            .contains(&("scope".to_owned(), "view_collection".to_owned()))
    );

    for request in &requests[1..] {
        assert_eq!(request.path(), "/users/2104/collected_items");
        assert!(
            request.query().is_empty(),
            "v3.32 collection is unpaginated"
        );
        assert_eq!(request.header("Numista-API-Key"), Some("fixture-api-key"));
        assert_eq!(
            request.header("Authorization"),
            Some("Bearer fixture-access-token")
        );
    }

    let preflighted = policy.preflighted.lock().await;
    let recorded = policy.recorded.lock().await;
    assert_eq!(*preflighted, *recorded);
    assert_eq!(
        preflighted
            .iter()
            .map(|call| &call.kind)
            .collect::<Vec<_>>(),
        vec![
            &ApiCallKind::OAuthToken,
            &ApiCallKind::CollectedItems,
            &ApiCallKind::CollectedItems
        ]
    );
}

#[tokio::test]
async fn type_metadata_requests_spanish_and_only_api_key_auth() {
    let transport = Arc::new(FixtureTransport::with_responses([TYPE_FIXTURE]));
    let policy = Arc::new(RecordingPolicy::default());
    let client =
        NumistaClient::with_transport(config(), transport.clone(), policy.clone(), policy).unwrap();

    let metadata = client.fetch_type_metadata(420).await.unwrap();

    assert_eq!(metadata.id, Some(420));
    assert_eq!(
        metadata
            .raw
            .as_ref()
            .and_then(|raw| raw.get("orientation"))
            .and_then(serde_json::Value::as_str),
        Some("coin")
    );
    assert_eq!(
        metadata
            .obverse
            .as_ref()
            .and_then(|side| side.picture.as_deref()),
        Some("https://en.numista.com/catalogue/photos/canada/1009-original.jpg")
    );
    let requests = transport.requests.lock().await;
    assert_eq!(requests.len(), 1);
    assert_eq!(requests[0].path(), "/types/420");
    assert_eq!(requests[0].query(), &[("lang".to_owned(), "es".to_owned())]);
    assert_eq!(
        requests[0].header("Numista-API-Key"),
        Some("fixture-api-key")
    );
    assert_eq!(requests[0].header("Authorization"), None);
}

#[tokio::test]
async fn estimate_is_network_free_deduplicated_and_token_aware() {
    let plan = SyncPlan {
        fetch_collection: true,
        missing_type_ids: vec![420, 420, 11331],
    };
    assert_eq!(
        SyncCallEstimate::for_plan(&plan, false),
        SyncCallEstimate {
            oauth_token: 1,
            collected_items: 1,
            type_metadata: 2,
            total: 4,
        }
    );

    let transport = Arc::new(FixtureTransport::with_responses([
        TOKEN_FIXTURE,
        COLLECTION_FIXTURE,
    ]));
    let policy = Arc::new(RecordingPolicy::default());
    let client =
        NumistaClient::with_transport(config(), transport, policy.clone(), policy).unwrap();
    client.fetch_collected_items(2104).await.unwrap();

    assert_eq!(
        client.estimate_sync_calls(&plan).await,
        SyncCallEstimate {
            oauth_token: 0,
            collected_items: 1,
            type_metadata: 2,
            total: 3,
        }
    );
}

#[tokio::test]
async fn budget_and_recording_failures_prevent_transport_calls() {
    let denied_transport = Arc::new(FixtureTransport::with_responses([TYPE_FIXTURE]));
    let client = NumistaClient::with_transport(
        config(),
        denied_transport.clone(),
        Arc::new(RejectingGate),
        Arc::new(RecordingPolicy::default()),
    )
    .unwrap();
    let error = client.fetch_type_metadata(420).await.unwrap_err();
    assert!(error.to_string().contains("monthly budget exhausted"));
    assert!(denied_transport.requests.lock().await.is_empty());

    let unrecorded_transport = Arc::new(FixtureTransport::with_responses([TYPE_FIXTURE]));
    let client = NumistaClient::with_transport(
        config(),
        unrecorded_transport.clone(),
        Arc::new(RecordingPolicy::default()),
        Arc::new(FailingRecorder),
    )
    .unwrap();
    let error = client.fetch_type_metadata(420).await.unwrap_err();
    assert!(error.to_string().contains("database unavailable"));
    assert!(unrecorded_transport.requests.lock().await.is_empty());
}

#[test]
fn dto_fields_are_optional_and_fixtures_preserve_unknown_fields() {
    let empty_collection: CollectedItemsResponse = serde_json::from_str("{}").unwrap();
    let empty_type: NumistaType = serde_json::from_str("{}").unwrap();
    assert_eq!(empty_collection, CollectedItemsResponse::default());
    assert_eq!(empty_type, NumistaType::default());

    let metadata: NumistaType = serde_json::from_slice(TYPE_FIXTURE).unwrap();
    assert_eq!(
        metadata
            .references
            .as_ref()
            .and_then(|references| references.first())
            .and_then(|reference| reference.catalogue.as_ref())
            .and_then(|catalogue| catalogue.code.as_deref()),
        Some("KM")
    );
}

#[test]
fn secrets_are_redacted_from_debug_output() {
    let config_debug = format!("{:?}", ClientConfig::new("super-secret-api-key"));
    assert!(!config_debug.contains("super-secret-api-key"));
    assert!(config_debug.contains("[REDACTED]"));

    let token: OAuthTokenResponse = serde_json::from_slice(TOKEN_FIXTURE).unwrap();
    let token_debug = format!("{token:?}");
    assert!(!token_debug.contains("fixture-access-token"));
    assert!(token_debug.contains("[REDACTED]"));
}

#[test]
fn raw_json_can_be_borrowed_or_taken_without_dto_serialization() {
    let mut response = CollectedItemsResponse {
        raw: Some(serde_json::json!({"type": 44, "missing_is_absent": true})),
        ..CollectedItemsResponse::default()
    };

    assert_eq!(
        response
            .raw_json()
            .and_then(|raw| raw.get("type"))
            .and_then(serde_json::Value::as_u64),
        Some(44)
    );
    let raw = response.take_raw_json().unwrap();
    assert_eq!(raw["type"], 44);
    assert!(response.raw_json().is_none());
}
