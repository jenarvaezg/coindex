use domain::{
    CollectedItem, Finish, Metal, ReleaseStatus, Series, SeriesId, Slot, SlotId, TypeMeta,
    TypeMetaIndex, build_collection_proposals, normalize_weight_millioz,
};

fn ounces(grams: f32) -> f32 {
    grams / 31.103_477
}

#[test]
fn common_ounce_weights_snap_without_turning_thirty_grams_into_one_ounce() {
    assert_eq!(normalize_weight_millioz(ounces(7.777)), Some(250));
    assert_eq!(normalize_weight_millioz(ounces(31.1)), Some(1_000));
    assert_eq!(normalize_weight_millioz(ounces(31.21)), Some(1_000));
    assert_eq!(normalize_weight_millioz(ounces(62.42)), Some(2_000));
    assert_eq!(normalize_weight_millioz(ounces(30.0)), Some(965));
}

fn item(id: u64, type_id: u32, quantity: u32) -> CollectedItem {
    CollectedItem {
        id,
        quantity,
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

fn metadata(id: u32, family: &str, grams: f32, finish: Option<Finish>) -> TypeMeta {
    TypeMeta {
        id,
        title: None,
        display_title: None,
        family: Some(family.to_owned()),
        issuer_code: None,
        min_year: None,
        max_year: None,
        weight_oz: Some(ounces(grams)),
        finish,
    }
}

#[test]
fn proposals_group_exact_normalized_family_weight_and_finish_variants() {
    let items = [item(1, 10, 2), item(2, 11, 3), item(3, 12, 1)];
    let metadata = TypeMetaIndex::from([
        (10, metadata(10, " Lunar   ounce ", 31.1, None)),
        (11, metadata(11, "Lunar ounce", 31.21, None)),
        (12, metadata(12, "Lunar ounce", 31.1, Some(Finish::Bullion))),
    ]);

    let proposals = build_collection_proposals(&[], &items, &metadata);

    assert_eq!(proposals.len(), 2);
    let unconfirmed = proposals
        .iter()
        .find(|proposal| proposal.finish.is_none())
        .unwrap();
    assert_eq!(unconfirmed.family, "Lunar ounce");
    assert_eq!(unconfirmed.weight_millioz, 1_000);
    assert_eq!(unconfirmed.distinct_types, 2);
    assert_eq!(unconfirmed.quantity, 5);
    let bullion = proposals
        .iter()
        .find(|proposal| proposal.finish == Some(Finish::Bullion))
        .unwrap();
    assert_eq!(bullion.distinct_types, 1);
    assert_eq!(bullion.quantity, 1);
}

#[test]
fn proposals_are_isolated_to_the_supplied_users_current_items_without_fuzzy_families() {
    let metadata = TypeMetaIndex::from([
        (
            10,
            metadata(10, "Lunar Series III", 7.777, Some(Finish::Coloured)),
        ),
        (20, metadata(20, "Lunar ounce", 31.1, Some(Finish::Bullion))),
    ]);

    let first_user = build_collection_proposals(&[], &[item(1, 10, 1)], &metadata);
    let second_user = build_collection_proposals(&[], &[item(2, 20, 2)], &metadata);

    assert_eq!(first_user.len(), 1);
    assert_eq!(first_user[0].family, "Lunar Series III");
    assert_eq!(first_user[0].weight_millioz, 250);
    assert_eq!(first_user[0].finish, Some(Finish::Coloured));
    assert_eq!(second_user.len(), 1);
    assert_eq!(second_user[0].family, "Lunar ounce");
    assert!(
        !second_user
            .iter()
            .any(|proposal| { proposal.family == "Lunar Series III" })
    );
}

#[test]
fn only_exact_curated_variants_are_filtered_and_unknown_finish_stays_separate() {
    let curated = Series {
        id: SeriesId::new("lunar-iii"),
        name: "Lunar Series III".to_owned(),
        mint: "Perth Mint".to_owned(),
        issuer_code: "australia".to_owned(),
        metal: Metal::Silver,
        notes: None,
        incomplete: false,
        sources: std::collections::BTreeMap::new(),
        slots: vec![Slot {
            id: SlotId::new("lunar-iii-bullion"),
            label: "Bullion".to_owned(),
            year: 2026,
            motif: "Caballo".to_owned(),
            weight_oz: 1.0,
            finish: Finish::Bullion,
            release_status: ReleaseStatus::Issued,
            numista_type_ids: Vec::new(),
            matchers: Vec::new(),
        }],
    };
    let metadata = TypeMetaIndex::from([
        (
            10,
            metadata(10, "Lunar Series III", 31.1, Some(Finish::Bullion)),
        ),
        (
            11,
            metadata(11, "Lunar Series III", 7.777, Some(Finish::Coloured)),
        ),
        (12, metadata(12, "Lunar Series III", 31.1, None)),
    ]);

    let proposals = build_collection_proposals(
        &[curated],
        &[item(1, 10, 1), item(2, 11, 1), item(3, 12, 1)],
        &metadata,
    );

    assert_eq!(proposals.len(), 2);
    assert!(proposals.iter().any(|proposal| {
        proposal.weight_millioz == 250 && proposal.finish == Some(Finish::Coloured)
    }));
    assert!(
        proposals
            .iter()
            .any(|proposal| { proposal.weight_millioz == 1_000 && proposal.finish.is_none() })
    );
    assert!(!proposals.iter().any(|proposal| {
        proposal.weight_millioz == 1_000 && proposal.finish == Some(Finish::Bullion)
    }));
}
