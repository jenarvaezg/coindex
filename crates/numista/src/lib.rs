//! Budget-aware client for the Numista API v3.32.
//!
//! The client deliberately keeps persistence concerns outside this crate. Backends inject
//! a [`BudgetGate`] and [`CallRecorder`], allowing every outbound request to be approved
//! and durably logged before any bytes are sent.

mod client;
mod dto;
mod policy;
mod transport;

pub use client::{
    ClientConfig, ClientError, DEFAULT_BASE_URL, DEFAULT_TOKEN_EXPIRY_MARGIN, NumistaClient,
    SyncCallEstimate, SyncPlan,
};
pub use dto::{
    Catalogue, CatalogueReference, CoinSide, CollectedItem, CollectedItemsResponse, Collection,
    Composition, Issue, Issuer, ItemType, NumistaType, OAuthTokenResponse, Price,
};
pub use policy::{
    ApiCall, ApiCallKind, BudgetGate, CallRecorder, NoopBudgetGate, NoopCallRecorder, PolicyError,
};
pub use transport::{
    HttpMethod, HttpRequest, HttpResponse, HttpTransport, ReqwestTransport, TransportError,
};
