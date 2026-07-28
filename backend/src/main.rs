use coindex_backend::bootstrap;
use shuttle_runtime::{SecretStore, Secrets};
use sqlx::PgPool;

#[shuttle_runtime::main]
async fn main(
    #[shuttle_shared_db::Postgres] pool: PgPool,
    #[Secrets] secrets: SecretStore,
) -> shuttle_axum::ShuttleAxum {
    let users = secrets
        .get("COINDEX_USERS")
        .ok_or_else(|| anyhow::anyhow!("missing required secret COINDEX_USERS"))?;
    let budget = secrets.get("NUMISTA_MONTHLY_BUDGET");
    let origin = secrets
        .get("COINDEX_ORIGIN")
        .ok_or_else(|| anyhow::anyhow!("missing required secret COINDEX_ORIGIN"))?;
    let router = bootstrap(pool, &users, budget.as_deref(), &origin)
        .await
        .map_err(anyhow::Error::from)?;
    Ok(router.into())
}
