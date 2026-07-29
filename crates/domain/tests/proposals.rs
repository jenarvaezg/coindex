use domain::{
    CollectedItem, CollectionProposal, CollectionProposalKey, CollectionProposalPreference, Finish,
    ProposalDisposition, TypeMeta, TypeMetaIndex, build_collection_proposals,
    classify_collection_proposals, collection_proposal_family_label, normalize_weight_millioz,
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

    let proposals = build_collection_proposals(&items, &metadata);

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

    let first_user = build_collection_proposals(&[item(1, 10, 1)], &metadata);
    let second_user = build_collection_proposals(&[item(2, 20, 2)], &metadata);

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
fn curated_variants_also_surface_as_proposals_and_unknown_finish_stays_separate() {
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

    let proposals =
        build_collection_proposals(&[item(1, 10, 1), item(2, 11, 1), item(3, 12, 1)], &metadata);

    assert_eq!(proposals.len(), 3);
    assert!(proposals.iter().any(|proposal| {
        proposal.weight_millioz == 250 && proposal.finish == Some(Finish::Coloured)
    }));
    assert!(
        proposals
            .iter()
            .any(|proposal| { proposal.weight_millioz == 1_000 && proposal.finish.is_none() })
    );
    assert!(proposals.iter().any(|proposal| {
        proposal.weight_millioz == 1_000 && proposal.finish == Some(Finish::Bullion)
    }));
}

#[test]
fn editorial_aliases_preserve_raw_keys_and_technical_system_years_are_ineligible() {
    let aliases = [
        ("SML", "Silver Maple Leaf"),
        ("Red Data Book", "Libro Rojo de Rusia"),
        (
            "Serie de monedas de plata obtenidas a valor facial",
            "Monedas españolas de plata a valor facial",
        ),
        ("Lunar ounce", "Rwanda Lunar Ounce"),
        ("Nautical Ounce", "Rwanda Nautical Ounce"),
    ];
    for (raw, display) in aliases {
        assert_eq!(collection_proposal_family_label(raw), display);
    }
    assert_eq!(collection_proposal_family_label("sml"), "sml");
    assert_eq!(
        collection_proposal_family_label("System of a Down"),
        "System of a Down"
    );

    let metadata = TypeMetaIndex::from([
        (10, metadata(10, "System 2025", 31.1, None)),
        (11, metadata(11, "System 1927-1968", 31.1, None)),
        (12, metadata(12, "System 1969-1980-2001", 31.1, None)),
        (20, metadata(20, "System of a Down", 31.1, None)),
        (21, metadata(21, "System 19-2001", 31.1, None)),
    ]);
    let proposals = build_collection_proposals(
        &[
            item(1, 10, 1),
            item(2, 11, 1),
            item(3, 12, 1),
            item(4, 20, 1),
            item(5, 21, 1),
        ],
        &metadata,
    );

    assert_eq!(
        proposals
            .iter()
            .map(|proposal| proposal.family.as_str())
            .collect::<Vec<_>>(),
        ["System 19-2001", "System of a Down"]
    );
}

#[test]
fn canonical_keys_round_trip_finish_codes_and_classification_keeps_stale_preferences_dormant() {
    let proposal = |family: &str, finish| CollectionProposal {
        family: family.to_owned(),
        weight_millioz: 1_000,
        finish,
        distinct_types: 1,
        quantity: 1,
    };
    let followed = proposal("SML", Some(Finish::ProofColoured));
    let available = proposal("Lunar ounce", None);
    let ignored = proposal("Nautical Ounce", Some(Finish::Bullion));
    let stale =
        CollectionProposalKey::from_canonical_parts("Red Data Book", 2_000, "proof").unwrap();
    let preferences = vec![
        CollectionProposalPreference {
            key: followed.key(),
            disposition: ProposalDisposition::Followed,
        },
        CollectionProposalPreference {
            key: ignored.key(),
            disposition: ProposalDisposition::Ignored,
        },
        CollectionProposalPreference {
            key: stale,
            disposition: ProposalDisposition::Followed,
        },
    ];

    assert_eq!(followed.key().finish_code(), "proof_coloured");
    assert_eq!(
        CollectionProposalKey::from_canonical_parts("Lunar ounce", 1_000, "unknown")
            .unwrap()
            .finish,
        None
    );
    assert!(
        CollectionProposalKey::from_canonical_parts(" Lunar ounce", 1_000, "unknown").is_none()
    );
    assert!(CollectionProposalKey::from_canonical_parts("Lunar ounce", 0, "unknown").is_none());
    assert!(CollectionProposalKey::from_canonical_parts("Lunar ounce", 1_000, "Proof").is_none());

    let classified =
        classify_collection_proposals(vec![followed, available, ignored], &preferences);

    assert_eq!(classified.followed.len(), 1);
    assert_eq!(classified.available.len(), 1);
    assert_eq!(classified.ignored.len(), 1);
    assert_eq!(classified.followed[0].family, "SML");
    assert_eq!(classified.available[0].family, "Lunar ounce");
    assert_eq!(classified.ignored[0].family, "Nautical Ounce");
}
