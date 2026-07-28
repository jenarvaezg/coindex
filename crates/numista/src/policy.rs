use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use std::fmt;

#[derive(Clone, Debug, Eq, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ApiCallKind {
    OAuthToken,
    CollectedItems,
    TypeMetadata,
}

#[derive(Clone, Debug, Eq, PartialEq, Serialize, Deserialize)]
pub struct ApiCall {
    /// Stable endpoint label suitable for `api_call_log.endpoint`.
    ///
    /// Authentication values and query strings are intentionally excluded.
    pub endpoint: String,
    pub kind: ApiCallKind,
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct PolicyError {
    message: String,
}

impl PolicyError {
    pub fn new(message: impl Into<String>) -> Self {
        Self {
            message: message.into(),
        }
    }
}

impl fmt::Display for PolicyError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter.write_str(&self.message)
    }
}

impl std::error::Error for PolicyError {}

/// Checks the monthly budget immediately before a request is recorded and sent.
#[async_trait]
pub trait BudgetGate: Send + Sync {
    async fn preflight(&self, call: &ApiCall) -> Result<(), PolicyError>;
}

/// Records an approved call immediately before the transport sends it.
///
/// A recording error aborts the request. This guarantees that a backend using a durable
/// recorder never makes an unaccounted Numista request.
#[async_trait]
pub trait CallRecorder: Send + Sync {
    async fn record(&self, call: &ApiCall) -> Result<(), PolicyError>;
}

#[derive(Clone, Copy, Debug, Default)]
pub struct NoopBudgetGate;

#[async_trait]
impl BudgetGate for NoopBudgetGate {
    async fn preflight(&self, _call: &ApiCall) -> Result<(), PolicyError> {
        Ok(())
    }
}

#[derive(Clone, Copy, Debug, Default)]
pub struct NoopCallRecorder;

#[async_trait]
impl CallRecorder for NoopCallRecorder {
    async fn record(&self, _call: &ApiCall) -> Result<(), PolicyError> {
        Ok(())
    }
}
