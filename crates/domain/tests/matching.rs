use std::collections::BTreeMap;

use domain::{
    CollectedItem, Finish, ManualOverride, MatchSource, Matcher, Metal, ReleaseStatus, Series,
    SeriesId, Slot, SlotId, SlotStatus, TypeMeta, TypeMetaIndex, UnmatchedReason, build_album,
};

fn slot(id: &str, type_ids: Vec<u32>) -> Slot {
    Slot {
        id: SlotId::new(id),
        label: "Dragón — 2024".into(),
        year: 2024,
        motif: "Dragón".into(),
        weight_oz: 1.0,
        finish: Finish::Bullion,
        release_status: ReleaseStatus::Issued,
        numista_type_ids: type_ids,
        matchers: vec![],
    }
}

fn series(slots: Vec<Slot>) -> Series {
    Series {
        id: SeriesId::new("lunar-iii"),
        name: "Lunar Series III".into(),
        mint: "Perth Mint".into(),
        issuer_code: "australie".into(),
        metal: Metal::Silver,
        notes: None,
        incomplete: false,
        sources: BTreeMap::new(),
        slots,
    }
}

fn item(id: u64, type_id: u32) -> CollectedItem {
    CollectedItem {
        id,
        quantity: 1,
        type_id,
        title: None,
        issuer_code: None,
        issue_year: None,
        gregorian_year: None,
        grade: None,
        price: None,
        for_swap: None,
        collection_name: None,
    }
}

#[test]
fn explicit_type_id_places_an_item_in_its_curated_slot() {
    let album = build_album(
        &[series(vec![slot("dragon", vec![4242])])],
        &[item(7, 4242)],
        &TypeMetaIndex::new(),
        &[],
    );

    assert!(matches!(
        album.series[0].slots[0].status,
        SlotStatus::Owned { quantity: 1, .. }
    ));
}

#[test]
fn heuristic_places_an_item_when_curated_type_ids_do_not_match() {
    let mut dragon = slot("dragon", vec![]);
    dragon.matchers.push(Matcher {
        issuer_code: Some("australie".into()),
        year: Some(2024),
        weight_oz: Some(1.0),
        finish: Some(Finish::Bullion),
        title_contains: vec!["dragon".into()],
        confidence: 0.95,
        explanation: "issuer, year, weight, finish, and title".into(),
    });
    let mut metadata = TypeMetaIndex::new();
    metadata.insert(
        9898,
        TypeMeta {
            id: 9898,
            title: Some("Australian Lunar Dragon".into()),
            issuer_code: Some("australie".into()),
            min_year: Some(2024),
            max_year: Some(2024),
            weight_oz: Some(1.0),
            finish: Some(Finish::Bullion),
        },
    );

    let album = build_album(&[series(vec![dragon])], &[item(8, 9898)], &metadata, &[]);

    let SlotStatus::Owned { items, .. } = &album.series[0].slots[0].status else {
        panic!("heuristically matched slot should be owned");
    };
    assert!(matches!(
        items[0].match_source,
        Some(MatchSource::Heuristic {
            confidence: 0.95,
            ..
        })
    ));
}

#[test]
fn manual_override_wins_over_a_different_explicit_type_id_match() {
    let album = build_album(
        &[series(vec![
            slot("explicit-dragon", vec![4242]),
            slot("corrected-dragon", vec![]),
        ])],
        &[item(7, 4242)],
        &TypeMetaIndex::new(),
        &[ManualOverride {
            item_id: 7,
            slot_id: Some(SlotId::new("corrected-dragon")),
        }],
    );

    assert!(matches!(
        album.series[0].slots[0].status,
        SlotStatus::Missing
    ));
    let SlotStatus::Owned { items, .. } = &album.series[0].slots[1].status else {
        panic!("manually selected slot should be owned");
    };
    assert_eq!(items[0].match_source, Some(MatchSource::ManualOverride));
}

#[test]
fn item_without_a_matching_slot_is_preserved_as_unmatched() {
    let album = build_album(
        &[series(vec![slot("dragon", vec![4242])])],
        &[item(99, 9898)],
        &TypeMetaIndex::new(),
        &[],
    );

    assert_eq!(album.unmatched.len(), 1);
    assert_eq!(album.unmatched[0].item_id, 99);
    assert_eq!(album.unmatched[0].match_source, None);
    assert_eq!(
        album.unmatched[0].unmatched_reason,
        Some(UnmatchedReason::NoMatchingSlot)
    );
}

#[test]
fn multiple_items_in_one_slot_are_kept_as_duplicates_and_quantities_are_summed() {
    let mut duplicate = item(8, 4242);
    duplicate.quantity = 2;
    let album = build_album(
        &[series(vec![slot("dragon", vec![4242])])],
        &[item(7, 4242), duplicate],
        &TypeMetaIndex::new(),
        &[],
    );

    let SlotStatus::Owned { quantity, items } = &album.series[0].slots[0].status else {
        panic!("slot with duplicate items should be owned");
    };
    assert_eq!(*quantity, 3);
    assert_eq!(items.len(), 2);
}

#[test]
fn future_slot_without_an_item_is_not_reported_as_missing() {
    let mut future = slot("goat", vec![]);
    future.release_status = ReleaseStatus::Expected;
    let album = build_album(&[series(vec![future])], &[], &TypeMetaIndex::new(), &[]);

    assert!(matches!(
        album.series[0].slots[0].status,
        SlotStatus::NotYetIssued
    ));
}

#[test]
fn explicit_type_id_ambiguity_across_series_is_auditable_and_unmatched() {
    let first = series(vec![slot("first-dragon", vec![4242])]);
    let mut second = series(vec![slot("second-dragon", vec![4242])]);
    second.id = SeriesId::new("other-series");
    let album = build_album(
        &[first, second],
        &[item(7, 4242)],
        &TypeMetaIndex::new(),
        &[],
    );

    assert_eq!(
        album.unmatched[0].unmatched_reason,
        Some(UnmatchedReason::AmbiguousExplicitTypeId {
            slot_ids: vec![SlotId::new("first-dragon"), SlotId::new("second-dragon")]
        })
    );
    assert!(album.series.iter().all(|series| {
        series
            .slots
            .iter()
            .all(|slot| matches!(slot.status, SlotStatus::Missing))
    }));
}

#[test]
fn unique_highest_confidence_heuristic_wins_regardless_of_catalog_order() {
    let matching = |confidence, explanation: &str| Matcher {
        issuer_code: Some("australie".into()),
        year: Some(2024),
        weight_oz: Some(1.0),
        finish: Some(Finish::Bullion),
        title_contains: vec!["dragon".into()],
        confidence,
        explanation: explanation.into(),
    };
    let mut lower = slot("lower-confidence", vec![]);
    lower.matchers.push(matching(0.7, "weaker"));
    let mut winner = slot("winner", vec![]);
    winner.matchers.push(matching(0.95, "stronger"));
    let mut metadata = TypeMetaIndex::new();
    metadata.insert(
        9898,
        TypeMeta {
            id: 9898,
            title: Some("Australian Lunar Dragon".into()),
            issuer_code: Some("australie".into()),
            min_year: Some(2024),
            max_year: Some(2024),
            weight_oz: Some(1.0),
            finish: Some(Finish::Bullion),
        },
    );

    let album = build_album(
        &[series(vec![lower, winner])],
        &[item(8, 9898)],
        &metadata,
        &[],
    );

    assert!(matches!(
        album.series[0].slots[0].status,
        SlotStatus::Missing
    ));
    assert!(matches!(
        album.series[0].slots[1].status,
        SlotStatus::Owned { .. }
    ));
}

#[test]
fn tied_highest_confidence_heuristics_are_auditable_and_unmatched() {
    let matching = |explanation: &str| Matcher {
        issuer_code: None,
        year: Some(2024),
        weight_oz: None,
        finish: None,
        title_contains: vec![],
        confidence: 0.8,
        explanation: explanation.into(),
    };
    let mut first = slot("first-candidate", vec![]);
    first.matchers.push(matching("first"));
    let mut second = slot("second-candidate", vec![]);
    second.matchers.push(matching("second"));
    let mut candidate = item(8, 9898);
    candidate.issue_year = Some(2024);

    let album = build_album(
        &[series(vec![first, second])],
        &[candidate],
        &TypeMetaIndex::new(),
        &[],
    );

    assert_eq!(
        album.unmatched[0].unmatched_reason,
        Some(UnmatchedReason::AmbiguousHeuristic {
            confidence: 0.8,
            slot_ids: vec![
                SlotId::new("first-candidate"),
                SlotId::new("second-candidate")
            ],
        })
    );
}

#[test]
fn negative_and_dangling_manual_overrides_keep_distinct_audit_provenance() {
    let album = build_album(
        &[series(vec![slot("dragon", vec![4242])])],
        &[item(7, 4242), item(8, 4242)],
        &TypeMetaIndex::new(),
        &[
            ManualOverride {
                item_id: 7,
                slot_id: None,
            },
            ManualOverride {
                item_id: 8,
                slot_id: Some(SlotId::new("deleted-slot")),
            },
        ],
    );

    assert_eq!(
        album.unmatched[0].unmatched_reason,
        Some(UnmatchedReason::ExcludedByManualOverride)
    );
    assert_eq!(
        album.unmatched[1].unmatched_reason,
        Some(UnmatchedReason::InvalidManualOverride {
            slot_id: SlotId::new("deleted-slot"),
            matching_slots: 0,
        })
    );
}
