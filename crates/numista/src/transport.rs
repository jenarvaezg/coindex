use async_trait::async_trait;
use reqwest::header::{HeaderName, HeaderValue};
use std::fmt;
use std::str::FromStr;
use std::time::Duration;
use thiserror::Error;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum HttpMethod {
    Get,
}

#[derive(Clone)]
pub struct HttpRequest {
    method: HttpMethod,
    path: String,
    query: Vec<(String, String)>,
    headers: Vec<(String, String)>,
}

impl HttpRequest {
    pub fn get(path: impl Into<String>) -> Self {
        Self {
            method: HttpMethod::Get,
            path: path.into(),
            query: Vec::new(),
            headers: Vec::new(),
        }
    }

    pub fn with_query(mut self, name: impl Into<String>, value: impl Into<String>) -> Self {
        self.query.push((name.into(), value.into()));
        self
    }

    pub fn with_header(mut self, name: impl Into<String>, value: impl Into<String>) -> Self {
        self.headers.push((name.into(), value.into()));
        self
    }

    pub fn method(&self) -> HttpMethod {
        self.method
    }

    pub fn path(&self) -> &str {
        &self.path
    }

    pub fn query(&self) -> &[(String, String)] {
        &self.query
    }

    pub fn header(&self, name: &str) -> Option<&str> {
        self.headers
            .iter()
            .find(|(candidate, _)| candidate.eq_ignore_ascii_case(name))
            .map(|(_, value)| value.as_str())
    }
}

impl fmt::Debug for HttpRequest {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        let header_names: Vec<&str> = self.headers.iter().map(|(name, _)| name.as_str()).collect();
        formatter
            .debug_struct("HttpRequest")
            .field("method", &self.method)
            .field("path", &self.path)
            .field("query", &self.query)
            .field("header_names", &header_names)
            .finish()
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct HttpResponse {
    pub status: u16,
    pub body: Vec<u8>,
}

#[derive(Debug, Error)]
pub enum TransportError {
    #[error("invalid base URL or request path: {0}")]
    InvalidUrl(String),
    #[error("invalid HTTP header: {0}")]
    InvalidHeader(String),
    #[error("HTTP transport failed: {0}")]
    Request(#[from] reqwest::Error),
}

#[async_trait]
pub trait HttpTransport: Send + Sync {
    async fn execute(&self, request: HttpRequest) -> Result<HttpResponse, TransportError>;
}

#[derive(Clone, Debug)]
pub struct ReqwestTransport {
    base_url: String,
    client: reqwest::Client,
}

impl ReqwestTransport {
    pub fn new(base_url: impl Into<String>, timeout: Duration) -> Result<Self, TransportError> {
        let base_url = base_url.into().trim_end_matches('/').to_owned();
        if reqwest::Url::parse(&base_url).is_err() {
            return Err(TransportError::InvalidUrl(base_url));
        }

        let client = reqwest::Client::builder().timeout(timeout).build()?;
        Ok(Self { base_url, client })
    }
}

#[async_trait]
impl HttpTransport for ReqwestTransport {
    async fn execute(&self, request: HttpRequest) -> Result<HttpResponse, TransportError> {
        let url = format!("{}{}", self.base_url, request.path);
        let mut builder = match request.method {
            HttpMethod::Get => self.client.get(url),
        };

        builder = builder.query(&request.query);
        for (name, value) in request.headers {
            let name = HeaderName::from_str(&name)
                .map_err(|error| TransportError::InvalidHeader(error.to_string()))?;
            let value = HeaderValue::from_str(&value)
                .map_err(|error| TransportError::InvalidHeader(error.to_string()))?;
            builder = builder.header(name, value);
        }

        let response = builder.send().await?;
        let status = response.status().as_u16();
        let body = response.bytes().await?.to_vec();
        Ok(HttpResponse { status, body })
    }
}
