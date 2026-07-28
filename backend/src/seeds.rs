use domain::{CatalogValidationError, Series, validate_catalog};
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

#[derive(Debug, Error)]
pub enum SeedError {
    #[error("cannot parse seed {file}: {source}")]
    Parse {
        file: &'static str,
        source: serde_json::Error,
    },
    #[error("seed catalog validation failed: {0}")]
    Catalog(#[from] CatalogValidationError),
}

pub fn load_series() -> Result<Vec<Series>, SeedError> {
    let mut series = Vec::with_capacity(SEEDS.len());
    for (file, contents) in SEEDS {
        series.push(parse_seed(file, contents)?);
    }
    validate_catalog(&series)?;
    Ok(series)
}

fn parse_seed(file: &'static str, contents: &str) -> Result<Series, SeedError> {
    serde_json::from_str(contents).map_err(|source| SeedError::Parse { file, source })
}

#[cfg(test)]
mod tests {
    use super::{load_series, parse_seed};

    #[test]
    fn embedded_seeds_form_a_valid_global_catalog() {
        let series = load_series().unwrap();

        assert_eq!(series.len(), 2);
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
