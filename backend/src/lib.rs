mod config;
mod repository;
mod seeds;
mod sync;
mod views;

#[cfg(test)]
mod acceptance_tests;

use std::collections::BTreeMap;
use std::sync::Arc;
use std::time::Duration;

use axum::body::Body;
use axum::extract::{Form, FromRequest, Path, Request, State};
use axum::http::header::{CACHE_CONTROL, CONTENT_TYPE, ORIGIN};
use axum::http::{HeaderMap, HeaderValue, StatusCode};
use axum::response::{Html, IntoResponse, Response};
use axum::routing::{get, post};
use axum::{Json, Router};
use domain::{
    Album, CollectedItem as DomainItem, CollectionCatalog, CollectionProposal,
    CollectionProposalKey, ProposalDisposition, Series, TypeMetaIndex, build_album,
    build_collection_catalog_album, build_collection_proposals, classify_collection_proposals,
};
use numista::{ClientConfig, NumistaClient};
use reqwest::redirect::Policy;
use serde::{Deserialize, Serialize};
use sqlx::PgPool;
use thiserror::Error;

pub use config::{AppConfig, ConfigError};
use repository::{PostgresCallPolicy, Repository, RepositoryError};
use seeds::{SeedError, load_collection_catalogs, load_series};
use sync::{NumistaApi, SyncError, SyncService};

const CSS: &str = include_str!("../static/site.css");
const MAX_IMAGE_BYTES: u64 = 12 * 1024 * 1024;

#[derive(Clone)]
pub struct AppState {
    config: AppConfig,
    repository: Repository,
    series: Arc<Vec<Series>>,
    collection_catalogs: Arc<Vec<CollectionCatalog>>,
    sync: SyncService,
    image_client: reqwest::Client,
}

struct LoadedAlbum {
    album: Album,
    type_meta: TypeMetaIndex,
    proposals: Vec<CollectionProposal>,
    items: Vec<DomainItem>,
}

#[derive(Debug, Error)]
pub enum StartupError {
    #[error(transparent)]
    Config(#[from] ConfigError),
    #[error(transparent)]
    Seed(#[from] SeedError),
    #[error("database migration failed: {0}")]
    Migration(#[from] sqlx::migrate::MigrateError),
    #[error("could not initialize Numista client for `{user}`: {source}")]
    NumistaClient {
        user: String,
        source: numista::ClientError,
    },
    #[error("could not initialize image proxy client: {0}")]
    ImageClient(reqwest::Error),
}

#[derive(Debug, Error)]
enum AppError {
    #[error("unknown user `{0}`")]
    UnknownUser(String),
    #[error("unknown series `{0}`")]
    UnknownSeries(String),
    #[error("unknown collection catalog `{0}`")]
    UnknownCollectionCatalog(String),
    #[error("followed collection catalog is not available for this collector")]
    UnavailableFollowedCollection,
    #[error("unknown slot `{0}`")]
    UnknownSlot(String),
    #[error("unknown collected item `{0}`")]
    UnknownItem(u64),
    #[error("invalid or unavailable collection proposal preference")]
    InvalidCollectionProposalPreference,
    #[error("mutating requests require the configured same-origin Origin")]
    CrossOriginPost,
    #[error("invalid image side `{0}`")]
    InvalidImageSide(String),
    #[error("no cached image for type {type_id} ({side})")]
    MissingImage { type_id: u32, side: String },
    #[error("cached image URL is not trusted")]
    UnsafeImageUrl,
    #[error("upstream image response was not an image")]
    InvalidImageResponse,
    #[error("upstream image is too large")]
    ImageTooLarge,
    #[error("upstream image request failed: {0}")]
    ImageRequest(#[from] reqwest::Error),
    #[error(transparent)]
    Repository(#[from] RepositoryError),
    #[error(transparent)]
    Sync(#[from] SyncError),
}

#[derive(Debug, Deserialize)]
struct OverrideForm {
    item_id: u64,
    slot_id: String,
}

#[derive(Debug, Deserialize)]
#[serde(deny_unknown_fields)]
struct CollectionProposalPreferenceForm {
    family: String,
    weight_millioz: u32,
    finish: String,
    action: String,
}

#[derive(Debug, Serialize)]
struct Health {
    status: &'static str,
    api_calls_this_month: u32,
    monthly_budget: u32,
}

pub async fn bootstrap(
    pool: PgPool,
    users_secret: &str,
    budget_secret: Option<&str>,
    origin_secret: &str,
) -> Result<Router, StartupError> {
    sqlx::migrate!("./migrations").run(&pool).await?;
    let config = AppConfig::parse(users_secret, budget_secret, origin_secret)?;
    let series = Arc::new(load_series()?);
    let collection_catalogs = Arc::new(load_collection_catalogs()?);
    let repository = Repository::new(pool.clone());
    let policy = Arc::new(PostgresCallPolicy::new(pool, config.monthly_budget));
    let mut clients: BTreeMap<String, Arc<dyn NumistaApi>> = BTreeMap::new();
    for user in config.users() {
        let client = NumistaClient::with_reqwest(
            ClientConfig::new(&user.api_key),
            policy.clone(),
            policy.clone(),
        )
        .map_err(|source| StartupError::NumistaClient {
            user: user.key.clone(),
            source,
        })?;
        clients.insert(user.key.clone(), Arc::new(client));
    }
    let image_client = reqwest::Client::builder()
        .timeout(Duration::from_secs(20))
        .redirect(Policy::none())
        .build()
        .map_err(StartupError::ImageClient)?;
    let sync = SyncService::new(repository.clone(), clients);
    Ok(build_router(AppState {
        config,
        repository,
        series,
        collection_catalogs,
        sync,
        image_client,
    }))
}

pub fn build_router(state: AppState) -> Router {
    Router::new()
        .route("/", get(index_handler))
        .route("/u/{user}/series/{series_id}", get(series_handler))
        .route(
            "/u/{user}/followed-collections/{catalog_id}",
            get(followed_collection_handler),
        )
        .route("/u/{user}/unmatched", get(unmatched_handler))
        .route("/u/{user}/override", post(override_handler))
        .route(
            "/u/{user}/collection-proposal-preference",
            post(collection_proposal_preference_handler),
        )
        .route("/u/{user}/sync", post(sync_handler))
        .route("/img/type/{type_id}/{side}", get(image_handler))
        .route("/api/album/{user}", get(album_handler))
        .route("/health", get(health_handler))
        .route("/static/site.css", get(css_handler))
        .with_state(state)
}

async fn index_handler(State(state): State<AppState>) -> Result<Html<String>, AppError> {
    let mut albums = Vec::new();
    for user in state.config.users() {
        let loaded = album_for(&state, &user.key).await?;
        let preferences = state
            .repository
            .load_collection_proposal_preferences(&user.key)
            .await?;
        let proposals = classify_collection_proposals(loaded.proposals, &preferences);
        let eligible_catalog_ids = state
            .collection_catalogs
            .iter()
            .filter(|catalog| catalog.is_evidenced_by(&loaded.items))
            .map(|catalog| catalog.id.clone())
            .collect();
        albums.push((
            user.key.clone(),
            loaded.album,
            proposals,
            eligible_catalog_ids,
        ));
    }
    Ok(Html(
        views::index(&state.config, &albums, &state.collection_catalogs).into_string(),
    ))
}

async fn series_handler(
    State(state): State<AppState>,
    Path((user, series_id)): Path<(String, String)>,
) -> Result<Html<String>, AppError> {
    require_user(&state, &user)?;
    let definition = state
        .series
        .iter()
        .find(|series| series.id.to_string() == series_id)
        .ok_or_else(|| AppError::UnknownSeries(series_id.clone()))?;
    let loaded = album_for(&state, &user).await?;
    let series_album = loaded
        .album
        .series
        .iter()
        .find(|series| series.series_id.to_string() == series_id)
        .ok_or_else(|| AppError::UnknownSeries(series_id))?;
    Ok(Html(
        views::series(&user, definition, series_album, &state.series).into_string(),
    ))
}

async fn followed_collection_handler(
    State(state): State<AppState>,
    Path((user, catalog_id)): Path<(String, String)>,
) -> Result<Html<String>, AppError> {
    require_user(&state, &user)?;
    let catalog = state
        .collection_catalogs
        .iter()
        .find(|catalog| catalog.id.as_str() == catalog_id)
        .ok_or_else(|| AppError::UnknownCollectionCatalog(catalog_id.clone()))?;
    let loaded = album_for(&state, &user).await?;
    let key = catalog.key();
    let current_exact_proposal = loaded
        .proposals
        .iter()
        .any(|proposal| proposal.key() == key);
    if !current_exact_proposal || !catalog.is_evidenced_by(&loaded.items) {
        return Err(AppError::UnavailableFollowedCollection);
    }
    let preferences = state
        .repository
        .load_collection_proposal_preferences(&user)
        .await?;
    let followed = preferences.iter().any(|preference| {
        preference.key == key && preference.disposition == ProposalDisposition::Followed
    });
    if !followed {
        return Err(AppError::UnavailableFollowedCollection);
    }
    let catalog_album = build_collection_catalog_album(catalog, &loaded.items);
    Ok(Html(
        views::followed_collection(&user, catalog, &catalog_album, &loaded.type_meta).into_string(),
    ))
}

async fn unmatched_handler(
    State(state): State<AppState>,
    Path(user): Path<String>,
) -> Result<Html<String>, AppError> {
    require_user(&state, &user)?;
    let loaded = album_for(&state, &user).await?;
    Ok(Html(
        views::unmatched(&user, &loaded.album, &state.series, &loaded.type_meta).into_string(),
    ))
}

async fn override_handler(
    State(state): State<AppState>,
    Path(user): Path<String>,
    headers: HeaderMap,
    Form(form): Form<OverrideForm>,
) -> Result<Response, AppError> {
    validate_same_origin(&headers, &state.config.origin)?;
    require_user(&state, &user)?;
    let items = state.repository.load_items(&user).await?;
    if !items.iter().any(|item| item.id == form.item_id) {
        return Err(AppError::UnknownItem(form.item_id));
    }
    let slot_id = form.slot_id.trim();
    if !slot_id.is_empty()
        && !state
            .series
            .iter()
            .flat_map(|series| &series.slots)
            .any(|slot| slot.id.to_string() == slot_id)
    {
        return Err(AppError::UnknownSlot(slot_id.to_owned()));
    }
    state
        .repository
        .upsert_override(
            &user,
            form.item_id,
            (!slot_id.is_empty()).then_some(slot_id),
        )
        .await?;
    Ok((
        StatusCode::SEE_OTHER,
        [("location", format!("/u/{user}/unmatched"))],
    )
        .into_response())
}

async fn collection_proposal_preference_handler(
    State(state): State<AppState>,
    Path(user): Path<String>,
    request: Request,
) -> Result<Response, AppError> {
    validate_same_origin(request.headers(), &state.config.origin)?;
    require_user(&state, &user)?;
    let Form(form) = Form::<CollectionProposalPreferenceForm>::from_request(request, &state)
        .await
        .map_err(|_| AppError::InvalidCollectionProposalPreference)?;
    let key = CollectionProposalKey::from_canonical_parts(
        &form.family,
        form.weight_millioz,
        &form.finish,
    )
    .ok_or(AppError::InvalidCollectionProposalPreference)?;

    match form.action.as_str() {
        "restore" => {
            state
                .repository
                .delete_collection_proposal_preference(&user, &key)
                .await?;
        }
        "follow" | "ignore" => {
            let loaded = album_for(&state, &user).await?;
            if !loaded
                .proposals
                .iter()
                .any(|proposal| proposal.key() == key)
            {
                return Err(AppError::InvalidCollectionProposalPreference);
            }
            let disposition = if form.action == "follow" {
                ProposalDisposition::Followed
            } else {
                ProposalDisposition::Ignored
            };
            state
                .repository
                .upsert_collection_proposal_preference(&user, &key, disposition)
                .await?;
        }
        _ => return Err(AppError::InvalidCollectionProposalPreference),
    }

    Ok((
        StatusCode::SEE_OTHER,
        [("location", format!("/#proposals-{user}"))],
    )
        .into_response())
}

async fn sync_handler(
    State(state): State<AppState>,
    Path(user): Path<String>,
    headers: HeaderMap,
) -> Result<Response, AppError> {
    validate_same_origin(&headers, &state.config.origin)?;
    let user_config = require_user(&state, &user)?;
    let report = state.sync.run(user_config, false).await?;
    tracing::info!(
        user = %user,
        collection_items = report.collection_items,
        "Numista sync completed"
    );
    Ok((
        StatusCode::SEE_OTHER,
        [("location", format!("/#proposals-{user}"))],
    )
        .into_response())
}

async fn album_handler(
    State(state): State<AppState>,
    Path(user): Path<String>,
) -> Result<Json<Album>, AppError> {
    require_user(&state, &user)?;
    Ok(Json(album_for(&state, &user).await?.album))
}

async fn health_handler(State(state): State<AppState>) -> Result<Json<Health>, AppError> {
    let used = state.repository.monthly_usage().await?;
    Ok(Json(Health {
        status: "ok",
        api_calls_this_month: used,
        monthly_budget: state.config.monthly_budget,
    }))
}

async fn css_handler() -> impl IntoResponse {
    ([(CONTENT_TYPE, "text/css; charset=utf-8")], CSS)
}

async fn image_handler(
    State(state): State<AppState>,
    Path((type_id, side)): Path<(u32, String)>,
) -> Result<Response, AppError> {
    let metadata = state
        .repository
        .cached_type(type_id)
        .await?
        .ok_or_else(|| AppError::MissingImage {
            type_id,
            side: side.clone(),
        })?;
    let coin_side = match side.as_str() {
        "obverse" => metadata.obverse.as_ref(),
        "reverse" => metadata.reverse.as_ref(),
        "edge" => metadata.edge.as_ref(),
        _ => return Err(AppError::InvalidImageSide(side)),
    };
    let source = coin_side
        .and_then(|side| side.picture.as_deref().or(side.thumbnail.as_deref()))
        .ok_or_else(|| AppError::MissingImage {
            type_id,
            side: side.clone(),
        })?;
    let url = trusted_image_url(source)?;

    let response = state.image_client.get(url).send().await?;
    if !response.status().is_success() {
        return Ok(StatusCode::BAD_GATEWAY.into_response());
    }
    if response
        .content_length()
        .is_some_and(|size| size > MAX_IMAGE_BYTES)
    {
        return Err(AppError::ImageTooLarge);
    }
    let content_type = response
        .headers()
        .get(CONTENT_TYPE)
        .and_then(|value| value.to_str().ok())
        .filter(|value| value.starts_with("image/"))
        .ok_or(AppError::InvalidImageResponse)?
        .to_owned();
    let bytes = response.bytes().await?;
    if bytes.len() as u64 > MAX_IMAGE_BYTES {
        return Err(AppError::ImageTooLarge);
    }
    let mut proxied = Response::new(Body::from(bytes));
    *proxied.status_mut() = StatusCode::OK;
    proxied.headers_mut().insert(
        CONTENT_TYPE,
        HeaderValue::from_str(&content_type).map_err(|_| AppError::InvalidImageResponse)?,
    );
    proxied.headers_mut().insert(
        CACHE_CONTROL,
        HeaderValue::from_static("public, max-age=86400, stale-while-revalidate=604800"),
    );
    Ok(proxied)
}

async fn album_for(state: &AppState, user: &str) -> Result<LoadedAlbum, AppError> {
    let items = state.repository.load_items(user).await?;
    let type_meta = state.repository.load_type_meta().await?;
    let overrides = state.repository.load_overrides(user).await?;
    let album = build_album(&state.series, &items, &type_meta, &overrides);
    let proposals = build_collection_proposals(&state.series, &items, &type_meta);
    Ok(LoadedAlbum {
        album,
        type_meta,
        proposals,
        items,
    })
}

fn require_user<'a>(state: &'a AppState, user: &str) -> Result<&'a config::UserConfig, AppError> {
    state
        .config
        .user(user)
        .ok_or_else(|| AppError::UnknownUser(user.to_owned()))
}

fn trusted_image_url(source: &str) -> Result<reqwest::Url, AppError> {
    let url = reqwest::Url::parse(source).map_err(|_| AppError::UnsafeImageUrl)?;
    let trusted = url.scheme() == "https"
        && url
            .host_str()
            .is_some_and(|host| host == "numista.com" || host.ends_with(".numista.com"));
    if trusted && url.username().is_empty() && url.password().is_none() {
        Ok(url)
    } else {
        Err(AppError::UnsafeImageUrl)
    }
}

fn validate_same_origin(headers: &HeaderMap, expected_origin: &str) -> Result<(), AppError> {
    let origin = headers
        .get(ORIGIN)
        .and_then(|value| value.to_str().ok())
        .ok_or(AppError::CrossOriginPost)?;
    if config::canonical_origin(origin).as_deref() == Some(expected_origin) {
        Ok(())
    } else {
        Err(AppError::CrossOriginPost)
    }
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let status = match &self {
            Self::UnknownUser(_)
            | Self::UnknownSeries(_)
            | Self::UnknownCollectionCatalog(_)
            | Self::UnavailableFollowedCollection
            | Self::UnknownSlot(_)
            | Self::UnknownItem(_)
            | Self::InvalidImageSide(_)
            | Self::MissingImage { .. } => StatusCode::NOT_FOUND,
            Self::CrossOriginPost => StatusCode::FORBIDDEN,
            Self::InvalidCollectionProposalPreference => StatusCode::BAD_REQUEST,
            Self::UnsafeImageUrl | Self::InvalidImageResponse => StatusCode::BAD_GATEWAY,
            Self::ImageTooLarge => StatusCode::PAYLOAD_TOO_LARGE,
            Self::Sync(_) => StatusCode::SERVICE_UNAVAILABLE,
            Self::ImageRequest(_) | Self::Repository(_) => StatusCode::INTERNAL_SERVER_ERROR,
        };
        if status.is_server_error() {
            tracing::error!(status = status.as_u16(), error = %self, "request failed");
            (status, "internal server error").into_response()
        } else {
            tracing::warn!(status = status.as_u16(), error = %self, "request rejected");
            (status, self.to_string()).into_response()
        }
    }
}

#[cfg(test)]
mod tests {
    use axum::body::{Body, to_bytes};
    use axum::http::{Request, StatusCode};
    use axum::response::IntoResponse;
    use tower::ServiceExt;

    use super::{
        AppConfig, AppError, AppState, Repository, RepositoryError, SyncService, build_router,
        trusted_image_url, validate_same_origin,
    };
    use sqlx::postgres::PgPoolOptions;
    use std::collections::BTreeMap;
    use std::sync::Arc;

    #[tokio::test]
    async fn router_serves_handwritten_css_without_database_or_network() {
        let pool = PgPoolOptions::new()
            .connect_lazy("postgres://unused:unused@localhost/unused")
            .unwrap();
        let state = AppState {
            config: AppConfig::parse("jose:1:a,padre:2:b", None, "http://localhost:8000").unwrap(),
            repository: Repository::new(pool.clone()),
            series: Arc::new(Vec::new()),
            collection_catalogs: Arc::new(Vec::new()),
            sync: SyncService::new(Repository::new(pool), BTreeMap::new()),
            image_client: reqwest::Client::new(),
        };
        let response = build_router(state)
            .oneshot(
                Request::builder()
                    .uri("/static/site.css")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::OK);
        assert_eq!(
            response.headers()["content-type"],
            "text/css; charset=utf-8"
        );
    }

    #[tokio::test]
    async fn proposal_preference_route_checks_origin_before_parsing_form_or_using_database() {
        let pool = PgPoolOptions::new()
            .connect_lazy("postgres://unused:unused@localhost/unused")
            .unwrap();
        let state = AppState {
            config: AppConfig::parse("jose:1:a,padre:2:b", None, "http://localhost:8000").unwrap(),
            repository: Repository::new(pool.clone()),
            series: Arc::new(Vec::new()),
            collection_catalogs: Arc::new(Vec::new()),
            sync: SyncService::new(Repository::new(pool), BTreeMap::new()),
            image_client: reqwest::Client::new(),
        };

        let response = build_router(state)
            .oneshot(
                Request::post("/u/jose/collection-proposal-preference")
                    .header("origin", "https://evil.example")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .body(Body::from("%not-valid"))
                    .unwrap(),
            )
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::FORBIDDEN);
    }

    #[tokio::test]
    async fn sync_route_rejects_cross_origin_requests_before_using_numista_or_database() {
        let pool = PgPoolOptions::new()
            .connect_lazy("postgres://unused:unused@localhost/unused")
            .unwrap();
        let state = AppState {
            config: AppConfig::parse("jose:1:a,padre:2:b", None, "http://localhost:8000").unwrap(),
            repository: Repository::new(pool.clone()),
            series: Arc::new(Vec::new()),
            collection_catalogs: Arc::new(Vec::new()),
            sync: SyncService::new(Repository::new(pool), BTreeMap::new()),
            image_client: reqwest::Client::new(),
        };

        let response = build_router(state)
            .oneshot(
                Request::post("/u/jose/sync")
                    .header("origin", "https://evil.example")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::FORBIDDEN);
    }

    #[tokio::test]
    async fn server_errors_do_not_expose_repository_diagnostics() {
        let response = AppError::Repository(RepositoryError::Database(sqlx::Error::RowNotFound))
            .into_response();
        let body = to_bytes(response.into_body(), usize::MAX).await.unwrap();

        assert_eq!(body.as_ref(), b"internal server error");
    }

    #[test]
    fn image_proxy_accepts_only_https_numista_hosts_without_credentials() {
        assert!(trusted_image_url("https://en.numista.com/catalogue/photos/a.jpg").is_ok());
        assert!(trusted_image_url("http://en.numista.com/a.jpg").is_err());
        assert!(trusted_image_url("https://numista.com.evil.example/a.jpg").is_err());
        assert!(trusted_image_url("https://user:pass@en.numista.com/a.jpg").is_err());
    }

    #[test]
    fn mutating_forms_require_the_configured_canonical_origin() {
        let mut headers = axum::http::HeaderMap::new();
        headers.insert("origin", "https://coindex.example".parse().unwrap());
        assert!(validate_same_origin(&headers, "https://coindex.example").is_ok());

        headers.insert("origin", "http://coindex.example".parse().unwrap());
        assert!(validate_same_origin(&headers, "https://coindex.example").is_err());
        headers.insert("origin", "https://coindex.example:8443".parse().unwrap());
        assert!(validate_same_origin(&headers, "https://coindex.example").is_err());
        headers.insert("origin", "https://evil.example".parse().unwrap());
        assert!(validate_same_origin(&headers, "https://coindex.example").is_err());
        headers.remove("origin");
        assert!(validate_same_origin(&headers, "https://coindex.example").is_err());
    }

    #[tokio::test]
    #[ignore = "set TEST_DATABASE_URL to run the isolated Postgres integration check"]
    async fn postgres_health_override_and_type_cache() {
        let database_url =
            std::env::var("TEST_DATABASE_URL").expect("TEST_DATABASE_URL must be set");
        let pool = PgPoolOptions::new()
            .max_connections(4)
            .connect(&database_url)
            .await
            .unwrap();
        let repository = Repository::new(pool.clone());
        let user_key = "coindex_backend_integration";
        let item_id = 8_000_000_001_u64;
        let type_id = 2_000_000_001_u32;
        sqlx::query!("DELETE FROM type_meta WHERE type_id = $1", type_id as i32)
            .execute(&pool)
            .await
            .unwrap();

        repository
            .upsert_override(user_key, item_id, Some("integration-slot"))
            .await
            .unwrap();
        repository.store_sync(user_key, &[], None).await.unwrap();
        assert_eq!(
            repository.load_overrides(user_key).await.unwrap()[0]
                .slot_id
                .as_ref()
                .unwrap()
                .to_string(),
            "integration-slot"
        );

        let claim = repository
            .claim_type_fetch(type_id)
            .await
            .unwrap()
            .expect("integration type id must be uncached");
        claim
            .cache(&numista::NumistaType {
                id: Some(type_id),
                title: Some("Integration type".to_owned()),
                ..numista::NumistaType::default()
            })
            .await
            .unwrap();
        assert!(repository.cached_type(type_id).await.unwrap().is_some());

        let app = build_router(AppState {
            config: AppConfig::parse(
                "jose:1:key-one,padre:2:key-two",
                Some("1500"),
                "http://localhost:8000",
            )
            .unwrap(),
            repository: repository.clone(),
            series: Arc::new(Vec::new()),
            collection_catalogs: Arc::new(Vec::new()),
            sync: SyncService::new(repository.clone(), BTreeMap::new()),
            image_client: reqwest::Client::new(),
        });
        let response = app
            .oneshot(
                Request::builder()
                    .uri("/health")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(response.status(), StatusCode::OK);

        sqlx::query!("DELETE FROM manual_overrides WHERE user_key = $1", user_key)
            .execute(&pool)
            .await
            .unwrap();
        sqlx::query!("DELETE FROM collected_items WHERE user_key = $1", user_key)
            .execute(&pool)
            .await
            .unwrap();
        sqlx::query!("DELETE FROM type_meta WHERE type_id = $1", type_id as i32)
            .execute(&pool)
            .await
            .unwrap();
    }
}
