use crate::dto::{CollectedItemsResponse, NumistaType, OAuthTokenResponse};
use crate::policy::{ApiCall, ApiCallKind, BudgetGate, CallRecorder};
use crate::transport::{HttpRequest, HttpTransport, ReqwestTransport, TransportError};
use serde::de::DeserializeOwned;
use serde_json::Value;
use std::collections::BTreeSet;
use std::fmt;
use std::sync::Arc;
use std::time::{Duration, Instant};
use thiserror::Error;
use tokio::sync::Mutex;

pub const DEFAULT_BASE_URL: &str = "https://api.numista.com/v3";
pub const DEFAULT_TOKEN_EXPIRY_MARGIN: Duration = Duration::from_secs(60);
const API_KEY_HEADER: &str = "Numista-API-Key";

#[derive(Clone)]
pub struct ClientConfig {
    pub api_key: String,
    pub base_url: String,
    pub request_timeout: Duration,
    pub token_expiry_margin: Duration,
}

impl fmt::Debug for ClientConfig {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("ClientConfig")
            .field("api_key", &"[REDACTED]")
            .field("base_url", &self.base_url)
            .field("request_timeout", &self.request_timeout)
            .field("token_expiry_margin", &self.token_expiry_margin)
            .finish()
    }
}

impl ClientConfig {
    pub fn new(api_key: impl Into<String>) -> Self {
        Self {
            api_key: api_key.into(),
            base_url: DEFAULT_BASE_URL.to_owned(),
            request_timeout: Duration::from_secs(30),
            token_expiry_margin: DEFAULT_TOKEN_EXPIRY_MARGIN,
        }
    }
}

#[derive(Clone, Debug, Default, Eq, PartialEq)]
pub struct SyncPlan {
    pub fetch_collection: bool,
    pub missing_type_ids: Vec<u32>,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct SyncCallEstimate {
    pub oauth_token: u32,
    pub collected_items: u32,
    pub type_metadata: u32,
    pub total: u32,
}

impl SyncCallEstimate {
    pub fn for_plan(plan: &SyncPlan, token_cached: bool) -> Self {
        let type_metadata = plan
            .missing_type_ids
            .iter()
            .copied()
            .collect::<BTreeSet<_>>()
            .len() as u32;
        let collected_items = u32::from(plan.fetch_collection);
        let oauth_token = u32::from(plan.fetch_collection && !token_cached);

        Self {
            oauth_token,
            collected_items,
            type_metadata,
            total: oauth_token + collected_items + type_metadata,
        }
    }
}

#[derive(Debug, Error)]
pub enum ClientError {
    #[error("Numista API key must not be empty")]
    EmptyApiKey,
    #[error("budget preflight rejected {endpoint}: {message}")]
    BudgetRejected { endpoint: String, message: String },
    #[error("could not record {endpoint} before sending it: {message}")]
    RecordingFailed { endpoint: String, message: String },
    #[error(transparent)]
    Transport(#[from] TransportError),
    #[error("Numista returned HTTP {status} for {endpoint}: {body}")]
    Api {
        endpoint: String,
        status: u16,
        body: String,
    },
    #[error("invalid JSON from {endpoint}: {source}")]
    InvalidJson {
        endpoint: String,
        #[source]
        source: serde_json::Error,
    },
    #[error("OAuth response omitted required field `{0}`")]
    InvalidToken(&'static str),
}

#[derive(Clone)]
struct CachedToken {
    value: String,
    refresh_at: Instant,
}

pub struct NumistaClient {
    api_key: String,
    token_expiry_margin: Duration,
    transport: Arc<dyn HttpTransport>,
    budget_gate: Arc<dyn BudgetGate>,
    call_recorder: Arc<dyn CallRecorder>,
    token: Mutex<Option<CachedToken>>,
}

impl NumistaClient {
    pub fn with_reqwest(
        config: ClientConfig,
        budget_gate: Arc<dyn BudgetGate>,
        call_recorder: Arc<dyn CallRecorder>,
    ) -> Result<Self, ClientError> {
        let transport = Arc::new(ReqwestTransport::new(
            config.base_url.clone(),
            config.request_timeout,
        )?);
        Self::with_transport(config, transport, budget_gate, call_recorder)
    }

    pub fn with_transport(
        config: ClientConfig,
        transport: Arc<dyn HttpTransport>,
        budget_gate: Arc<dyn BudgetGate>,
        call_recorder: Arc<dyn CallRecorder>,
    ) -> Result<Self, ClientError> {
        if config.api_key.trim().is_empty() {
            return Err(ClientError::EmptyApiKey);
        }

        Ok(Self {
            api_key: config.api_key,
            token_expiry_margin: config.token_expiry_margin,
            transport,
            budget_gate,
            call_recorder,
            token: Mutex::new(None),
        })
    }

    /// Estimate calls for a sync without performing I/O or reserving budget.
    ///
    /// API v3.32 documents `collected_items` as an unpaginated endpoint, so a collection
    /// refresh costs exactly one collection request. Duplicate type IDs count once.
    pub async fn estimate_sync_calls(&self, plan: &SyncPlan) -> SyncCallEstimate {
        SyncCallEstimate::for_plan(plan, self.has_usable_token().await)
    }

    pub async fn fetch_collected_items(
        &self,
        user_id: u64,
    ) -> Result<CollectedItemsResponse, ClientError> {
        let token = self.access_token().await?;
        let path = format!("/users/{user_id}/collected_items");
        let request = HttpRequest::get(&path)
            .with_header(API_KEY_HEADER, &self.api_key)
            .with_header("Authorization", format!("Bearer {token}"));
        let (mut response, raw): (CollectedItemsResponse, Value) = self
            .send_json(
                ApiCall {
                    endpoint: path,
                    kind: ApiCallKind::CollectedItems,
                },
                request,
            )
            .await?;
        response.raw = Some(raw);
        Ok(response)
    }

    pub async fn fetch_type_metadata(&self, type_id: u32) -> Result<NumistaType, ClientError> {
        let path = format!("/types/{type_id}");
        let request = HttpRequest::get(&path)
            .with_query("lang", "es")
            .with_header(API_KEY_HEADER, &self.api_key);
        let (mut response, raw): (NumistaType, Value) = self
            .send_json(
                ApiCall {
                    endpoint: path,
                    kind: ApiCallKind::TypeMetadata,
                },
                request,
            )
            .await?;
        response.raw = Some(raw);
        Ok(response)
    }

    async fn access_token(&self) -> Result<String, ClientError> {
        // Holding this lock while refreshing intentionally coalesces simultaneous refreshes.
        let mut cached = self.token.lock().await;
        if let Some(token) = cached.as_ref() {
            if token.refresh_at > Instant::now() {
                return Ok(token.value.clone());
            }
        }

        let request = HttpRequest::get("/oauth_token")
            .with_query("grant_type", "client_credentials")
            .with_query("scope", "view_collection")
            .with_header(API_KEY_HEADER, &self.api_key);
        let (response, _raw): (OAuthTokenResponse, Value) = self
            .send_json(
                ApiCall {
                    endpoint: "/oauth_token".to_owned(),
                    kind: ApiCallKind::OAuthToken,
                },
                request,
            )
            .await?;
        let value = response
            .access_token
            .filter(|value| !value.is_empty())
            .ok_or(ClientError::InvalidToken("access_token"))?;
        let expires_in = response
            .expires_in
            .filter(|seconds| *seconds > 0)
            .ok_or(ClientError::InvalidToken("expires_in"))?;
        let lifetime = Duration::from_secs(expires_in);
        let effective_margin = self.token_expiry_margin.min(lifetime / 2);
        let refresh_at = Instant::now() + (lifetime - effective_margin);

        *cached = Some(CachedToken {
            value: value.clone(),
            refresh_at,
        });
        Ok(value)
    }

    async fn has_usable_token(&self) -> bool {
        self.token
            .lock()
            .await
            .as_ref()
            .is_some_and(|token| token.refresh_at > Instant::now())
    }

    async fn send_json<T: DeserializeOwned>(
        &self,
        call: ApiCall,
        request: HttpRequest,
    ) -> Result<(T, Value), ClientError> {
        self.budget_gate
            .preflight(&call)
            .await
            .map_err(|error| ClientError::BudgetRejected {
                endpoint: call.endpoint.clone(),
                message: error.to_string(),
            })?;
        self.call_recorder
            .record(&call)
            .await
            .map_err(|error| ClientError::RecordingFailed {
                endpoint: call.endpoint.clone(),
                message: error.to_string(),
            })?;

        let response = self.transport.execute(request).await?;
        if !(200..300).contains(&response.status) {
            return Err(ClientError::Api {
                endpoint: call.endpoint,
                status: response.status,
                body: String::from_utf8_lossy(&response.body).into_owned(),
            });
        }

        let raw: Value =
            serde_json::from_slice(&response.body).map_err(|source| ClientError::InvalidJson {
                endpoint: call.endpoint.clone(),
                source,
            })?;
        let parsed =
            serde_json::from_value(raw.clone()).map_err(|source| ClientError::InvalidJson {
                endpoint: call.endpoint,
                source,
            })?;
        Ok((parsed, raw))
    }
}
