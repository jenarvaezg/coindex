use std::collections::BTreeMap;

use domain::{
    CatalogValidationError, Finish, Matcher, Metal, ReleaseStatus, Series, SeriesId,
    SeriesValidationError, Slot, SlotId, validate_catalog,
};
use serde_json::Value;

fn valid_series() -> Series {
    Series {
        id: SeriesId::new("lunar-iii"),
        name: "Lunar Series III".into(),
        mint: "Perth Mint".into(),
        issuer_code: "australie".into(),
        metal: Metal::Silver,
        notes: None,
        incomplete: false,
        sources: BTreeMap::new(),
        slots: vec![Slot {
            id: SlotId::new("lunar-iii-2024-dragon-1oz"),
            label: "Dragón — 2024".into(),
            year: 2024,
            motif: "Dragón".into(),
            weight_oz: 1.0,
            finish: Finish::Bullion,
            release_status: ReleaseStatus::Issued,
            numista_type_ids: vec![],
            matchers: vec![],
        }],
    }
}

#[test]
fn complete_well_formed_series_is_valid() {
    assert_eq!(valid_series().validate(), Ok(()));
}

#[test]
fn duplicate_slot_ids_are_rejected() {
    let mut series = valid_series();
    series.slots.push(series.slots[0].clone());

    assert!(series.validate().is_err());
}

#[test]
fn identifiers_must_be_nonempty_lowercase_slugs() {
    for invalid_id in ["", "Lunar III", "-lunar", "lunar-", "lunar--iii"] {
        let mut series = valid_series();
        series.id = SeriesId::new(invalid_id);
        assert!(
            series.validate().is_err(),
            "`{invalid_id}` should not be accepted as a series id"
        );
    }

    let mut series = valid_series();
    series.slots[0].id = SlotId::new("Dragon 2024");
    assert!(series.validate().is_err());
}

#[test]
fn malformed_series_and_matchers_are_rejected() {
    let mut blank_name = valid_series();
    blank_name.name = " ".into();
    assert!(blank_name.validate().is_err());

    let mut invalid_weight = valid_series();
    invalid_weight.slots[0].weight_oz = 0.0;
    assert!(invalid_weight.validate().is_err());

    let mut duplicate_type_id = valid_series();
    duplicate_type_id.slots[0].numista_type_ids = vec![42, 42];
    assert!(duplicate_type_id.validate().is_err());

    let mut empty_matcher = valid_series();
    empty_matcher.slots[0].matchers.push(Matcher {
        issuer_code: None,
        year: None,
        weight_oz: None,
        finish: None,
        title_contains: vec![],
        confidence: 1.5,
        explanation: String::new(),
    });
    assert!(empty_matcher.validate().is_err());
}

#[test]
fn matcher_title_terms_default_when_the_json_omits_them() {
    let matcher: Matcher = serde_json::from_str(
        r#"{"year":2024,"confidence":0.8,"explanation":"year-only fallback"}"#,
    )
    .unwrap();

    assert!(matcher.title_contains.is_empty());
}

#[test]
fn series_deserializes_incomplete_and_slot_sources_as_runtime_data() {
    let series: Series = serde_json::from_str(
        r#"{
            "id":"field-series",
            "name":"Field Series",
            "mint":"Field Mint",
            "issuer_code":"field",
            "metal":"Silver",
            "notes":null,
            "incomplete":true,
            "sources":{"field-series-2026-one":"https://mint.example/one"},
            "slots":[{
                "id":"field-series-2026-one",
                "label":"One — 2026",
                "year":2026,
                "motif":"One",
                "weight_oz":1.0,
                "finish":"Bullion",
                "release_status":"Issued",
                "numista_type_ids":[],
                "matchers":[]
            }]
        }"#,
    )
    .unwrap();

    assert!(series.incomplete);
    assert_eq!(
        series
            .sources
            .get(&SlotId::new("field-series-2026-one"))
            .map(String::as_str),
        Some("https://mint.example/one")
    );
}

#[test]
fn seed_schema_rejects_unknown_fields_at_every_nested_level() {
    let mut with_matcher = valid_series();
    with_matcher.slots[0].matchers.push(Matcher {
        issuer_code: Some("australie".into()),
        year: Some(2024),
        weight_oz: None,
        finish: None,
        title_contains: Vec::new(),
        confidence: 0.8,
        explanation: "verified fallback".into(),
    });

    let mut top_level = serde_json::to_value(&with_matcher).unwrap();
    top_level
        .as_object_mut()
        .unwrap()
        .insert("incmplete".into(), Value::Bool(true));
    assert!(serde_json::from_value::<Series>(top_level).is_err());

    let mut slot_level = serde_json::to_value(&with_matcher).unwrap();
    slot_level["slots"][0]
        .as_object_mut()
        .unwrap()
        .insert("release_stats".into(), Value::String("Issued".into()));
    assert!(serde_json::from_value::<Series>(slot_level).is_err());

    let mut matcher_level = serde_json::to_value(&with_matcher).unwrap();
    matcher_level["slots"][0]["matchers"][0]
        .as_object_mut()
        .unwrap()
        .insert("confidnce".into(), Value::from(0.9));
    assert!(serde_json::from_value::<Series>(matcher_level).is_err());
}

#[test]
fn source_keys_must_reference_slots_in_the_same_series() {
    let mut series = valid_series();
    series.sources.insert(
        SlotId::new("lunar-iii-missing-slot"),
        "https://mint.example/missing".into(),
    );

    assert!(matches!(
        series.validate(),
        Err(SeriesValidationError::UnknownSourceSlot { slot_id })
            if slot_id == SlotId::new("lunar-iii-missing-slot")
    ));
}

#[test]
fn catalog_validation_rejects_ids_and_explicit_types_reused_across_series() {
    let first = valid_series();
    let mut duplicate_series = valid_series();
    duplicate_series.slots[0].id = SlotId::new("other-slot");
    assert!(matches!(
        validate_catalog(&[first.clone(), duplicate_series]),
        Err(CatalogValidationError::DuplicateSeriesId { .. })
    ));

    let mut duplicate_slot = valid_series();
    duplicate_slot.id = SeriesId::new("second-series");
    assert!(matches!(
        validate_catalog(&[first.clone(), duplicate_slot]),
        Err(CatalogValidationError::DuplicateSlotId { .. })
    ));

    let mut first_with_type = first;
    first_with_type.slots[0].numista_type_ids = vec![42];
    let mut duplicate_type = valid_series();
    duplicate_type.id = SeriesId::new("second-series");
    duplicate_type.slots[0].id = SlotId::new("second-series-slot");
    duplicate_type.slots[0].numista_type_ids = vec![42];
    assert!(matches!(
        validate_catalog(&[first_with_type, duplicate_type]),
        Err(CatalogValidationError::DuplicateNumistaTypeId { type_id: 42, .. })
    ));
}

#[test]
fn lunar_seed_has_the_twelve_specified_slots_and_release_states() {
    let series: Series =
        serde_json::from_str(include_str!("../../../data/series/lunar-iii.json")).unwrap();
    let expected = [
        (2019, "Cerdo"),
        (2020, "Ratón"),
        (2021, "Buey"),
        (2022, "Tigre"),
        (2023, "Conejo"),
        (2024, "Dragón"),
        (2025, "Serpiente"),
        (2026, "Caballo"),
        (2027, "Cabra"),
        (2028, "Mono"),
        (2029, "Gallo"),
        (2030, "Perro"),
    ];

    assert_eq!(series.slots.len(), expected.len());
    assert!(!series.incomplete);
    assert!(series.sources.is_empty());
    for (slot, (year, motif)) in series.slots.iter().zip(expected) {
        assert_eq!((slot.year, slot.motif.as_str()), (year, motif));
        assert_eq!(slot.weight_oz, 1.0);
        assert_eq!(slot.finish, Finish::Bullion);
        assert!(slot.numista_type_ids.is_empty());
        let expected_status = if year <= 2026 {
            ReleaseStatus::Issued
        } else {
            ReleaseStatus::Expected
        };
        assert_eq!(slot.release_status, expected_status);
        assert!(slot.matchers.iter().all(|matcher| {
            matcher.issuer_code.as_deref() == Some(series.issuer_code.as_str())
                && matcher.weight_oz == Some(slot.weight_oz)
                && matcher.finish == Some(slot.finish.clone())
        }));
    }
    series.validate().unwrap();
}

#[test]
fn tudor_seed_contains_only_verified_bullion_releases_and_is_marked_incomplete() {
    let source = include_str!("../../../data/series/tudor-beasts.json");
    let series: Series = serde_json::from_str(source).unwrap();

    assert!(series.incomplete);
    assert_eq!(series.sources.len(), 9);
    assert_eq!(series.slots.len(), 9);
    let expected = [
        (2022, "León de Inglaterra"),
        (2023, "Yale de Beaufort"),
        (2023, "Toro de Clarence"),
        (2024, "Unicornio de Seymour"),
        (2024, "Dragón Tudor"),
        (2025, "Pantera de la Reina"),
        (2025, "Galgo de Richmond"),
        (2026, "León de la Reina"),
        (2026, "Dragón Real"),
    ];
    for (slot, (year, motif)) in series.slots.iter().zip(expected) {
        assert_eq!((slot.year, slot.motif.as_str()), (year, motif));
        assert_eq!(slot.weight_oz, 2.0);
        assert_eq!(slot.finish, Finish::Bullion);
        assert_eq!(slot.release_status, ReleaseStatus::Issued);
        assert!(slot.numista_type_ids.is_empty());
        assert!(slot.matchers.iter().all(|matcher| {
            matcher.issuer_code.as_deref() == Some(series.issuer_code.as_str())
                && matcher.weight_oz == Some(slot.weight_oz)
                && matcher.finish == Some(slot.finish.clone())
        }));
        assert!(series.sources[&slot.id].starts_with("https://www.royalmint.com/"));
    }
    series.validate().unwrap();
    let lunar: Series =
        serde_json::from_str(include_str!("../../../data/series/lunar-iii.json")).unwrap();
    validate_catalog(&[lunar, series]).unwrap();
}
