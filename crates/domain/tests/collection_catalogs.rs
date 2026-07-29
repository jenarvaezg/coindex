use domain::{
    CollectedItem, CollectionCatalog, CollectionCatalogId, CollectionCatalogMember,
    CollectionCatalogMemberStatus, build_collection_catalog_album,
};

fn catalog() -> CollectionCatalog {
    CollectionCatalog {
        schema_version: 1,
        id: CollectionCatalogId::new("nikola-tesla-serbia-1oz"),
        name: "Nikola Tesla · Serbia · 1 oz".to_owned(),
        issuer_code: "serbie".to_owned(),
        family: "Nikola Tesla".to_owned(),
        weight_millioz: 1_000,
        finish: None,
        source: "https://en.numista.com/catalogue/series.php?id=5303".to_owned(),
        updated_at: "2026-07-29".to_owned(),
        members: vec![
            CollectionCatalogMember {
                id: "alternating-current".to_owned(),
                label: "Alternating current".to_owned(),
                year: 2018,
                numista_type_id: 150_352,
            },
            CollectionCatalogMember {
                id: "x-rays".to_owned(),
                label: "X-Rays".to_owned(),
                year: 2020,
                numista_type_id: 195_591,
            },
        ],
    }
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

#[test]
fn catalog_validation_requires_versioned_slugged_unique_sourced_exact_variants() {
    let definition = catalog();
    assert_eq!(
        definition.key(),
        domain::CollectionProposalKey::from_canonical_parts("Nikola Tesla", 1_000, "unknown")
            .unwrap()
    );
    definition.validate().unwrap();

    let mut invalid = definition.clone();
    invalid.members[1].id = invalid.members[0].id.clone();
    assert!(
        invalid
            .validate()
            .unwrap_err()
            .to_string()
            .contains("duplicated")
    );

    let mut invalid = definition.clone();
    invalid.members[1].numista_type_id = invalid.members[0].numista_type_id;
    assert!(
        invalid
            .validate()
            .unwrap_err()
            .to_string()
            .contains("150352")
    );

    let mut invalid = definition.clone();
    invalid.source = "https://example.com/catalog".to_owned();
    assert!(
        invalid
            .validate()
            .unwrap_err()
            .to_string()
            .contains("Numista series")
    );

    let mut invalid = definition.clone();
    invalid.family = " Nikola Tesla".to_owned();
    assert!(
        invalid
            .validate()
            .unwrap_err()
            .to_string()
            .contains("variant key")
    );
}

#[test]
fn catalog_album_uses_exact_type_ids_sums_quantity_and_isolates_supplied_holdings() {
    let definition = catalog();
    assert!(definition.is_evidenced_by(&[item(9, 195_591, 1)]));
    assert!(!definition.is_evidenced_by(&[item(10, 999_999, 1)]));
    let jose = build_collection_catalog_album(
        &definition,
        &[
            item(1, 195_591, 1),
            item(2, 195_591, 2),
            item(3, 999_999, 7),
        ],
    );
    let padre = build_collection_catalog_album(&definition, &[]);

    assert_eq!(jose.members.len(), 2);
    assert!(matches!(
        jose.members[0].status,
        CollectionCatalogMemberStatus::Missing
    ));
    let CollectionCatalogMemberStatus::Owned {
        quantity,
        ref items,
    } = jose.members[1].status
    else {
        panic!("X-Rays should be owned");
    };
    assert_eq!(quantity, 3);
    assert_eq!(items.len(), 2);
    assert_eq!(jose.owned_members(), 1);
    assert_eq!(padre.owned_members(), 0);
    assert!(
        padre
            .members
            .iter()
            .all(|member| { matches!(member.status, CollectionCatalogMemberStatus::Missing) })
    );
}
