use std::collections::BTreeSet;

use domain::{
    CatalogValidationError, CollectionCatalog, CollectionCatalogValidationError, Series,
    validate_catalog,
};
use thiserror::Error;

const SEEDS: [(&str, &str); 2] = [
    (
        "lunar-iii.json",
        include_str!("../../data/series/lunar-iii.json"),
    ),
    (
        "tudor-beasts.json",
        include_str!("../../data/series/tudor-beasts.json"),
    ),
];

const COLLECTION_CATALOG_SEEDS: [(&str, &str); 1] = [(
    "nikola-tesla-serbia-1oz.json",
    include_str!("../../data/collection-catalogs/nikola-tesla-serbia-1oz.json"),
)];

#[derive(Debug, Error)]
pub enum SeedError {
    #[error("cannot parse seed {file}: {source}")]
    Parse {
        file: &'static str,
        source: serde_json::Error,
    },
    #[error("seed catalog validation failed: {0}")]
    Catalog(#[from] CatalogValidationError),
    #[error("collection catalog seed {file} is invalid: {source}")]
    CollectionCatalog {
        file: &'static str,
        source: CollectionCatalogValidationError,
    },
    #[error("collection catalog id `{id}` is duplicated")]
    DuplicateCollectionCatalogId { id: String },
}

pub fn load_series() -> Result<Vec<Series>, SeedError> {
    let mut series = Vec::with_capacity(SEEDS.len());
    for (file, contents) in SEEDS {
        series.push(parse_seed(file, contents)?);
    }
    validate_catalog(&series)?;
    Ok(series)
}

pub fn load_collection_catalogs() -> Result<Vec<CollectionCatalog>, SeedError> {
    let mut ids = BTreeSet::new();
    let mut catalogs = Vec::with_capacity(COLLECTION_CATALOG_SEEDS.len());
    for (file, contents) in COLLECTION_CATALOG_SEEDS {
        let catalog: CollectionCatalog =
            serde_json::from_str(contents).map_err(|source| SeedError::Parse { file, source })?;
        catalog
            .validate()
            .map_err(|source| SeedError::CollectionCatalog { file, source })?;
        if !ids.insert(catalog.id.clone()) {
            return Err(SeedError::DuplicateCollectionCatalogId {
                id: catalog.id.to_string(),
            });
        }
        catalogs.push(catalog);
    }
    Ok(catalogs)
}

fn parse_seed(file: &'static str, contents: &str) -> Result<Series, SeedError> {
    serde_json::from_str(contents).map_err(|source| SeedError::Parse { file, source })
}

#[cfg(test)]
mod tests {
    use super::{load_collection_catalogs, load_series, parse_seed};

    #[test]
    fn embedded_seeds_form_a_valid_global_catalog() {
        let series = load_series().unwrap();

        assert_eq!(series.len(), 2);
    }

    #[test]
    fn embedded_collection_catalog_is_versioned_sourced_and_complete() {
        let catalogs = load_collection_catalogs().unwrap();

        assert_eq!(catalogs.len(), 1);
        let catalog = &catalogs[0];
        assert_eq!(catalog.id.as_str(), "nikola-tesla-serbia-1oz");
        assert_eq!(catalog.schema_version, 1);
        assert_eq!(catalog.family, "Nikola Tesla");
        assert_eq!(catalog.weight_millioz, 1_000);
        assert_eq!(catalog.finish, None);
        assert_eq!(
            catalog.source,
            "https://en.numista.com/catalogue/series.php?id=5303"
        );
        assert_eq!(catalog.members.len(), 12);
        assert_eq!(
            catalog
                .members
                .iter()
                .map(|member| member.numista_type_id)
                .collect::<Vec<_>>(),
            [
                150_352, 162_242, 195_591, 302_302, 334_411, 371_257, 359_331, 421_848, 421_849,
                448_067, 493_347, 493_329,
            ]
        );
    }

    #[test]
    fn seed_typo_reports_the_filename_and_unknown_field() {
        let error = parse_seed(
            "typo.json",
            r#"{
                "id":"field",
                "name":"Field",
                "mint":"Mint",
                "issuer_code":"field",
                "metal":"Silver",
                "notes":null,
                "incmplete":true,
                "slots":[]
            }"#,
        )
        .unwrap_err()
        .to_string();

        assert!(error.contains("typo.json"));
        assert!(error.contains("unknown field `incmplete`"));
    }
}
