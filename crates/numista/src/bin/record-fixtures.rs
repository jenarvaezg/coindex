use async_trait::async_trait;
use numista::{
    ApiCall, BudgetGate, CallRecorder, ClientConfig, NumistaClient, PolicyError, SyncCallEstimate,
    SyncPlan,
};
use serde_json::Value;
use sqlx::PgPool;
use sqlx::postgres::PgPoolOptions;
use std::env;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::path::{Path, PathBuf};
use std::process::ExitCode;
use std::sync::Arc;

const CONFIRM_FLAG: &str = "--confirm-live-api";
const DEFAULT_MONTHLY_BUDGET: u32 = 1_500;
const BUDGET_LOCK_ID: i64 = 0x0043_4F49_4E44_4558;

#[derive(Debug, Default)]
struct Arguments {
    confirm_live_api: bool,
    dry_run: bool,
    user_id: Option<u64>,
    type_ids: Vec<u32>,
    output_dir: PathBuf,
}

struct ConsoleRecorder {
    policy: Arc<PostgresCallPolicy>,
}

struct PostgresCallPolicy {
    pool: PgPool,
    monthly_budget: u32,
}

impl PostgresCallPolicy {
    fn new(pool: PgPool, monthly_budget: u32) -> Self {
        Self {
            pool,
            monthly_budget,
        }
    }

    async fn monthly_usage(&self) -> Result<i64, PolicyError> {
        sqlx::query_scalar!(
            "SELECT count(*) AS \"count!\" FROM api_call_log
             WHERE called_at >= date_trunc('month', now())
               AND called_at < date_trunc('month', now()) + interval '1 month'"
        )
        .fetch_one(&self.pool)
        .await
        .map_err(|error| PolicyError::new(error.to_string()))
    }
}

#[async_trait]
impl BudgetGate for PostgresCallPolicy {
    async fn preflight(&self, _call: &ApiCall) -> Result<(), PolicyError> {
        let used = self.monthly_usage().await?;
        if used >= i64::from(self.monthly_budget) {
            return Err(budget_exhausted(used, self.monthly_budget));
        }
        Ok(())
    }
}

#[async_trait]
impl CallRecorder for PostgresCallPolicy {
    async fn record(&self, call: &ApiCall) -> Result<(), PolicyError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        sqlx::query!("SELECT pg_advisory_xact_lock($1)", BUDGET_LOCK_ID)
            .execute(&mut *transaction)
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        let used: i64 = sqlx::query_scalar!(
            "SELECT count(*) AS \"count!\" FROM api_call_log
             WHERE called_at >= date_trunc('month', now())
               AND called_at < date_trunc('month', now()) + interval '1 month'"
        )
        .fetch_one(&mut *transaction)
        .await
        .map_err(|error| PolicyError::new(error.to_string()))?;
        if used >= i64::from(self.monthly_budget) {
            return Err(budget_exhausted(used, self.monthly_budget));
        }
        sqlx::query!(
            "INSERT INTO api_call_log (endpoint, called_at) VALUES ($1, now())",
            &call.endpoint
        )
        .execute(&mut *transaction)
        .await
        .map_err(|error| PolicyError::new(error.to_string()))?;
        transaction
            .commit()
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        Ok(())
    }
}

#[async_trait]
impl CallRecorder for ConsoleRecorder {
    async fn record(&self, call: &ApiCall) -> Result<(), PolicyError> {
        self.policy.record(call).await?;
        eprintln!(
            "Reserved Numista call in authoritative Postgres log: {}",
            call.endpoint
        );
        Ok(())
    }
}

#[tokio::main]
async fn main() -> ExitCode {
    match run().await {
        Ok(()) => ExitCode::SUCCESS,
        Err(error) => {
            eprintln!("record-fixtures: {error}");
            ExitCode::FAILURE
        }
    }
}

async fn run() -> Result<(), String> {
    let mut arguments = parse_arguments(env::args().skip(1))?;
    deduplicate_type_ids(&mut arguments.type_ids);

    let plan = SyncPlan {
        fetch_collection: arguments.user_id.is_some(),
        missing_type_ids: arguments.type_ids.clone(),
    };
    let estimate = SyncCallEstimate::for_plan(&plan, false);
    print_estimate(estimate);

    if arguments.dry_run {
        eprintln!(
            "Dry-run is database-free: it does not inspect authoritative Postgres usage or \
             remaining monthly budget."
        );
        return Ok(());
    }
    if !arguments.confirm_live_api {
        return Err(format!(
            "refusing live requests without {CONFIRM_FLAG}; use --dry-run to estimate only"
        ));
    }
    if estimate.total == 0 {
        return Err("nothing selected; provide --user-id and/or --type-id".to_owned());
    }

    let monthly_budget =
        parse_environment_or_default("NUMISTA_MONTHLY_BUDGET", DEFAULT_MONTHLY_BUDGET)?;
    let database_url = budget_database_url()?;
    let pool = PgPoolOptions::new()
        .max_connections(2)
        .connect(&database_url)
        .await
        .map_err(|error| format!("could not connect to authoritative budget database: {error}"))?;
    let policy = Arc::new(PostgresCallPolicy::new(pool, monthly_budget));
    let monthly_usage = policy
        .monthly_usage()
        .await
        .map_err(|error| error.to_string())?;
    let remaining = i64::from(monthly_budget)
        .saturating_sub(monthly_usage)
        .max(0);
    eprintln!(
        "Authoritative Postgres budget: {monthly_usage}/{monthly_budget} used, \
         {remaining} remaining"
    );
    if i64::from(estimate.total) > remaining {
        return Err(format!(
            "planned invocation needs {} calls but authoritative monthly budget has only \
             {remaining} remaining",
            estimate.total
        ));
    }

    let api_key = env::var("NUMISTA_API_KEY")
        .map_err(|_| "NUMISTA_API_KEY is required for live recording".to_owned())?;
    let recorder = Arc::new(ConsoleRecorder {
        policy: policy.clone(),
    });
    let client = NumistaClient::with_reqwest(ClientConfig::new(api_key), policy, recorder)
        .map_err(|error| error.to_string())?;

    fs::create_dir_all(&arguments.output_dir).map_err(|error| {
        format!(
            "could not create {}: {error}",
            arguments.output_dir.display()
        )
    })?;

    let mut captures = Vec::new();
    if let Some(user_id) = arguments.user_id {
        let collection = client
            .fetch_collected_items(user_id)
            .await
            .map_err(|error| error.to_string())?;
        let raw = collection.into_raw_json().ok_or_else(|| {
            "collection response did not retain its exact raw JSON; refusing replacement".to_owned()
        })?;
        captures.push((arguments.output_dir.join("collected_items.json"), raw));
    }

    for type_id in arguments.type_ids {
        let metadata = client
            .fetch_type_metadata(type_id)
            .await
            .map_err(|error| error.to_string())?;
        let raw = metadata.into_raw_json().ok_or_else(|| {
            format!(
                "type {type_id} response did not retain its exact raw JSON; refusing replacement"
            )
        })?;
        captures.push((
            arguments.output_dir.join(format!("type_{type_id}_es.json")),
            raw,
        ));
    }

    stage_and_replace(captures)
}

fn parse_arguments(arguments: impl Iterator<Item = String>) -> Result<Arguments, String> {
    let mut parsed = Arguments {
        output_dir: PathBuf::from("fixtures/numista"),
        ..Arguments::default()
    };
    let mut arguments = arguments.peekable();

    while let Some(argument) = arguments.next() {
        match argument.as_str() {
            CONFIRM_FLAG => parsed.confirm_live_api = true,
            "--dry-run" => parsed.dry_run = true,
            "--user-id" => {
                parsed.user_id = Some(parse_next(&mut arguments, "--user-id")?);
            }
            "--type-id" => {
                parsed
                    .type_ids
                    .push(parse_next(&mut arguments, "--type-id")?);
            }
            "--output-dir" => {
                parsed.output_dir = PathBuf::from(
                    arguments
                        .next()
                        .ok_or_else(|| "--output-dir requires a path".to_owned())?,
                );
            }
            "--help" | "-h" => {
                return Err(format!(
                    "usage: record-fixtures [{CONFIRM_FLAG} | --dry-run] \
                     [--user-id ID] [--type-id ID]... [--output-dir PATH]"
                ));
            }
            unknown => return Err(format!("unknown argument `{unknown}`")),
        }
    }

    Ok(parsed)
}

fn parse_next<T>(arguments: &mut impl Iterator<Item = String>, flag: &str) -> Result<T, String>
where
    T: std::str::FromStr,
    T::Err: std::fmt::Display,
{
    let value = arguments
        .next()
        .ok_or_else(|| format!("{flag} requires a value"))?;
    value
        .parse()
        .map_err(|error| format!("invalid value for {flag}: {error}"))
}

fn deduplicate_type_ids(type_ids: &mut Vec<u32>) {
    type_ids.sort_unstable();
    type_ids.dedup();
}

fn print_estimate(estimate: SyncCallEstimate) {
    eprintln!(
        "Planned-attempt ceiling: {} calls (oauth={}, collection={}, type_metadata={})",
        estimate.total, estimate.oauth_token, estimate.collected_items, estimate.type_metadata
    );
    eprintln!(
        "This is exact for the explicitly supplied IDs in a fresh recorder process; actual calls \
         may be lower after an early failure, and type IDs not supplied here are not estimated."
    );
}

fn parse_environment_or_default<T>(name: &str, default: T) -> Result<T, String>
where
    T: std::str::FromStr,
    T::Err: std::fmt::Display,
{
    match env::var(name) {
        Ok(value) => value
            .parse()
            .map_err(|error| format!("invalid {name}: {error}")),
        Err(env::VarError::NotPresent) => Ok(default),
        Err(error) => Err(format!("could not read {name}: {error}")),
    }
}

fn budget_database_url() -> Result<String, String> {
    env::var("NUMISTA_BUDGET_DATABASE_URL")
        .or_else(|_| env::var("DATABASE_URL"))
        .map_err(|_| {
            "confirmed live recording requires NUMISTA_BUDGET_DATABASE_URL or DATABASE_URL \
             pointing to the backend Postgres database"
                .to_owned()
        })
}

fn budget_exhausted(used: i64, monthly_budget: u32) -> PolicyError {
    PolicyError::new(format!(
        "monthly Numista budget exhausted ({used}/{monthly_budget})"
    ))
}

fn stage_and_replace(captures: Vec<(PathBuf, Value)>) -> Result<(), String> {
    let Some((first_destination, _)) = captures.first() else {
        return Ok(());
    };
    let output_directory = first_destination.parent().ok_or_else(|| {
        format!(
            "fixture path has no parent: {}",
            first_destination.display()
        )
    })?;
    if captures
        .iter()
        .any(|(destination, _)| destination.parent() != Some(output_directory))
    {
        return Err("all fixture destinations must share one output directory".to_owned());
    }

    let directory_name = output_directory
        .file_name()
        .and_then(|name| name.to_str())
        .ok_or_else(|| format!("invalid output directory {}", output_directory.display()))?;
    let parent = output_directory.parent().ok_or_else(|| {
        format!(
            "output directory has no parent: {}",
            output_directory.display()
        )
    })?;
    let stage = parent.join(format!(".{directory_name}.record-fixtures-stage"));
    let backup = parent.join(format!(".{directory_name}.record-fixtures-backup"));
    let marker = parent.join(format!(".{directory_name}.record-fixtures-swap"));

    recover_fixture_swap(output_directory, &stage, &backup, &marker)?;
    copy_directory(output_directory, &stage)?;

    for (destination, raw) in &captures {
        let file_name = destination
            .file_name()
            .ok_or_else(|| format!("invalid fixture path {}", destination.display()))?;
        let staged_destination = stage.join(file_name);
        if staged_destination.exists() {
            fs::remove_file(&staged_destination).map_err(|error| {
                format!(
                    "could not replace copied fixture {} in stage: {error}",
                    staged_destination.display()
                )
            })?;
        }
        stage_raw_json(&staged_destination, raw)?;
    }
    sync_directory(&stage)?;
    stage_raw_json(&marker, &serde_json::json!({"state": "prepared"}))?;
    sync_directory(parent)?;

    fs::rename(output_directory, &backup).map_err(|error| {
        format!(
            "could not preserve current fixture set {}: {error}",
            output_directory.display()
        )
    })?;
    if let Err(error) = fs::rename(&stage, output_directory) {
        let rollback = fs::rename(&backup, output_directory);
        return Err(format!(
            "could not install staged fixture set: {error}; rollback result: {rollback:?}"
        ));
    }
    sync_directory(parent)?;

    fs::remove_file(&marker)
        .map_err(|error| format!("could not commit fixture-set swap: {error}"))?;
    sync_directory(parent)?;
    fs::remove_dir_all(&backup)
        .map_err(|error| format!("could not remove committed fixture backup: {error}"))?;

    for (destination, _) in captures {
        eprintln!("Wrote exact raw JSON to {}", destination.display());
    }
    Ok(())
}

fn stage_raw_json(path: &Path, raw: &Value) -> Result<(), String> {
    let mut bytes = serde_json::to_vec_pretty(raw)
        .map_err(|error| format!("could not encode raw JSON for {}: {error}", path.display()))?;
    bytes.push(b'\n');
    let mut file = OpenOptions::new()
        .create_new(true)
        .write(true)
        .open(path)
        .map_err(|error| format!("could not stage {}: {error}", path.display()))?;
    file.write_all(&bytes)
        .and_then(|()| file.flush())
        .and_then(|()| file.sync_all())
        .map_err(|error| format!("could not durably stage {}: {error}", path.display()))
}

fn recover_fixture_swap(
    output: &Path,
    stage: &Path,
    backup: &Path,
    marker: &Path,
) -> Result<(), String> {
    if marker.exists() {
        if backup.exists() {
            if output.exists() {
                if stage.exists() {
                    fs::remove_dir_all(stage)
                        .map_err(|error| format!("could not clear stale stage: {error}"))?;
                }
                fs::rename(output, stage)
                    .map_err(|error| format!("could not stage interrupted new set: {error}"))?;
                fs::rename(backup, output).map_err(|error| {
                    format!("could not restore interrupted fixture set: {error}")
                })?;
                fs::remove_dir_all(stage)
                    .map_err(|error| format!("could not clear interrupted new set: {error}"))?;
            } else {
                fs::rename(backup, output)
                    .map_err(|error| format!("could not restore fixture backup: {error}"))?;
                if stage.exists() {
                    fs::remove_dir_all(stage)
                        .map_err(|error| format!("could not clear stale stage: {error}"))?;
                }
            }
        } else if stage.exists() {
            fs::remove_dir_all(stage)
                .map_err(|error| format!("could not clear uncommitted fixture stage: {error}"))?;
        }
        fs::remove_file(marker)
            .map_err(|error| format!("could not clear fixture transaction marker: {error}"))?;
    } else {
        if backup.exists() {
            if output.exists() {
                fs::remove_dir_all(backup).map_err(|error| {
                    format!("could not clear committed fixture backup: {error}")
                })?;
            } else {
                fs::rename(backup, output).map_err(|error| {
                    format!("could not restore orphaned fixture backup: {error}")
                })?;
            }
        }
        if stage.exists() {
            fs::remove_dir_all(stage)
                .map_err(|error| format!("could not clear orphaned fixture stage: {error}"))?;
        }
    }
    Ok(())
}

fn copy_directory(source: &Path, destination: &Path) -> Result<(), String> {
    fs::create_dir(destination).map_err(|error| {
        format!(
            "could not create fixture stage {}: {error}",
            destination.display()
        )
    })?;
    for entry in fs::read_dir(source).map_err(|error| {
        format!(
            "could not read fixture directory {}: {error}",
            source.display()
        )
    })? {
        let entry = entry.map_err(|error| format!("could not read fixture entry: {error}"))?;
        let file_type = entry
            .file_type()
            .map_err(|error| format!("could not inspect fixture entry: {error}"))?;
        let target = destination.join(entry.file_name());
        if file_type.is_dir() {
            copy_directory(&entry.path(), &target)?;
        } else if file_type.is_file() {
            fs::copy(entry.path(), &target).map_err(|error| {
                format!("could not stage fixture {}: {error}", target.display())
            })?;
            OpenOptions::new()
                .read(true)
                .open(&target)
                .and_then(|file| file.sync_all())
                .map_err(|error| {
                    format!(
                        "could not sync staged fixture {}: {error}",
                        target.display()
                    )
                })?;
        } else {
            return Err(format!(
                "fixture directory contains unsupported entry {}",
                entry.path().display()
            ));
        }
    }
    sync_directory(destination)
}

fn sync_directory(path: &Path) -> Result<(), String> {
    OpenOptions::new()
        .read(true)
        .open(path)
        .and_then(|directory| directory.sync_all())
        .map_err(|error| format!("could not sync directory {}: {error}", path.display()))
}

#[cfg(test)]
mod tests {
    use super::{deduplicate_type_ids, recover_fixture_swap, stage_and_replace};
    use serde_json::json;
    use std::fs;

    #[test]
    fn live_type_ids_are_deduplicated_before_fetching() {
        let mut type_ids = vec![11331, 420, 11331, 420, 99];
        deduplicate_type_ids(&mut type_ids);
        assert_eq!(type_ids, vec![99, 420, 11331]);
    }

    #[test]
    fn raw_outputs_are_all_staged_before_replacement() {
        let directory = tempfile::tempdir().unwrap();
        let collection_path = directory.path().join("collected_items.json");
        let type_path = directory.path().join("type_420_es.json");
        fs::write(&collection_path, b"{\"old\":true}\n").unwrap();
        fs::write(&type_path, b"{\"old\":true}\n").unwrap();
        let collection_raw = json!({"items": [{"type": {"id": 420}}]});
        let type_raw = json!({"id": 420, "commemorated_event": "original-key"});

        stage_and_replace(vec![
            (collection_path.clone(), collection_raw.clone()),
            (type_path.clone(), type_raw.clone()),
        ])
        .unwrap();

        let recorded_collection: serde_json::Value =
            serde_json::from_slice(&fs::read(collection_path).unwrap()).unwrap();
        let recorded_type: serde_json::Value =
            serde_json::from_slice(&fs::read(type_path).unwrap()).unwrap();
        assert_eq!(recorded_collection, collection_raw);
        assert_eq!(recorded_type, type_raw);
        assert_eq!(
            fs::read_dir(directory.path()).unwrap().count(),
            2,
            "no staged files remain"
        );
    }

    #[test]
    fn interrupted_set_swap_rolls_back_as_one_unit() {
        let parent = tempfile::tempdir().unwrap();
        let output = parent.path().join("numista");
        let stage = parent.path().join(".numista.record-fixtures-stage");
        let backup = parent.path().join(".numista.record-fixtures-backup");
        let marker = parent.path().join(".numista.record-fixtures-swap");
        fs::create_dir(&output).unwrap();
        fs::create_dir(&backup).unwrap();
        fs::write(output.join("one.json"), b"{\"version\":\"new\"}").unwrap();
        fs::write(output.join("two.json"), b"{\"version\":\"new\"}").unwrap();
        fs::write(backup.join("one.json"), b"{\"version\":\"old\"}").unwrap();
        fs::write(backup.join("two.json"), b"{\"version\":\"old\"}").unwrap();
        fs::write(&marker, b"{\"state\":\"prepared\"}\n").unwrap();

        recover_fixture_swap(&output, &stage, &backup, &marker).unwrap();

        assert!(
            String::from_utf8(fs::read(output.join("one.json")).unwrap())
                .unwrap()
                .contains("old")
        );
        assert!(
            String::from_utf8(fs::read(output.join("two.json")).unwrap())
                .unwrap()
                .contains("old")
        );
        assert!(!stage.exists());
        assert!(!backup.exists());
        assert!(!marker.exists());
    }
}
