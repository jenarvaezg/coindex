use std::collections::{BTreeMap, BTreeSet};
use std::fmt;

use thiserror::Error;

pub const DEFAULT_MONTHLY_BUDGET: u32 = 1_500;

#[derive(Clone, Eq, PartialEq)]
pub struct UserConfig {
    pub key: String,
    pub numista_user_id: u64,
    pub api_key: String,
}

#[derive(Clone)]
pub struct AppConfig {
    users: BTreeMap<String, UserConfig>,
    pub monthly_budget: u32,
    pub origin: String,
}

#[derive(Debug, Error, Eq, PartialEq)]
pub enum ConfigError {
    #[error("COINDEX_USERS must define exactly two users; found {0}")]
    UserCount(usize),
    #[error(
        "invalid COINDEX_USERS entry at position {position}; expected key:numista_user_id:api_key"
    )]
    InvalidUser { position: usize },
    #[error("duplicate user key `{0}`")]
    DuplicateUser(String),
    #[error("user key `{0}` may contain only lowercase ASCII letters, digits, `_`, and `-`")]
    InvalidUserKey(String),
    #[error("Numista user id for user `{key}` is invalid: {value}")]
    InvalidUserId { key: String, value: String },
    #[error("Numista user id `{0}` is assigned to more than one user")]
    DuplicateNumistaUserId(u64),
    #[error("NUMISTA_MONTHLY_BUDGET must be a positive integer, got `{0}`")]
    InvalidBudget(String),
    #[error("COINDEX_ORIGIN must be one canonical http(s) origin without a path: `{0}`")]
    InvalidOrigin(String),
}

impl AppConfig {
    pub fn parse(users: &str, budget: Option<&str>, origin: &str) -> Result<Self, ConfigError> {
        let entries = users
            .split(',')
            .map(str::trim)
            .filter(|entry| !entry.is_empty())
            .collect::<Vec<_>>();
        if entries.len() != 2 {
            return Err(ConfigError::UserCount(entries.len()));
        }

        let mut parsed = BTreeMap::new();
        let mut numista_ids = BTreeSet::new();
        for (index, entry) in entries.into_iter().enumerate() {
            let position = index + 1;
            let parts = entry.splitn(3, ':').collect::<Vec<_>>();
            if parts.len() != 3 || parts.iter().any(|part| part.trim().is_empty()) {
                return Err(ConfigError::InvalidUser { position });
            }
            let key = parts[0].trim().to_owned();
            if !key
                .chars()
                .all(|ch| ch.is_ascii_lowercase() || ch.is_ascii_digit() || ch == '_' || ch == '-')
            {
                return Err(ConfigError::InvalidUserKey(key));
            }
            let numista_user_id =
                parts[1]
                    .trim()
                    .parse::<u64>()
                    .map_err(|_| ConfigError::InvalidUserId {
                        key: key.clone(),
                        value: parts[1].trim().to_owned(),
                    })?;
            if parsed.contains_key(&key) {
                return Err(ConfigError::DuplicateUser(key));
            }
            if !numista_ids.insert(numista_user_id) {
                return Err(ConfigError::DuplicateNumistaUserId(numista_user_id));
            }
            parsed.insert(
                key.clone(),
                UserConfig {
                    key,
                    numista_user_id,
                    api_key: parts[2].trim().to_owned(),
                },
            );
        }

        let monthly_budget = match budget {
            None | Some("") => DEFAULT_MONTHLY_BUDGET,
            Some(value) => value
                .parse::<u32>()
                .ok()
                .filter(|value| *value > 0)
                .ok_or_else(|| ConfigError::InvalidBudget(value.to_owned()))?,
        };
        let origin = canonical_origin(origin)
            .ok_or_else(|| ConfigError::InvalidOrigin(origin.to_owned()))?;

        Ok(Self {
            users: parsed,
            monthly_budget,
            origin,
        })
    }

    pub fn user(&self, key: &str) -> Option<&UserConfig> {
        self.users.get(key)
    }

    pub fn users(&self) -> impl Iterator<Item = &UserConfig> {
        self.users.values()
    }
}

impl fmt::Debug for UserConfig {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("UserConfig")
            .field("key", &self.key)
            .field("numista_user_id", &self.numista_user_id)
            .field("api_key", &"[REDACTED]")
            .finish()
    }
}

impl fmt::Debug for AppConfig {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("AppConfig")
            .field("users", &self.users)
            .field("monthly_budget", &self.monthly_budget)
            .field("origin", &self.origin)
            .finish()
    }
}

pub fn canonical_origin(value: &str) -> Option<String> {
    let url = reqwest::Url::parse(value).ok()?;
    if !matches!(url.scheme(), "http" | "https")
        || !url.username().is_empty()
        || url.password().is_some()
        || url.host_str().is_none()
        || url.path() != "/"
        || url.query().is_some()
        || url.fragment().is_some()
    {
        return None;
    }
    Some(url.origin().ascii_serialization())
}

#[cfg(test)]
mod tests {
    use super::{AppConfig, ConfigError, DEFAULT_MONTHLY_BUDGET};

    #[test]
    fn parses_exactly_two_typed_users() {
        let config = AppConfig::parse(
            "jose:123:key-one,padre:456:key:with-colon",
            None,
            "https://coindex.example",
        )
        .unwrap();

        assert_eq!(config.monthly_budget, DEFAULT_MONTHLY_BUDGET);
        assert_eq!(config.user("jose").unwrap().numista_user_id, 123);
        assert_eq!(config.user("padre").unwrap().api_key, "key:with-colon");
    }

    #[test]
    fn rejects_wrong_count_duplicate_and_bad_budget() {
        assert_eq!(
            AppConfig::parse("jose:123:key", None, "https://coindex.example").unwrap_err(),
            ConfigError::UserCount(1)
        );
        assert_eq!(
            AppConfig::parse("jose:123:key,jose:456:key", None, "https://coindex.example",)
                .unwrap_err(),
            ConfigError::DuplicateUser("jose".to_owned())
        );
        assert_eq!(
            AppConfig::parse(
                "jose:123:key,padre:456:key",
                Some("0"),
                "https://coindex.example",
            )
            .unwrap_err(),
            ConfigError::InvalidBudget("0".to_owned())
        );
    }

    #[test]
    fn canonicalizes_origin_and_redacts_api_keys_from_debug() {
        let config = AppConfig::parse(
            "jose:123:top-secret,padre:456:other-secret",
            None,
            "HTTPS://Coindex.Example:443",
        )
        .unwrap();

        assert_eq!(config.origin, "https://coindex.example");
        let debug = format!("{config:?}");
        assert!(debug.contains("[REDACTED]"));
        assert!(!debug.contains("top-secret"));
        assert!(!debug.contains("other-secret"));
    }

    #[test]
    fn malformed_user_errors_never_include_api_keys() {
        let malformed = AppConfig::parse(
            "jose:not-a-number:top-secret,padre:456:other-secret",
            None,
            "https://coindex.example",
        )
        .unwrap_err()
        .to_string();
        let duplicate_id = AppConfig::parse(
            "jose:456:top-secret,padre:456:other-secret",
            None,
            "https://coindex.example",
        )
        .unwrap_err()
        .to_string();

        assert!(!malformed.contains("top-secret"));
        assert!(!malformed.contains("other-secret"));
        assert!(!duplicate_id.contains("top-secret"));
        assert!(!duplicate_id.contains("other-secret"));
    }
}
