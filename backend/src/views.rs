use domain::{
    Album, AlbumSlot, ClassifiedCollectionProposals, CollectionCatalog, CollectionCatalogAlbum,
    CollectionCatalogId, CollectionCatalogMemberStatus, CollectionProposal, Finish, MatchSource,
    ReleaseStatus, Series, SeriesAlbum, SlotStatus, TypeMetaIndex, UnmatchedReason,
    collection_proposal_family_label,
};
use maud::{DOCTYPE, Markup, html};

use crate::config::AppConfig;

pub fn layout(title: &str, body: Markup) -> Markup {
    html! {
        (DOCTYPE)
        html lang="es" {
            head {
                meta charset="utf-8";
                meta name="viewport" content="width=device-width, initial-scale=1";
                title { (title) " · Coindex" }
                link rel="stylesheet" href="/static/site.css";
            }
            body {
                header class="masthead" {
                    a href="/" class="brand" { "Coindex" }
                    p { "Inventario de campo · plata bullion" }
                }
                main { (body) }
                footer { "Colección privada · datos de catálogo cacheados con cuidado" }
            }
        }
    }
}

pub fn index(
    config: &AppConfig,
    collections: &[(
        String,
        ClassifiedCollectionProposals,
        Vec<CollectionCatalogId>,
    )],
    collection_catalogs: &[CollectionCatalog],
) -> Markup {
    layout(
        "Álbumes",
        html! {
            section class="intro" {
                p class="eyebrow" { "Cuaderno de colección" }
                h1 { "Láminas de plata" }
                p { "Dos colecciones, ordenadas por serie y emisión." }
            }
            @for user in config.users() {
                @let collection = collections.iter().find(|(key, _, _)| key == &user.key);
                section class="user-section" {
                    div class="section-heading" {
                        h2 { (user.key) }
                        div class="sync-actions" {
                            form method="post" action=(format!("/u/{}/sync", user.key)) {
                                button type="submit" { "Sincronizar" }
                            }
                            a href=(format!("/u/{}/unmatched", user.key)) { "Sin clasificar" }
                        }
                    }
                    @if let Some((_, proposals, eligible_catalog_ids)) = collection {
                        @if !proposals.followed.is_empty()
                            || !proposals.available.is_empty()
                            || !proposals.ignored.is_empty()
                        {
                            section class="proposal-section" id=(format!("proposals-{}", user.key)) {
                                div class="proposal-heading" {
                                    p class="eyebrow" { "Propuestas desde las piezas actuales" }
                                    p {
                                        "Se basan solo en tus piezas actuales. Seguir una propuesta no inventa huecos; "
                                        "si existe un catálogo curado, podrás abrir su lámina de referencia."
                                    }
                                }
                                @if !proposals.followed.is_empty() {
                                    h3 class="proposal-section-title" { "Seguidas" }
                                    div class="proposal-grid" {
                                        @for proposal in &proposals.followed {
                                            (proposal_card(
                                                &user.key,
                                                proposal,
                                                ProposalCardState::Followed,
                                                collection_catalogs,
                                                eligible_catalog_ids,
                                            ))
                                        }
                                    }
                                }
                                @if !proposals.available.is_empty() {
                                    h3 class="proposal-section-title" { "Disponibles" }
                                    div class="proposal-grid" {
                                        @for proposal in &proposals.available {
                                            (proposal_card(
                                                &user.key,
                                                proposal,
                                                ProposalCardState::Available,
                                                collection_catalogs,
                                                eligible_catalog_ids,
                                            ))
                                        }
                                    }
                                }
                                @if !proposals.ignored.is_empty() {
                                    details class="ignored-proposals" {
                                        summary {
                                            "Propuestas ignoradas · "
                                            (proposals.ignored.len())
                                        }
                                        div class="proposal-grid" {
                                            @for proposal in &proposals.ignored {
                                                (proposal_card(
                                                    &user.key,
                                                    proposal,
                                                    ProposalCardState::Ignored,
                                                    collection_catalogs,
                                                    eligible_catalog_ids,
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

#[derive(Clone, Copy)]
enum ProposalCardState {
    Followed,
    Available,
    Ignored,
}

fn proposal_card(
    user_key: &str,
    proposal: &CollectionProposal,
    state: ProposalCardState,
    collection_catalogs: &[CollectionCatalog],
    eligible_catalog_ids: &[CollectionCatalogId],
) -> Markup {
    let key = proposal.key();
    let matching_catalog = collection_catalogs
        .iter()
        .find(|catalog| catalog.key() == key);
    let followed_catalog = matches!(state, ProposalCardState::Followed)
        .then(|| matching_catalog.filter(|catalog| eligible_catalog_ids.contains(&catalog.id)))
        .flatten();
    html! {
        article class="proposal-card" {
            p class="plate-number" { "Evidencia de colección" }
            h3 {
                @if let Some(catalog) = followed_catalog {
                    a href=(format!(
                        "/u/{user_key}/followed-collections/{}",
                        catalog.id
                    )) {
                        (collection_proposal_family_label(&proposal.family))
                    }
                } @else if let Some(catalog) = matching_catalog {
                    a href=(&catalog.source) rel="noreferrer" {
                        (collection_proposal_family_label(&proposal.family))
                    }
                } @else {
                    (collection_proposal_family_label(&proposal.family))
                }
            }
            p class="proposal-variant" {
                (proposal_weight_label(proposal.weight_millioz))
                " · "
                (proposal_finish_label(proposal.finish.as_ref()))
            }
            p class="proposal-count" {
                (proposal.distinct_types)
                @if proposal.distinct_types == 1 {
                    " tipo distinto"
                } @else {
                    " tipos distintos"
                }
                " · "
                (proposal.quantity)
                @if proposal.quantity == 1 {
                    " pieza"
                } @else {
                    " piezas"
                }
            }
            div class="proposal-actions" {
                @match state {
                    ProposalCardState::Followed => {
                        (proposal_preference_form(user_key, &key, "restore", "Dejar de seguir"))
                        (proposal_preference_form(user_key, &key, "ignore", "Ignorar"))
                    }
                    ProposalCardState::Available => {
                        (proposal_preference_form(user_key, &key, "follow", "Seguir"))
                        (proposal_preference_form(user_key, &key, "ignore", "Ignorar"))
                    }
                    ProposalCardState::Ignored => {
                        (proposal_preference_form(user_key, &key, "restore", "Restaurar"))
                    }
                }
            }
        }
    }
}

pub fn followed_collection(
    user_key: &str,
    catalog: &CollectionCatalog,
    album: &CollectionCatalogAlbum,
    type_meta: &TypeMetaIndex,
) -> Markup {
    layout(
        &catalog.name,
        html! {
            nav class="breadcrumb" {
                a href=(format!("/#proposals-{user_key}")) { "← Colecciones seguidas" }
            }
            header class="series-header" {
                div {
                    p class="eyebrow" { "Catálogo curado" }
                    h1 { (&catalog.name) }
                    p {
                        "Referencia curada de emisiones catalogadas para esta variante; "
                        "no afirma que sea una serie cerrada."
                    }
                    p class="catalog-source" {
                        "Actualizado el " (&catalog.updated_at) " · "
                        a href=(&catalog.source) rel="noreferrer" { "Fuente Numista" }
                    }
                }
                dl class="field-card" {
                    dt { "Progreso" }
                    dd { (album.owned_members()) " / " (album.members.len()) " emisiones" }
                    dt { "Peso" }
                    dd { (proposal_weight_label(catalog.weight_millioz)) }
                    dt { "Acabado" }
                    dd { (proposal_finish_label(catalog.finish.as_ref())) }
                }
            }
            section class="plate-grid" aria-label="Emisiones del catálogo curado" {
                @for album_member in &album.members {
                    @let (class, state) = match &album_member.status {
                        CollectionCatalogMemberStatus::Owned { quantity, .. } if *quantity > 1 => {
                            ("slot owned", format!("Tengo · ×{quantity}"))
                        }
                        CollectionCatalogMemberStatus::Owned { .. } => {
                            ("slot owned", "Tengo".to_owned())
                        }
                        CollectionCatalogMemberStatus::Missing => {
                            ("slot missing", "Me falta".to_owned())
                        }
                    };
                    article class=(class) {
                        div class="coin-image" {
                            @if type_meta.contains_key(&album_member.member.numista_type_id) {
                                (coin_sides(
                                    album_member.member.numista_type_id,
                                    &album_member.member.label,
                                ))
                            } @else {
                                span class="silhouette" aria-hidden="true" {}
                            }
                        }
                        div class="slot-copy" {
                            p class="slot-state" { (state) }
                            h2 { (&album_member.member.label) }
                            p {
                                (album_member.member.year)
                                " · Numista "
                                (album_member.member.numista_type_id)
                            }
                        }
                    }
                }
            }
        },
    )
}

fn proposal_preference_form(
    user_key: &str,
    key: &domain::CollectionProposalKey,
    action: &str,
    label: &str,
) -> Markup {
    html! {
        form
            method="post"
            action=(format!("/u/{user_key}/collection-proposal-preference"))
        {
            input type="hidden" name="family" value=(&key.family);
            input type="hidden" name="weight_millioz" value=(key.weight_millioz);
            input type="hidden" name="finish" value=(key.finish_code());
            input type="hidden" name="action" value=(action);
            button type="submit" class="quiet" { (label) }
        }
    }
}

fn proposal_weight_label(weight_millioz: u32) -> String {
    let whole = weight_millioz / 1_000;
    let fraction = format!("{:03}", weight_millioz % 1_000);
    let fraction = fraction.trim_end_matches('0');
    if fraction.is_empty() {
        format!("{whole} oz")
    } else {
        format!("{whole},{fraction} oz")
    }
}

fn proposal_finish_label(finish: Option<&Finish>) -> &'static str {
    match finish {
        None => "Por confirmar",
        Some(Finish::Bullion) => "Bullion",
        Some(Finish::Proof) => "Proof",
        Some(Finish::Coloured) => "Coloreado",
        Some(Finish::ProofColoured) => "Proof coloreado",
        Some(Finish::Gilded) => "Dorado",
        Some(Finish::Antiqued) => "Envejecido",
    }
}

pub fn series(
    user_key: &str,
    definition: &Series,
    album: &SeriesAlbum,
    all_series: &[Series],
) -> Markup {
    let (owned, issued, future) = progress(album);
    layout(
        &album.name,
        html! {
            nav class="breadcrumb" { a href="/" { "← Todas las láminas" } }
            header class="series-header" {
                div {
                    p class="eyebrow" { (definition.mint) " · " (definition.metal_label()) }
                    h1 { (album.name) }
                    @if let Some(notes) = &definition.notes {
                        p { (notes) }
                    }
                }
                dl class="field-card" {
                    dt { "Progreso" }
                    dd { (owned) " / " (issued) " emitidas" }
                    dt { "Próximas" }
                    dd { (future) " por emitir" }
                    dt { "Metal" }
                    dd { (definition.metal_label()) }
                    dt { "Acabado" }
                    dd { (series_finish_label(definition)) }
                    dt { "Peso" }
                    dd { (series_weight_label(definition)) }
                }
            }
            section class="plate-grid" aria-label="Casillas de la serie" {
                @for album_slot in &album.slots {
                    (slot_card(user_key, album_slot, all_series))
                }
            }
        },
    )
}

pub fn unmatched(
    user_key: &str,
    album: &Album,
    all_series: &[Series],
    type_meta: &TypeMetaIndex,
) -> Markup {
    layout(
        "Sin clasificar",
        html! {
            nav class="breadcrumb" { a href="/" { "← Todas las láminas" } }
            header class="series-header" {
                div {
                    p class="eyebrow" { (user_key) }
                    h1 { "Piezas sin clasificar" }
                    p { "Nada se descarta: asigna cada pieza a una casilla o márcala como ajena a estas series." }
                }
            }
            section class="unmatched-list" {
                @if album.unmatched.is_empty() {
                    p class="empty-state" { "No hay piezas pendientes de clasificación." }
                }
                @for item in &album.unmatched {
                    @let title = type_meta
                        .get(&item.type_id)
                        .and_then(|metadata| {
                            metadata
                                .display_title
                                .as_deref()
                                .or(metadata.title.as_deref())
                        })
                        .map(str::to_owned)
                        .unwrap_or_else(|| format!("Pieza {}", item.item_id));
                    article class="unmatched-item" {
                        (coin_sides(item.type_id, &title))
                        div {
                            h2 { (&title) }
                            p {
                                "ID de pieza " (item.item_id)
                                " · Tipo Numista " (item.type_id)
                                " · cantidad " (item.quantity)
                            }
                            @let (reason_class, reason) = unmatched_reason(item.unmatched_reason.as_ref());
                            p class=(format!("unmatched-reason {reason_class}")) { (reason) }
                            form method="post" action=(format!("/u/{}/override", user_key)) {
                                input type="hidden" name="item_id" value=(item.item_id);
                                label {
                                    "Casilla"
                                    select name="slot_id" {
                                        option value="" { "No pertenece a una casilla" }
                                        (slot_options(all_series, None))
                                    }
                                }
                                button type="submit" { "Guardar asignación" }
                            }
                        }
                    }
                }
            }
        },
    )
}

fn unmatched_reason(reason: Option<&UnmatchedReason>) -> (&'static str, String) {
    match reason {
        Some(UnmatchedReason::ExcludedByManualOverride) => (
            "manually-excluded",
            "Excluida manualmente: esta pieza está marcada para no ocupar ninguna casilla."
                .to_owned(),
        ),
        Some(UnmatchedReason::NoMatchingSlot) | None => (
            "unresolved",
            "Sin coincidencia: todavía no se ha encontrado una casilla compatible.".to_owned(),
        ),
        Some(UnmatchedReason::InvalidManualOverride {
            slot_id,
            matching_slots,
        }) => (
            "dangling-override",
            format!(
                "Corrección manual inválida hacia `{slot_id}` ({matching_slots} casillas encontradas)."
            ),
        ),
        Some(UnmatchedReason::AmbiguousExplicitTypeId { slot_ids }) => (
            "ambiguous",
            format!(
                "Ambigua por ID explícito: coincide con {} casillas.",
                slot_ids.len()
            ),
        ),
        Some(UnmatchedReason::AmbiguousHeuristic {
            confidence,
            slot_ids,
        }) => {
            let percentage = confidence * 100.0;
            (
                "ambiguous",
                format!(
                    "Coincidencia heurística ambigua ({percentage:.0}%): {} casillas posibles.",
                    slot_ids.len()
                ),
            )
        }
    }
}

fn slot_card(user_key: &str, album_slot: &AlbumSlot, all_series: &[Series]) -> Markup {
    let (class, state, type_id, owned_items) = match &album_slot.status {
        SlotStatus::Owned { items, quantity } => (
            "slot owned",
            if *quantity > 1 {
                format!("Tengo · ×{quantity}")
            } else {
                "Tengo".to_owned()
            },
            items.first().map(|item| item.type_id),
            Some(items.as_slice()),
        ),
        SlotStatus::Missing => (
            "slot missing",
            "Me falta".to_owned(),
            album_slot.slot.numista_type_ids.first().copied(),
            None,
        ),
        SlotStatus::NotYetIssued => ("slot future", "Sin emitir".to_owned(), None, None),
    };
    let has_heuristic = owned_items.is_some_and(|items| {
        items
            .iter()
            .any(|item| matches!(item.match_source, Some(MatchSource::Heuristic { .. })))
    });
    html! {
        article class=(class) {
            div class="coin-image" {
                @if let Some(type_id) = type_id {
                    (coin_sides(type_id, &album_slot.slot.label))
                } @else {
                    span class="silhouette" aria-hidden="true" {}
                }
            }
            div class="slot-copy" {
                p class="slot-state" { (state) }
                h2 { (album_slot.slot.motif) }
                p { (album_slot.slot.year) " · " (album_slot.slot.weight_oz) " oz" }
            }
            @if let Some(items) = owned_items {
                details
                    class=(if has_heuristic { "match-review heuristic-review" } else { "match-review" })
                    id=(format!("review-{}", album_slot.slot.id))
                {
                    @if has_heuristic {
                        summary
                            class="heuristic"
                            title="Emparejamiento heurístico: revisar o corregir"
                            aria-label="Revisar o corregir emparejamiento heurístico"
                        { "?" }
                    } @else {
                        summary { "Revisar piezas" }
                    }
                    @for item in items {
                        form method="post" action=(format!("/u/{}/override", user_key)) {
                            input type="hidden" name="item_id" value=(item.item_id);
                            label {
                                "Pieza " (item.item_id)
                                @if matches!(item.match_source, Some(MatchSource::Heuristic { .. })) {
                                    " · heurístico"
                                }
                                select name="slot_id" {
                                    option value="" { "No pertenece a una casilla" }
                                    (slot_options(all_series, Some(&album_slot.slot.id.to_string())))
                                }
                            }
                            button type="submit" { "Guardar corrección" }
                        }
                    }
                }
            }
        }
    }
}

fn coin_sides(type_id: u32, label: &str) -> Markup {
    html! {
        div class="coin-sides" role="group" aria-label=(format!("Anverso y reverso de {label}")) {
            figure class="coin-side" {
                img
                    src=(format!("/img/type/{type_id}/obverse"))
                    alt=(format!("Anverso de {label}"))
                    loading="lazy"
                    decoding="async";
                figcaption { "Anverso" }
            }
            figure class="coin-side" {
                img
                    src=(format!("/img/type/{type_id}/reverse"))
                    alt=(format!("Reverso de {label}"))
                    loading="lazy"
                    decoding="async";
                figcaption { "Reverso" }
            }
        }
    }
}

fn series_finish_label(series: &Series) -> &'static str {
    let Some(first) = series.slots.first().map(|slot| &slot.finish) else {
        return "Sin datos";
    };
    if series.slots.iter().any(|slot| slot.finish != *first) {
        return "Varios";
    }
    match first {
        domain::Finish::Bullion => "Bullion",
        domain::Finish::Proof => "Proof",
        domain::Finish::Coloured => "Coloreado",
        domain::Finish::ProofColoured => "Proof coloreado",
        domain::Finish::Gilded => "Dorado",
        domain::Finish::Antiqued => "Envejecido",
    }
}

fn series_weight_label(series: &Series) -> String {
    let Some(first) = series.slots.first().map(|slot| slot.weight_oz) else {
        return "Sin datos".to_owned();
    };
    if series
        .slots
        .iter()
        .any(|slot| (slot.weight_oz - first).abs() > 0.001)
    {
        "Varios".to_owned()
    } else {
        format!("{first} oz")
    }
}

fn slot_options(all_series: &[Series], selected: Option<&str>) -> Markup {
    html! {
        @for series in all_series {
            optgroup label=(series.name) {
                @for slot in &series.slots {
                    option
                        value=(slot.id)
                        selected[selected.is_some_and(|value| value == slot.id.to_string())]
                    {
                        (slot.label)
                    }
                }
            }
        }
    }
}

fn progress(series: &SeriesAlbum) -> (usize, usize, usize) {
    let mut owned = 0;
    let mut issued = 0;
    let mut future = 0;
    for slot in &series.slots {
        if slot.slot.release_status == ReleaseStatus::Issued {
            issued += 1;
            if matches!(slot.status, SlotStatus::Owned { .. }) {
                owned += 1;
            }
        } else {
            future += 1;
        }
    }
    (owned, issued, future)
}

trait MetalLabel {
    fn metal_label(&self) -> &'static str;
}

impl MetalLabel for Series {
    fn metal_label(&self) -> &'static str {
        match self.metal {
            domain::Metal::Silver => "Plata",
            domain::Metal::Gold => "Oro",
            domain::Metal::Platinum => "Platino",
            domain::Metal::Other => "Otro metal",
        }
    }
}

#[cfg(test)]
mod tests {
    use domain::{
        Album, AlbumSlot, ClassifiedCollectionProposals, CollectedItem, CollectionProposal, Finish,
        ItemRef, MatchSource, ReleaseStatus, SeriesAlbum, SeriesId, Slot, SlotId, SlotStatus,
        TypeMeta, TypeMetaIndex, UnmatchedReason, build_collection_catalog_album,
    };

    use super::{followed_collection, index, progress, series, unmatched};
    use crate::config::AppConfig;
    use crate::seeds::load_collection_catalogs;

    fn slot(id: &str, status: SlotStatus) -> AlbumSlot {
        AlbumSlot {
            slot: Slot {
                id: SlotId::new(id),
                label: id.to_owned(),
                year: 2027,
                motif: id.to_owned(),
                weight_oz: 1.0,
                finish: Finish::Bullion,
                release_status: ReleaseStatus::Expected,
                numista_type_ids: Vec::new(),
                matchers: Vec::new(),
            },
            status,
        }
    }

    #[test]
    fn index_offers_one_real_sync_action_per_user_without_cost_estimates() {
        let config = AppConfig::parse(
            "jose:1:first-key,padre:2:second-key",
            None,
            "http://127.0.0.1:8000",
        )
        .unwrap();

        let html = index(&config, &[], &[]).into_string();

        assert_eq!(html.matches("action=\"/u/jose/sync\"").count(), 1);
        assert_eq!(html.matches("action=\"/u/padre/sync\"").count(), 1);
        assert_eq!(html.matches(">Sincronizar</button>").count(), 2);
        assert!(!html.contains("Calcular gasto"));
        assert!(!html.contains("dry_run"));
    }

    #[test]
    fn index_shows_read_only_current_proposals_per_user_without_curated_plates() {
        let config = AppConfig::parse(
            "jose:1:first-key,padre:2:second-key",
            None,
            "http://127.0.0.1:8000",
        )
        .unwrap();
        let jose_proposals = ClassifiedCollectionProposals {
            followed: vec![
                CollectionProposal {
                    family: "SML".to_owned(),
                    weight_millioz: 1_000,
                    finish: Some(Finish::ProofColoured),
                    distinct_types: 1,
                    quantity: 1,
                },
                CollectionProposal {
                    family: "Nikola Tesla".to_owned(),
                    weight_millioz: 1_000,
                    finish: None,
                    distinct_types: 1,
                    quantity: 1,
                },
            ],
            available: vec![CollectionProposal {
                family: "Lunar Series III".to_owned(),
                weight_millioz: 250,
                finish: Some(Finish::Coloured),
                distinct_types: 1,
                quantity: 2,
            }],
            ignored: vec![CollectionProposal {
                family: "Nautical Ounce".to_owned(),
                weight_millioz: 1_000,
                finish: Some(Finish::Bullion),
                distinct_types: 2,
                quantity: 3,
            }],
        };
        let padre_proposals = ClassifiedCollectionProposals {
            followed: vec![CollectionProposal {
                family: "Nikola Tesla".to_owned(),
                weight_millioz: 1_000,
                finish: None,
                distinct_types: 1,
                quantity: 1,
            }],
            available: vec![CollectionProposal {
                family: "Lunar ounce".to_owned(),
                weight_millioz: 1_000,
                finish: None,
                distinct_types: 2,
                quantity: 3,
            }],
            ..ClassifiedCollectionProposals::default()
        };
        let collection_catalogs = load_collection_catalogs().unwrap();

        let html = index(
            &config,
            &[
                (
                    "jose".to_owned(),
                    jose_proposals,
                    vec![collection_catalogs[0].id.clone()],
                ),
                ("padre".to_owned(), padre_proposals, Vec::new()),
            ],
            &collection_catalogs,
        )
        .into_string();

        assert!(!html.contains("Lámina ·"));
        assert!(!html.contains("series-card"));
        assert!(html.contains("id=\"proposals-jose\""));
        assert!(html.contains("Silver Maple Leaf"));
        assert!(html.contains("1 oz · Proof coloreado"));
        assert!(html.contains("name=\"family\" value=\"SML\""));
        assert!(html.contains("name=\"finish\" value=\"proof_coloured\""));
        assert!(html.contains(">Dejar de seguir</button>"));
        assert!(html.contains("Lunar Series III"));
        assert!(html.contains("0,25 oz · Coloreado"));
        assert!(html.contains("1 tipo distinto · 2 piezas"));
        assert!(html.contains(">Seguir</button>"));
        assert!(html.contains(">Ignorar</button>"));
        assert!(html.contains("<summary>Propuestas ignoradas · 1</summary>"));
        assert!(html.contains("Rwanda Nautical Ounce"));
        assert!(html.contains(">Restaurar</button>"));
        assert!(html.contains("Rwanda Lunar Ounce"));
        assert!(html.contains("2 tipos distintos · 3 piezas"));
        assert!(html.contains("href=\"/u/jose/followed-collections/nikola-tesla-serbia-1oz\""));
        assert!(!html.contains("href=\"/u/padre/followed-collections/nikola-tesla-serbia-1oz\""));

        let available_html = index(
            &config,
            &[(
                "padre".to_owned(),
                ClassifiedCollectionProposals {
                    available: vec![CollectionProposal {
                        family: "Nikola Tesla".to_owned(),
                        weight_millioz: 1_000,
                        finish: None,
                        distinct_types: 1,
                        quantity: 1,
                    }],
                    ..ClassifiedCollectionProposals::default()
                },
                vec![collection_catalogs[0].id.clone()],
            )],
            &collection_catalogs,
        )
        .into_string();
        assert!(
            !available_html
                .contains("href=\"/u/padre/followed-collections/nikola-tesla-serbia-1oz\"")
        );
        assert!(available_html.contains(
            "href=\"https://en.numista.com/catalogue/series.php?id=5303\" rel=\"noreferrer\""
        ));
    }

    #[test]
    fn followed_collection_renders_curated_tesla_progress_and_only_cached_images() {
        let catalog = load_collection_catalogs().unwrap().remove(0);
        let album = build_collection_catalog_album(
            &catalog,
            &[CollectedItem {
                id: 1,
                quantity: 1,
                type_id: 195_591,
                title: None,
                issuer_code: None,
                issue_year: None,
                gregorian_year: None,
                grade: None,
                price: None,
                for_swap: None,
                collection_name: None,
            }],
        );
        let type_meta = TypeMetaIndex::from([(
            195_591,
            TypeMeta {
                id: 195_591,
                title: Some("1 Dinar - X-Rays".to_owned()),
                display_title: Some("1 Dinar - X-Rays".to_owned()),
                family: Some("Nikola Tesla".to_owned()),
                issuer_code: Some("serbie".to_owned()),
                min_year: Some(2020),
                max_year: Some(2020),
                weight_oz: Some(1.0),
                finish: None,
            },
        )]);

        let html = followed_collection("jose", &catalog, &album, &type_meta).into_string();

        assert!(html.contains("1 / 12 emisiones"));
        assert!(html.contains("<dt>Peso</dt><dd>1 oz</dd>"));
        assert!(html.contains("<dt>Acabado</dt><dd>Por confirmar</dd>"));
        assert_eq!(html.matches("class=\"slot ").count(), 12);
        assert_eq!(html.matches(">Tengo<").count(), 1);
        assert_eq!(html.matches(">Me falta<").count(), 11);
        assert_eq!(html.matches("class=\"silhouette\"").count(), 11);
        assert!(html.contains("/img/type/195591/obverse"));
        assert!(html.contains("/img/type/195591/reverse"));
        assert!(html.contains("Catálogo curado"));
        assert!(html.contains("Actualizado el 2026-07-29"));
        assert!(html.contains("https://en.numista.com/catalogue/series.php?id=5303"));
        assert!(!html.contains("name=\"item_id\""));
        assert!(!html.contains("Guardar asignación"));
    }

    #[test]
    fn renders_missing_future_and_owned_as_distinct_states() {
        let definition = domain::Series {
            id: SeriesId::new("field"),
            name: "Field".to_owned(),
            mint: "Mint".to_owned(),
            issuer_code: "issuer".to_owned(),
            metal: domain::Metal::Silver,
            notes: None,
            incomplete: false,
            sources: std::collections::BTreeMap::new(),
            slots: Vec::new(),
        };
        let album = SeriesAlbum {
            series_id: SeriesId::new("field"),
            name: "Field".to_owned(),
            slots: vec![
                slot(
                    "owned",
                    SlotStatus::Owned {
                        quantity: 1,
                        items: vec![],
                    },
                ),
                slot("missing", SlotStatus::Missing),
                slot("future", SlotStatus::NotYetIssued),
            ],
        };

        let html = series("jose", &definition, &album, &[]).into_string();

        assert!(html.contains("class=\"slot owned\""));
        assert!(html.contains("class=\"slot missing\""));
        assert!(html.contains("class=\"slot future\""));
        assert!(html.contains("Tengo"));
        assert!(html.contains("Me falta"));
        assert!(html.contains("Sin emitir"));
        assert!(html.contains("<summary>Revisar piezas</summary>"));
    }

    #[test]
    fn series_cards_show_accessible_obverse_and_reverse_for_known_types() {
        let definition = domain::Series {
            id: SeriesId::new("field"),
            name: "Field".to_owned(),
            mint: "Mint".to_owned(),
            issuer_code: "issuer".to_owned(),
            metal: domain::Metal::Silver,
            notes: None,
            incomplete: false,
            sources: std::collections::BTreeMap::new(),
            slots: Vec::new(),
        };
        let mut owned = slot(
            "owned",
            SlotStatus::Owned {
                quantity: 1,
                items: vec![ItemRef {
                    item_id: 1,
                    type_id: 10,
                    quantity: 1,
                    match_source: Some(MatchSource::ExplicitTypeId),
                    unmatched_reason: None,
                }],
            },
        );
        owned.slot.label = "Lince ibérico".to_owned();
        let mut missing = slot("missing", SlotStatus::Missing);
        missing.slot.label = "Águila imperial".to_owned();
        missing.slot.numista_type_ids = vec![20];
        let album = SeriesAlbum {
            series_id: SeriesId::new("field"),
            name: "Field".to_owned(),
            slots: vec![owned, missing, slot("unknown", SlotStatus::Missing)],
        };

        let html = series("jose", &definition, &album, &[]).into_string();

        for expected in [
            "/img/type/10/obverse",
            "/img/type/10/reverse",
            "/img/type/20/obverse",
            "/img/type/20/reverse",
            "alt=\"Anverso de Lince ibérico\"",
            "alt=\"Reverso de Águila imperial\"",
        ] {
            assert!(html.contains(expected), "missing `{expected}` in {html}");
        }
        assert_eq!(html.matches("<figcaption>Anverso</figcaption>").count(), 2);
        assert_eq!(html.matches("<figcaption>Reverso</figcaption>").count(), 2);
        assert_eq!(
            html.matches("loading=\"lazy\" decoding=\"async\"").count(),
            4
        );
        assert_eq!(html.matches("class=\"silhouette\"").count(), 1);
    }

    #[test]
    fn renders_correction_and_negative_override_for_every_owned_item() {
        let mut definition = domain::Series {
            id: SeriesId::new("field"),
            name: "Field".to_owned(),
            mint: "Mint".to_owned(),
            issuer_code: "issuer".to_owned(),
            metal: domain::Metal::Silver,
            notes: None,
            incomplete: false,
            sources: std::collections::BTreeMap::new(),
            slots: vec![slot("owned", SlotStatus::Missing).slot],
        };
        definition.slots[0].release_status = ReleaseStatus::Issued;
        let album = SeriesAlbum {
            series_id: SeriesId::new("field"),
            name: "Field".to_owned(),
            slots: vec![AlbumSlot {
                slot: definition.slots[0].clone(),
                status: SlotStatus::Owned {
                    quantity: 2,
                    items: vec![
                        ItemRef {
                            item_id: 1,
                            type_id: 10,
                            quantity: 1,
                            match_source: Some(MatchSource::ExplicitTypeId),
                            unmatched_reason: None,
                        },
                        ItemRef {
                            item_id: 2,
                            type_id: 20,
                            quantity: 1,
                            match_source: Some(MatchSource::Heuristic {
                                confidence: 0.8,
                                explanation: "test".to_owned(),
                            }),
                            unmatched_reason: None,
                        },
                    ],
                },
            }],
        };

        let html = series("jose", &definition, &album, &[definition.clone()]).into_string();

        assert_eq!(html.matches("name=\"item_id\"").count(), 2);
        assert_eq!(html.matches("No pertenece a una casilla").count(), 2);
        assert!(html.contains("Emparejamiento heurístico"));
        assert!(html.contains("<summary class=\"heuristic\""));
        assert_eq!(html.matches("<summary").count(), 1);
        assert!(!html.contains("href=\"#review-owned\""));
        assert!(html.contains("id=\"review-owned\""));
        assert!(!html.contains("select name=\"slot_id\" required"));
    }

    #[test]
    fn renders_physical_series_specifications_in_the_field_card() {
        let mut definition = domain::Series {
            id: SeriesId::new("field"),
            name: "Field".to_owned(),
            mint: "Mint".to_owned(),
            issuer_code: "issuer".to_owned(),
            metal: domain::Metal::Silver,
            notes: None,
            incomplete: false,
            sources: std::collections::BTreeMap::new(),
            slots: vec![slot("issued", SlotStatus::Missing).slot],
        };
        definition.slots[0].release_status = ReleaseStatus::Issued;
        let album = SeriesAlbum {
            series_id: definition.id.clone(),
            name: definition.name.clone(),
            slots: vec![AlbumSlot {
                slot: definition.slots[0].clone(),
                status: SlotStatus::Missing,
            }],
        };

        let html = series("jose", &definition, &album, &[]).into_string();

        assert!(html.contains("<dt>Metal</dt><dd>Plata</dd>"));
        assert!(html.contains("<dt>Acabado</dt><dd>Bullion</dd>"));
        assert!(html.contains("<dt>Peso</dt><dd>1 oz</dd>"));
    }

    #[test]
    fn progress_uses_release_status_even_when_a_future_slot_is_owned() {
        let mut issued = slot(
            "issued",
            SlotStatus::Owned {
                quantity: 1,
                items: Vec::new(),
            },
        );
        issued.slot.release_status = ReleaseStatus::Issued;
        let future_owned = slot(
            "future-owned",
            SlotStatus::Owned {
                quantity: 1,
                items: Vec::new(),
            },
        );
        let album = SeriesAlbum {
            series_id: SeriesId::new("field"),
            name: "Field".to_owned(),
            slots: vec![issued, future_owned],
        };

        assert_eq!(progress(&album), (1, 1, 1));
    }

    #[test]
    fn unmatched_view_distinguishes_manual_ambiguous_and_dangling_reasons() {
        let item = |item_id, reason| ItemRef {
            item_id,
            type_id: item_id as u32,
            quantity: 1,
            match_source: None,
            unmatched_reason: Some(reason),
        };
        let album = Album {
            series: Vec::new(),
            unmatched: vec![
                item(1, UnmatchedReason::ExcludedByManualOverride),
                item(
                    2,
                    UnmatchedReason::AmbiguousExplicitTypeId {
                        slot_ids: vec![SlotId::new("a"), SlotId::new("b")],
                    },
                ),
                item(
                    3,
                    UnmatchedReason::InvalidManualOverride {
                        slot_id: SlotId::new("gone"),
                        matching_slots: 0,
                    },
                ),
                item(4, UnmatchedReason::NoMatchingSlot),
            ],
        };

        let html = unmatched("jose", &album, &[], &TypeMetaIndex::new()).into_string();

        assert!(html.contains("manually-excluded"));
        assert!(html.contains("Excluida manualmente"));
        assert!(html.contains("ambiguous"));
        assert!(html.contains("dangling-override"));
        assert!(html.contains("Sin coincidencia"));
        assert_eq!(html.matches("Guardar asignación").count(), 4);
    }

    #[test]
    fn unmatched_uses_exact_cached_titles_with_fallback_and_both_coin_sides() {
        let item = |item_id, type_id| ItemRef {
            item_id,
            type_id,
            quantity: 1,
            match_source: None,
            unmatched_reason: Some(UnmatchedReason::NoMatchingSlot),
        };
        let album = Album {
            series: Vec::new(),
            unmatched: vec![item(41, 10), item(42, 20)],
        };
        let type_meta = TypeMetaIndex::from([(
            10,
            TypeMeta {
                id: 10,
                title: Some("Onza «Dragón ibérico» — edición especial [dragon]".to_owned()),
                display_title: Some("Onza «Dragón ibérico» — edición especial".to_owned()),
                family: None,
                issuer_code: None,
                min_year: None,
                max_year: None,
                weight_oz: None,
                finish: None,
            },
        )]);

        let html = unmatched("jose", &album, &[], &type_meta).into_string();

        assert!(html.contains("<h2>Onza «Dragón ibérico» — edición especial</h2>"));
        assert!(html.contains("<h2>Pieza 42</h2>"));
        assert!(html.contains("ID de pieza 41 · Tipo Numista 10"));
        for expected in [
            "/img/type/10/obverse",
            "/img/type/10/reverse",
            "/img/type/20/obverse",
            "/img/type/20/reverse",
            "alt=\"Anverso de Onza «Dragón ibérico» — edición especial\"",
            "alt=\"Reverso de Pieza 42\"",
        ] {
            assert!(html.contains(expected), "missing `{expected}` in {html}");
        }
        assert_eq!(html.matches("<figcaption>Anverso</figcaption>").count(), 2);
        assert_eq!(html.matches("<figcaption>Reverso</figcaption>").count(), 2);
    }
}
