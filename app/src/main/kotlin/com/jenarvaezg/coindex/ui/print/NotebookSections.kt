package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.CommemorativeProgramme
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.coverageLabel
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.piecesSubject
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.plateCellFootnote
import com.jenarvaezg.coindex.ui.plateCommonFacts
import com.jenarvaezg.coindex.ui.plateEntries
import com.jenarvaezg.coindex.ui.plateMemberStateLabel

/**
 * The whole notebook as pages: one section per card the index is showing, in the index's order.
 *
 * **`página(tarjeta) = su destino`.** Which page a card gets is decided by [destinationOf] and by
 * nothing else, so the paper cannot disagree with the tap: a card whose plate opens prints its
 * plate, a card without an issue list prints its pieces, and the collector's own box goes through
 * that same door (ADR 0021 §9). Adding a second rule here would be a second architecture of
 * information, kept only in the exporter.
 *
 * A card with nothing to draw is dropped rather than printed empty — a box the collector emptied is
 * a page of blank paper, and the notebook is what is *in* the collection.
 */
fun notebookSections(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    programmes: List<CommemorativeProgramme> = emptyList(),
): List<PrintSection> = state.index.mapNotNull { card ->
    val section = when (val destination = destinationOf(card)) {
        is CardDestination.Plate -> plateSection(state, catalogs, programmes, destination.catalogId)
            // A plate that will not resolve is not a reason to skip the card: its pieces are still
            // a collection, and the same fallback the screens have is the one the paper gets.
            ?: piecesSection(state, card)
        is CardDestination.Pieces, is CardDestination.Box -> piecesSection(state, card)
    }
    section.takeIf { it.cells.isNotEmpty() }
}

/**
 * How many cards of the index the export would put on paper, counted without building a page.
 *
 * The button says what it is about to do — «Exportar 12 láminas» — and it says it on every
 * recomposition of the index, so it cannot afford to resolve fifty-six albums to find out. The
 * agreement with [notebookSections] is the one condition that drops a card: having nothing in it.
 */
fun exportableCards(state: CollectionState): Int = state.index.count { card ->
    when (card) {
        is IndexCard.Derived -> card.distinctTypes > 0
        is IndexCard.Box -> card.box.items.isNotEmpty()
    }
}

private fun plateSection(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    programmes: List<CommemorativeProgramme>,
    catalogId: String,
): PrintSection? {
    val plate = resolvePlate(state, catalogs, catalogId, programmes) as? PlateResult.Available
        ?: return null
    val catalog = plate.catalog
    val common = plateCommonFacts(catalog.members)
    return PrintSection(
        // The same claim the exported sheet makes, and for the same reason: the paper outlives the
        // app, and a page that says «curado» about a list nobody curated cannot be taken back.
        eyebrow = "COINDEX · CATÁLOGO CURADO",
        title = catalog.name,
        subtitle = null,
        facts = plateEntries(catalog, plate.album.ownedMembers(), common, plate.programmes),
        source = "Fuente: ${catalog.source}",
        cells = plate.album.members.map { albumMember ->
            val member = albumMember.member
            val owned = albumMember.status is CollectionCatalogMemberStatus.Owned
            PrintCell(
                label = member.label,
                state = plateMemberStateLabel(albumMember.status),
                footnote = plateCellFootnote(member, common),
                diameterMm = state.diameterOf(member.numistaTypeId),
                reverse = member.numistaTypeId?.let { state.images[it]?.reverse },
                filled = owned,
            )
        },
    )
}

private fun piecesSection(state: CollectionState, card: IndexCard): PrintSection {
    val subject: PiecesSubject = piecesSubject(state, card)
    return PrintSection(
        eyebrow = "COINDEX · COLECCIÓN",
        title = subject.title,
        subtitle = subject.variant,
        facts = buildList {
            subject.issuer?.let { issuer -> add("País" to issuer) }
            add(
                "Piezas" to (
                    subject.coverage?.let(::coverageLabel)
                        ?: countLabel(subject.distinctTypes, subject.quantity)
                    ),
            )
        },
        source = "Fuente: tu colección en Numista",
        cells = subject.pieces.map { piece ->
            PrintCell(
                label = state.typeMeta[piece.typeId]?.displayTitle
                    ?: piece.title
                    ?: "Pieza ${piece.id}",
                state = null,
                footnote = pieceLine(piece),
                diameterMm = state.diameterOf(piece.typeId),
                reverse = state.images[piece.typeId]?.reverse,
                // Never a hole: a collection with no issue list has nothing to be missing from,
                // and a box cannot contain one by construction (ADR 0020, ADR 0021 §11).
                filled = true,
            )
        },
    )
}

/** Numista's `size` for one type, in millimetres, or null where nobody recorded one. */
private fun CollectionState.diameterOf(typeId: Int?): Float? =
    typeId?.let { typeMeta[it]?.sizeMillimetres?.toFloat() }
