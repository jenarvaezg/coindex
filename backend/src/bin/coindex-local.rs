use std::env;
use std::ffi::OsString;
use std::fmt;
use std::fs::File;
use std::io::Read;
use std::net::SocketAddr;
#[cfg(unix)]
use std::os::unix::fs::PermissionsExt;
use std::path::{Path, PathBuf};

use serde::Deserialize;
use thiserror::Error;

const DEFAULT_LOCAL_BIND: &str = "127.0.0.1:8000";

#[derive(Deserialize)]
#[serde(deny_unknown_fields)]
struct LocalSecrets {
    #[serde(rename = "COINDEX_USERS")]
    users: String,
    #[serde(rename = "COINDEX_ORIGIN")]
    origin: String,
    #[serde(rename = "NUMISTA_MONTHLY_BUDGET", default)]
    monthly_budget: Option<String>,
}

struct RuntimeConfig {
    secrets_file: PathBuf,
    database_url: String,
    bind: SocketAddr,
}

impl RuntimeConfig {
    fn from_env() -> Result<Self, LocalError> {
        Self::from_values(
            env::var_os("COINDEX_SECRETS_FILE"),
            env::var("DATABASE_URL").ok(),
            env::var("COINDEX_BIND").ok(),
        )
    }

    fn from_values(
        secrets_file: Option<OsString>,
        database_url: Option<String>,
        bind: Option<String>,
    ) -> Result<Self, LocalError> {
        let secrets_file = secrets_file
            .map(PathBuf::from)
            .unwrap_or_else(|| PathBuf::from("Secrets.dev.toml"));
        let database_url = database_url
            .filter(|value| !value.trim().is_empty())
            .ok_or(LocalError::MissingDatabaseUrl)?;
        let bind_value = bind.unwrap_or_else(|| DEFAULT_LOCAL_BIND.to_owned());
        let bind: SocketAddr = bind_value
            .parse()
            .map_err(|_| LocalError::InvalidBind(bind_value))?;
        if !bind.ip().is_loopback() {
            return Err(LocalError::NonLoopbackBind(bind));
        }
        Ok(Self {
            secrets_file,
            database_url,
            bind,
        })
    }
}

impl fmt::Debug for RuntimeConfig {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("RuntimeConfig")
            .field("secrets_file", &self.secrets_file)
            .field("database_url", &"[REDACTED]")
            .field("bind", &self.bind)
            .finish()
    }
}

impl fmt::Debug for LocalSecrets {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("LocalSecrets")
            .field("users", &"[REDACTED]")
            .field("origin", &self.origin)
            .field("monthly_budget", &self.monthly_budget)
            .finish()
    }
}

#[derive(Debug, Error)]
enum LocalError {
    #[error("DATABASE_URL is required for the local runner")]
    MissingDatabaseUrl,
    #[error("COINDEX_BIND must be an IP socket address, got `{0}`")]
    InvalidBind(String),
    #[error("COINDEX_BIND must use a loopback address for the unauthenticated local runner")]
    NonLoopbackBind(SocketAddr),
    #[error("cannot read secrets file `{path}`: {source}")]
    SecretsFile {
        path: PathBuf,
        #[source]
        source: std::io::Error,
    },
    #[error("secrets file `{path}` has overly permissive Unix permissions; use mode 0600")]
    InsecurePermissions { path: PathBuf },
    #[error("secrets file is not valid TOML with the required COINDEX_* keys")]
    InvalidSecretsToml,
    #[error("cannot connect to Postgres: {0}")]
    Database(#[from] sqlx::Error),
    #[error("backend startup failed: {0}")]
    Startup(#[from] coindex_backend::StartupError),
    #[error("cannot bind local server to {bind}: {source}")]
    Bind {
        bind: SocketAddr,
        #[source]
        source: std::io::Error,
    },
    #[error("local HTTP server failed: {0}")]
    Server(std::io::Error),
}

fn parse_secrets(contents: &str) -> Result<LocalSecrets, LocalError> {
    toml::from_str(contents).map_err(|_| LocalError::InvalidSecretsToml)
}

fn load_secrets_file(path: &Path) -> Result<LocalSecrets, LocalError> {
    let mut file = File::open(path).map_err(|source| LocalError::SecretsFile {
        path: path.to_owned(),
        source,
    })?;
    #[cfg(unix)]
    {
        let metadata = file.metadata().map_err(|source| LocalError::SecretsFile {
            path: path.to_owned(),
            source,
        })?;
        if metadata.permissions().mode() & 0o077 != 0 {
            return Err(LocalError::InsecurePermissions {
                path: path.to_owned(),
            });
        }
    }
    let mut contents = String::new();
    file.read_to_string(&mut contents)
        .map_err(|source| LocalError::SecretsFile {
            path: path.to_owned(),
            source,
        })?;
    parse_secrets(&contents)
}

#[tokio::main]
async fn main() -> Result<(), LocalError> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();

    let config = RuntimeConfig::from_env()?;
    let secrets = load_secrets_file(&config.secrets_file)?;
    let pool = sqlx::PgPool::connect(&config.database_url).await?;
    let router = coindex_backend::bootstrap(
        pool,
        &secrets.users,
        secrets.monthly_budget.as_deref(),
        &secrets.origin,
    )
    .await?;
    let listener = tokio::net::TcpListener::bind(config.bind)
        .await
        .map_err(|source| LocalError::Bind {
            bind: config.bind,
            source,
        })?;
    tracing::info!(
        bind = %config.bind,
        open_url = %secrets.origin,
        secrets_file = %config.secrets_file.display(),
        "Coindex local server ready"
    );
    axum::serve(listener, router)
        .await
        .map_err(LocalError::Server)
}

#[cfg(test)]
mod tests {
    #[cfg(unix)]
    use std::fs;
    #[cfg(unix)]
    use std::os::unix::fs::PermissionsExt;

    #[cfg(unix)]
    use super::load_secrets_file;
    use super::{DEFAULT_LOCAL_BIND, RuntimeConfig, parse_secrets};

    #[test]
    fn parses_uppercase_shuttle_secret_keys_without_exposing_values() {
        let secrets = parse_secrets(
            r#"
                COINDEX_USERS = "jose:1:first-secret,padre:2:second-secret"
                COINDEX_ORIGIN = "http://127.0.0.1:8000"
                NUMISTA_MONTHLY_BUDGET = "1500"
            "#,
        )
        .unwrap();

        assert_eq!(secrets.users, "jose:1:first-secret,padre:2:second-secret");
        assert_eq!(secrets.origin, "http://127.0.0.1:8000");
        assert_eq!(secrets.monthly_budget.as_deref(), Some("1500"));
        let debug = format!("{secrets:?}");
        assert!(!debug.contains("first-secret"));
        assert!(!debug.contains("second-secret"));
    }

    #[cfg(unix)]
    #[test]
    fn rejects_secret_files_readable_by_group_or_other_users() {
        let path = std::env::temp_dir().join(format!(
            "coindex-local-secrets-permissions-{}.toml",
            std::process::id()
        ));
        fs::write(
            &path,
            "COINDEX_USERS='hidden'\nCOINDEX_ORIGIN='http://127.0.0.1:8000'\n",
        )
        .unwrap();
        fs::set_permissions(&path, fs::Permissions::from_mode(0o644)).unwrap();

        let error = load_secrets_file(&path).unwrap_err().to_string();

        fs::remove_file(path).unwrap();
        assert!(error.contains("permissions"));
        assert!(!error.contains("hidden"));
    }

    #[test]
    fn runtime_config_has_safe_local_defaults_and_requires_database_url() {
        let config = RuntimeConfig::from_values(
            None,
            Some("postgres://user:db-password@localhost/coindex".into()),
            None,
        )
        .unwrap();

        assert_eq!(
            config.secrets_file,
            std::path::Path::new("Secrets.dev.toml")
        );
        assert_eq!(config.bind.to_string(), "127.0.0.1:8000");
        assert_eq!(
            format!("http://{DEFAULT_LOCAL_BIND}"),
            "http://127.0.0.1:8000"
        );
        assert!(RuntimeConfig::from_values(None, None, None).is_err());
        assert!(
            RuntimeConfig::from_values(
                None,
                Some("postgres://localhost/coindex".into()),
                Some("0.0.0.0:8000".into()),
            )
            .is_err()
        );
        assert!(!format!("{config:?}").contains("db-password"));
    }
}
