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
        let value: Series =
            serde_json::from_str(contents).map_err(|source| SeedError::Parse { file, source })?;
        series.push(value);
    }
    validate_catalog(&series)?;
    Ok(series)
}

#[cfg(test)]
mod tests {
    use super::load_series;

    #[test]
    fn embedded_seeds_form_a_valid_global_catalog() {
        let series = load_series().unwrap();

        assert_eq!(series.len(), 2);
    }
}
