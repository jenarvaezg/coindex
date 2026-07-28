use std::collections::{BTreeSet, HashSet};

use async_trait::async_trait;
use domain::{
    CollectedItem as DomainItem, Finish, ManualOverride, SlotId, TypeMeta, TypeMetaIndex,
};
use numista::{ApiCall, BudgetGate, CallRecorder, CollectedItem, NumistaType, PolicyError};
use serde_json::Value;
use sqlx::{PgPool, Postgres, Transaction};
use thiserror::Error;

const BUDGET_LOCK_ID: i64 = 0x0043_4F49_4E44_4558;
const TYPE_FETCH_LOCK_PREFIX: i64 = 0x0054_5950_4500_0000;

#[derive(Clone)]
pub struct Repository {
    pool: PgPool,
}

#[derive(Clone)]
pub struct PostgresCallPolicy {
    pool: PgPool,
    monthly_budget: u32,
}

pub struct TypeFetchClaim {
    transaction: Transaction<'static, Postgres>,
    type_id: i32,
}

#[derive(Debug, Error)]
pub enum RepositoryError {
    #[error(transparent)]
    Database(#[from] sqlx::Error),
    #[error("cached JSON is invalid: {0}")]
    InvalidJson(#[from] serde_json::Error),
    #[error("Numista item has no id")]
    MissingItemId,
    #[error("Numista item {0} has no type id")]
    MissingTypeId(u64),
    #[error("Numista type metadata has no id")]
    MissingTypeMetaId,
    #[error("requested Numista type {requested}, but response contained type {actual}")]
    TypeMetadataMismatch { requested: u32, actual: u32 },
    #[error("numeric field `{field}` is outside the supported range: {value}")]
    NumericRange { field: &'static str, value: u64 },
}

impl Repository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub async fn load_items(&self, user_key: &str) -> Result<Vec<DomainItem>, RepositoryError> {
        let rows = sqlx::query!(
            "SELECT raw FROM collected_items WHERE user_key = $1 ORDER BY id",
            user_key
        )
        .fetch_all(&self.pool)
        .await?;

        rows.into_iter()
            .map(|row| {
                let item: CollectedItem = serde_json::from_value(row.raw)?;
                domain_item(&item)
            })
            .collect()
    }

    pub async fn load_type_meta(&self) -> Result<TypeMetaIndex, RepositoryError> {
        let rows = sqlx::query!("SELECT type_id, raw FROM type_meta ORDER BY type_id")
            .fetch_all(&self.pool)
            .await?;
        let mut index = TypeMetaIndex::new();
        for row in rows {
            let type_id = row.type_id;
            let meta: NumistaType = serde_json::from_value(row.raw)?;
            let id = u32::try_from(type_id).map_err(|_| RepositoryError::NumericRange {
                field: "type_id",
                value: type_id.unsigned_abs() as u64,
            })?;
            index.insert(id, domain_type_meta(id, &meta));
        }
        Ok(index)
    }

    pub async fn load_overrides(
        &self,
        user_key: &str,
    ) -> Result<Vec<ManualOverride>, RepositoryError> {
        let rows = sqlx::query!(
            "SELECT item_id, slot_id FROM manual_overrides WHERE user_key = $1 ORDER BY item_id",
            user_key
        )
        .fetch_all(&self.pool)
        .await?;
        rows.into_iter()
            .map(|row| {
                let item_id = row.item_id;
                Ok(ManualOverride {
                    item_id: u64::try_from(item_id).map_err(|_| RepositoryError::NumericRange {
                        field: "item_id",
                        value: item_id.unsigned_abs(),
                    })?,
                    slot_id: row.slot_id.map(SlotId::new),
                })
            })
            .collect()
    }

    pub async fn upsert_override(
        &self,
        user_key: &str,
        item_id: u64,
        slot_id: Option<&str>,
    ) -> Result<(), RepositoryError> {
        let item_id = to_i64("item_id", item_id)?;
        sqlx::query!(
            "INSERT INTO manual_overrides (user_key, item_id, slot_id, created_at)
             VALUES ($1, $2, $3, now())
             ON CONFLICT (user_key, item_id)
             DO UPDATE SET slot_id = EXCLUDED.slot_id, created_at = now()",
            user_key,
            item_id,
            slot_id
        )
        .execute(&self.pool)
        .await?;
        Ok(())
    }

    pub async fn missing_type_ids(
        &self,
        type_ids: impl IntoIterator<Item = u32>,
    ) -> Result<Vec<u32>, RepositoryError> {
        let unique = type_ids.into_iter().collect::<BTreeSet<_>>();
        if unique.is_empty() {
            return Ok(Vec::new());
        }
        let ids = unique
            .iter()
            .map(|id| {
                i32::try_from(*id).map_err(|_| RepositoryError::NumericRange {
                    field: "type_id",
                    value: u64::from(*id),
                })
            })
            .collect::<Result<Vec<_>, _>>()?;
        let cached = sqlx::query_scalar!(
            "SELECT type_id FROM type_meta WHERE type_id = ANY($1)",
            &ids[..]
        )
        .fetch_all(&self.pool)
        .await?;
        let cached = cached.into_iter().collect::<HashSet<_>>();
        Ok(ids
            .into_iter()
            .filter(|id| !cached.contains(id))
            .map(|id| id as u32)
            .collect())
    }

    pub async fn store_sync(
        &self,
        user_key: &str,
        items: &[CollectedItem],
        raw_items: Option<&[Value]>,
    ) -> Result<(), RepositoryError> {
        let mut tx = self.pool.begin().await?;
        sqlx::query!("DELETE FROM collected_items WHERE user_key = $1", user_key)
            .execute(&mut *tx)
            .await?;
        for item in items {
            let normalized = domain_item(item)?;
            let id = to_i64("item_id", normalized.id)?;
            let type_id =
                i32::try_from(normalized.type_id).map_err(|_| RepositoryError::NumericRange {
                    field: "type_id",
                    value: u64::from(normalized.type_id),
                })?;
            let quantity =
                i32::try_from(normalized.quantity).map_err(|_| RepositoryError::NumericRange {
                    field: "quantity",
                    value: u64::from(normalized.quantity),
                })?;
            let raw = raw_items
                .and_then(|values| {
                    values.iter().find(|value| {
                        value.get("id").and_then(Value::as_u64) == Some(normalized.id)
                    })
                })
                .cloned()
                .unwrap_or(serde_json::to_value(item)?);
            sqlx::query!(
                "INSERT INTO collected_items
                 (id, user_key, type_id, quantity, issue_year, grade, collection_name, raw, synced_at)
                 VALUES ($1, $2, $3, $4, $5, $6, $7, $8, now())",
                id,
                user_key,
                type_id,
                quantity,
                normalized.issue_year.or(normalized.gregorian_year),
                normalized.grade.as_deref(),
                normalized.collection_name.as_deref(),
                raw
            )
            .execute(&mut *tx)
            .await?;
        }
        tx.commit().await?;
        Ok(())
    }

    pub async fn claim_type_fetch(
        &self,
        type_id: u32,
    ) -> Result<Option<TypeFetchClaim>, RepositoryError> {
        let database_type_id =
            i32::try_from(type_id).map_err(|_| RepositoryError::NumericRange {
                field: "type_id",
                value: u64::from(type_id),
            })?;
        let mut transaction = self.pool.begin().await?;
        let lock_id = TYPE_FETCH_LOCK_PREFIX | i64::from(type_id);
        sqlx::query!("SELECT pg_advisory_xact_lock($1)", lock_id)
            .execute(&mut *transaction)
            .await?;
        let cached = sqlx::query_scalar!(
            r#"SELECT EXISTS(SELECT 1 FROM type_meta WHERE type_id = $1) as "cached!""#,
            database_type_id
        )
        .fetch_one(&mut *transaction)
        .await?;
        if cached {
            transaction.commit().await?;
            return Ok(None);
        }
        Ok(Some(TypeFetchClaim {
            transaction,
            type_id: database_type_id,
        }))
    }

    pub async fn cached_type(&self, type_id: u32) -> Result<Option<NumistaType>, RepositoryError> {
        let type_id = i32::try_from(type_id).map_err(|_| RepositoryError::NumericRange {
            field: "type_id",
            value: u64::from(type_id),
        })?;
        let raw = sqlx::query_scalar!("SELECT raw FROM type_meta WHERE type_id = $1", type_id)
            .fetch_optional(&self.pool)
            .await?;
        raw.map(serde_json::from_value)
            .transpose()
            .map_err(Into::into)
    }

    pub async fn monthly_usage(&self) -> Result<u32, RepositoryError> {
        let used = sqlx::query_scalar!(
            r#"SELECT count(*) as "used!" FROM api_call_log
             WHERE called_at >= date_trunc('month', now())
               AND called_at < date_trunc('month', now()) + interval '1 month'"#,
        )
        .fetch_one(&self.pool)
        .await?;
        Ok(u32::try_from(used).unwrap_or(u32::MAX))
    }
}

impl TypeFetchClaim {
    pub async fn cache(mut self, metadata: &NumistaType) -> Result<(), RepositoryError> {
        let actual = metadata.id.ok_or(RepositoryError::MissingTypeMetaId)?;
        let requested = self.type_id as u32;
        if actual != requested {
            return Err(RepositoryError::TypeMetadataMismatch { requested, actual });
        }
        let raw = metadata
            .raw_json()
            .cloned()
            .unwrap_or(serde_json::to_value(metadata)?);
        sqlx::query!(
            "INSERT INTO type_meta (type_id, raw, fetched_at)
             VALUES ($1, $2, now())
             ON CONFLICT (type_id) DO NOTHING",
            self.type_id,
            raw
        )
        .execute(&mut *self.transaction)
        .await?;
        self.transaction.commit().await?;
        Ok(())
    }
}

impl PostgresCallPolicy {
    pub fn new(pool: PgPool, monthly_budget: u32) -> Self {
        Self {
            pool,
            monthly_budget,
        }
    }

    async fn used(&self) -> Result<i64, sqlx::Error> {
        sqlx::query_scalar!(
            r#"SELECT count(*) as "used!" FROM api_call_log
             WHERE called_at >= date_trunc('month', now())
               AND called_at < date_trunc('month', now()) + interval '1 month'"#,
        )
        .fetch_one(&self.pool)
        .await
    }
}

#[async_trait]
impl BudgetGate for PostgresCallPolicy {
    async fn preflight(&self, _call: &ApiCall) -> Result<(), PolicyError> {
        let used = self
            .used()
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        if used >= i64::from(self.monthly_budget) {
            return Err(PolicyError::new(format!(
                "monthly Numista budget exhausted ({used}/{})",
                self.monthly_budget
            )));
        }
        Ok(())
    }
}

#[async_trait]
impl CallRecorder for PostgresCallPolicy {
    async fn record(&self, call: &ApiCall) -> Result<(), PolicyError> {
        let mut tx = self
            .pool
            .begin()
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        sqlx::query!("SELECT pg_advisory_xact_lock($1)", BUDGET_LOCK_ID)
            .execute(&mut *tx)
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        let used = sqlx::query_scalar!(
            r#"SELECT count(*) as "used!" FROM api_call_log
             WHERE called_at >= date_trunc('month', now())
               AND called_at < date_trunc('month', now()) + interval '1 month'"#,
        )
        .fetch_one(&mut *tx)
        .await
        .map_err(|error| PolicyError::new(error.to_string()))?;
        if used >= i64::from(self.monthly_budget) {
            return Err(PolicyError::new(format!(
                "monthly Numista budget exhausted ({used}/{})",
                self.monthly_budget
            )));
        }
        sqlx::query!(
            "INSERT INTO api_call_log (endpoint, called_at) VALUES ($1, now())",
            &call.endpoint
        )
        .execute(&mut *tx)
        .await
        .map_err(|error| PolicyError::new(error.to_string()))?;
        tx.commit()
            .await
            .map_err(|error| PolicyError::new(error.to_string()))?;
        Ok(())
    }
}

fn domain_item(item: &CollectedItem) -> Result<DomainItem, RepositoryError> {
    let id = item.id.ok_or(RepositoryError::MissingItemId)?;
    let item_type = item
        .item_type
        .as_ref()
        .and_then(|item_type| item_type.id)
        .ok_or(RepositoryError::MissingTypeId(id))?;
    Ok(DomainItem {
        id,
        quantity: item.quantity.unwrap_or(1).max(1),
        type_id: item_type,
        title: matching_title(
            item.item_type
                .as_ref()
                .and_then(|item_type| item_type.title.as_deref()),
        ),
        issuer_code: item
            .item_type
            .as_ref()
            .and_then(|item_type| item_type.issuer.as_ref())
            .and_then(|issuer| issuer.code.clone()),
        issue_year: item.issue.as_ref().and_then(|issue| issue.year),
        gregorian_year: item.issue.as_ref().and_then(|issue| issue.gregorian_year),
        grade: item.grade.clone(),
        price: item.price.as_ref().and_then(|price| price.value),
        for_swap: item.for_swap,
        collection_name: item
            .collection
            .as_ref()
            .and_then(|collection| collection.name.clone()),
    })
}

fn domain_type_meta(id: u32, meta: &NumistaType) -> TypeMeta {
    TypeMeta {
        id,
        title: matching_title(meta.title.as_deref()),
        issuer_code: meta.issuer.as_ref().and_then(|issuer| issuer.code.clone()),
        min_year: meta.min_year,
        max_year: meta.max_year,
        weight_oz: meta.weight.map(|grams| (grams / 31.103_476_8) as f32),
        finish: infer_finish(meta),
    }
}

fn infer_finish(meta: &NumistaType) -> Option<Finish> {
    let title = meta.title.as_deref()?.to_ascii_lowercase();
    if title.contains("proof") {
        Some(Finish::Proof)
    } else if title.contains("colour")
        || title.contains("color")
        || title.contains("coloread")
        || title.contains("coloriz")
        || is_lunar_colour_variant(&title, meta.series.as_deref())
    {
        Some(Finish::Coloured)
    } else if title.contains("gild") || title.contains("dorad") || title.contains("chapado en oro")
    {
        Some(Finish::Gilded)
    } else if title.contains("antiqu") || title.contains("acabado antiguo") {
        Some(Finish::Antiqued)
    } else if title.contains("bullion")
        || matches!(
            meta.series.as_deref(),
            Some("Lunar Series III" | "The Royal Tudor Beasts")
        )
    {
        Some(Finish::Bullion)
    } else {
        None
    }
}

fn is_lunar_colour_variant(title: &str, series: Option<&str>) -> bool {
    series == Some("Lunar Series III")
        && [
            "blue", "golden", "lilac", "purple", "red", "teal", "white", "yellow",
        ]
        .iter()
        .any(|colour| title.contains(&format!("year of the {colour} ")))
}

fn matching_title(title: Option<&str>) -> Option<String> {
    let title = title?;
    let aliases: BTreeSet<&str> = title
        .to_lowercase()
        .split(|character: char| !character.is_alphanumeric())
        .filter_map(|word| match word {
            "buey" => Some("ox"),
            "caballo" => Some("horse"),
            "cabra" => Some("goat"),
            "cerdo" => Some("pig"),
            "conejo" => Some("rabbit"),
            "dragón" => Some("dragon"),
            "galgo" => Some("greyhound"),
            "gallo" => Some("rooster"),
            "inglaterra" => Some("england"),
            "león" => Some("lion"),
            "mono" => Some("monkey"),
            "pantera" => Some("panther"),
            "perro" => Some("dog"),
            "ratón" => Some("mouse"),
            "real" => Some("royal"),
            "reina" => Some("queen"),
            "serpiente" => Some("snake"),
            "tigre" => Some("tiger"),
            "toro" => Some("bull"),
            "unicornio" => Some("unicorn"),
            _ => None,
        })
        .collect();
    if aliases.is_empty() {
        Some(title.to_owned())
    } else {
        Some(format!(
            "{title} [{}]",
            aliases.into_iter().collect::<Vec<_>>().join(" ")
        ))
    }
}

fn to_i64(field: &'static str, value: u64) -> Result<i64, RepositoryError> {
    i64::try_from(value).map_err(|_| RepositoryError::NumericRange { field, value })
}

#[cfg(test)]
mod tests {
    use domain::{Finish, SlotStatus, TypeMetaIndex, build_album};
    use numista::{CollectedItem, NumistaType};

    use crate::seeds::load_series;

    use super::{domain_item, domain_type_meta, infer_finish, matching_title};

    #[test]
    fn ordinary_catalog_titles_do_not_invent_a_finish() {
        let metadata = NumistaType {
            title: Some("1 Dollar – Lunar Series III".to_owned()),
            ..NumistaType::default()
        };

        assert_eq!(infer_finish(&metadata), None);
    }

    #[test]
    fn spanish_lunar_motifs_add_every_committed_english_alias() {
        let cases = [
            ("Cerdo", "pig"),
            ("Ratón", "mouse"),
            ("Buey", "ox"),
            ("Tigre", "tiger"),
            ("Conejo", "rabbit"),
            ("Dragón", "dragon"),
            ("Serpiente", "snake"),
            ("Caballo", "horse"),
            ("Cabra", "goat"),
            ("Mono", "monkey"),
            ("Gallo", "rooster"),
            ("Perro", "dog"),
        ];

        for (spanish, english) in cases {
            let title = matching_title(Some(spanish)).unwrap();
            assert!(
                title.contains(english),
                "expected `{spanish}` to add alias `{english}`, got `{title}`"
            );
        }
    }

    #[test]
    fn production_metadata_only_matches_the_committed_bullion_slot_for_the_ordinary_variant() {
        let series = load_series().unwrap();
        let candidates = [
            (
                386_213,
                "1 Dólar - Isabel II (Año del Dragón; Plata)",
                31.107,
                Finish::Bullion,
                true,
            ),
            (
                404_044,
                "1 Dollar - Elizabeth II (Year of the Dragon - Silver Proof High Relief)",
                31.107,
                Finish::Proof,
                false,
            ),
            (
                394_043,
                "1 Dollar - Elizabeth II (Year of the Dragon - Coloured)",
                31.107,
                Finish::Coloured,
                false,
            ),
            (
                404_285,
                "1 Dollar - Elizabeth II (Year of the Dragon - Silver Gilded)",
                31.107,
                Finish::Gilded,
                false,
            ),
            (
                482_185,
                "2 Dollars - Elizabeth II (Year of the Dragon - Silver Antiqued)",
                62.213,
                Finish::Antiqued,
                false,
            ),
        ];

        for (type_id, title, weight, expected_finish, should_match) in candidates {
            let raw_type = serde_json::json!({
                "id": type_id,
                "title": title,
                "issuer": {"code": "australie", "name": "Australia"},
                "min_year": 2024,
                "max_year": 2024,
                "weight": weight,
                "series": "Lunar Series III"
            });
            let metadata: NumistaType = serde_json::from_value(raw_type).unwrap();
            let adapted_metadata = domain_type_meta(type_id, &metadata);
            assert_eq!(adapted_metadata.finish, Some(expected_finish));

            let raw_item = serde_json::json!({
                "id": u64::from(type_id),
                "quantity": 1,
                "type": {
                    "id": type_id,
                    "title": title,
                    "issuer": {"code": "australie", "name": "Australia"}
                },
                "issue": {"year": 2024, "gregorian_year": 2024}
            });
            let item: CollectedItem = serde_json::from_value(raw_item).unwrap();
            let item = domain_item(&item).unwrap();
            let metadata = TypeMetaIndex::from([(type_id, adapted_metadata)]);

            let album = build_album(&series, &[item], &metadata, &[]);
            let dragon = album
                .series
                .iter()
                .flat_map(|series| &series.slots)
                .find(|slot| slot.slot.id.as_str() == "lunar-iii-2024-dragon-1oz")
                .unwrap();

            assert_eq!(
                matches!(dragon.status, SlotStatus::Owned { .. }),
                should_match,
                "unexpected match result for Numista type {type_id}: {title}"
            );
        }
    }
}
