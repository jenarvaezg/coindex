use domain::{
    Album, AlbumSlot, MatchSource, ReleaseStatus, Series, SeriesAlbum, SlotStatus, TypeMetaIndex,
    UnmatchedReason,
};
use maud::{DOCTYPE, Markup, html};

use crate::config::AppConfig;
use crate::sync::{SyncCallProjection, SyncReport};

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

pub fn index(config: &AppConfig, albums: &[(String, Album)]) -> Markup {
    layout(
        "Álbumes",
        html! {
            section class="intro" {
                p class="eyebrow" { "Cuaderno de colección" }
                h1 { "Láminas de plata" }
                p { "Dos colecciones, ordenadas por serie y emisión." }
            }
            @for user in config.users() {
                @let album = albums.iter().find(|(key, _)| key == &user.key).map(|(_, album)| album);
                section class="user-section" {
                    div class="section-heading" {
                        h2 { (user.key) }
                        div class="sync-actions" {
                            form method="post" action=(format!("/u/{}/sync", user.key)) {
                                button type="submit" { "Sincronizar" }
                            }
                            form method="post" action=(format!("/u/{}/sync?dry_run=true", user.key)) {
                                button type="submit" class="quiet" { "Calcular gasto" }
                            }
                            a href=(format!("/u/{}/unmatched", user.key)) { "Sin clasificar" }
                        }
                    }
                    div class="series-grid" {
                        @if let Some(album) = album {
                            @for series in &album.series {
                                @let (owned, issued, future) = progress(series);
                                article class="series-card" {
                                    p class="plate-number" { "Lámina · " (series.series_id) }
                                    h3 {
                                        a href=(format!("/u/{}/series/{}", user.key, series.series_id)) {
                                            (series.name)
                                        }
                                    }
                                    p class="progress" { (owned) " / " (issued) " emitidas" }
                                    p class="future" { (future) " por emitir" }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
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

pub fn sync_report(user_key: &str, report: &SyncReport) -> Markup {
    layout(
        if report.dry_run {
            "Previsión de sincronización"
        } else {
            "Sincronización completa"
        },
        html! {
            nav class="breadcrumb" { a href="/" { "← Álbumes" } }
            section class="report-card" {
                p class="eyebrow" { (user_key) }
                h1 {
                    @if report.dry_run { "Previsión, sin llamadas" } @else { "Colección sincronizada" }
                }
                p { (report.collection_items) " piezas en el snapshot." }
                @match report.calls {
                    SyncCallProjection::LowerBound {
                        oauth_token,
                        collected_items,
                        local_snapshot_missing_type_metadata,
                        minimum_total,
                        ..
                    } => {
                        dl class="field-card horizontal" {
                            dt { "OAuth mínimo" } dd { (oauth_token) }
                            dt { "Colección" } dd { (collected_items) }
                            dt { "Tipos ausentes en snapshot (orientativo)" } dd { (local_snapshot_missing_type_metadata) }
                            dt { "Mínimo total" } dd { (minimum_total) }
                        }
                    }
                    SyncCallProjection::Estimated {
                        oauth_token,
                        collected_items,
                        type_metadata,
                        total,
                    } => {
                        dl class="field-card horizontal" {
                            dt { "Precisión" } dd { "Estimación post-sync" }
                            dt { "OAuth" } dd { (oauth_token) }
                            dt { "Colección" } dd { (collected_items) }
                            dt { "Tipos" } dd { (type_metadata) }
                            dt { "Total" } dd { (total) }
                        }
                    }
                }
                @if report.dry_run {
                    p class="note" {
                        "Es un límite inferior, no un total exacto: una colección remota modificada puede contener tipos nuevos desconocidos para el snapshot local."
                    }
                }
            }
        },
    )
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
    html! {
        article class=(class) {
            div class="coin-image" {
                @if let Some(type_id) = type_id {
                    (coin_sides(type_id, &album_slot.slot.label))
                } @else {
                    span class="silhouette" aria-hidden="true" {}
                }
                @if owned_items.is_some_and(|items| items.iter().any(|item| matches!(item.match_source, Some(MatchSource::Heuristic { .. })))) {
                    a
                        class="heuristic"
                        href=(format!("#review-{}", album_slot.slot.id))
                        title="Emparejamiento heurístico: revisar o corregir"
                        aria-label="Revisar o corregir emparejamiento heurístico"
                    { "?" }
                }
            }
            div class="slot-copy" {
                p class="slot-state" { (state) }
                h2 { (album_slot.slot.motif) }
                p { (album_slot.slot.year) " · " (album_slot.slot.weight_oz) " oz" }
            }
            @if let Some(items) = owned_items {
                details class="match-review" id=(format!("review-{}", album_slot.slot.id)) {
                    summary { "Revisar piezas" }
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
        Album, AlbumSlot, Finish, ItemRef, MatchSource, ReleaseStatus, SeriesAlbum, SeriesId, Slot,
        SlotId, SlotStatus, TypeMeta, TypeMetaIndex, UnmatchedReason,
    };

    use super::{progress, series, sync_report, unmatched};
    use crate::sync::{SyncCallProjection, SyncReport};

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
        assert!(html.contains("href=\"#review-owned\""));
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
    fn dry_run_view_calls_out_lower_bound_instead_of_exact_total() {
        let report = SyncReport {
            dry_run: true,
            collection_items: 3,
            missing_type_ids: vec![10, 20],
            calls: SyncCallProjection::LowerBound {
                oauth_token: 1,
                collected_items: 1,
                local_snapshot_missing_type_metadata: 2,
                minimum_total: 2,
                unknown_remote_type_metadata: true,
            },
        };

        let html = sync_report("jose", &report).into_string();

        assert!(html.contains("límite inferior"));
        assert!(html.contains("Mínimo total"));
        assert!(!html.contains(">Total<"));
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
