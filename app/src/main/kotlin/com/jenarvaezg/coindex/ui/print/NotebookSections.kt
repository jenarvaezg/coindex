package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CoinPhoto
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.coverageLabel
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.piecesSubject
import com.jenarvaezg.coindex.ui.plateCellFootnote
import com.jenarvaezg.coindex.ui.plateCommonFacts
import com.jenarvaezg.coindex.ui.plateEntries
import com.jenarvaezg.coindex.ui.plateMemberStateLabel

/**
 * The whole notebook as pages: one section per card, in the order they were handed over.
 *
 * **`página(tarjeta) = su destino`.** Which page a card gets is decided by [destinationOf] and by
 * nothing else, so the paper cannot disagree with the tap: a card whose plate opens prints its
 * plate, a card without an issue list prints its pieces, and the collector's own box goes through
 * that same door (ADR 0021 §9). Adding a second rule here would be a second architecture of
 * information, kept only in the exporter.
 *
 * **[cards] is what the index is showing**, passed in rather than read off [CollectionState.index]:
 * the unit of the export is what is on screen at that moment, filters and search included, and
 * reading the whole index here would silently print what the collector had just narrowed away. One
 * card in, one section out — nothing is dropped, because what stays out of the notebook is a
 * question for the index and not for the printer (#147).
 *
 * **[options] reaches the cells and not only the millimetres** (#228): what a cell *is* depends on
 * the configuration too — a notebook printed without photographs carries none, so no page of it ever
 * waits for a picture. How tall that cell then gets is arithmetic, and lives with the geometry.
 */
fun notebookSections(
    state: CollectionState,
    cards: List<IndexCard>,
    curation: Curation,
    options: NotebookOptions,
): List<PrintSection> = cards.map { card ->
    when (val destination = destinationOf(card)) {
        is CardDestination.Plate -> plateSection(state, curation, destination.catalogId, options)
            // A plate that will not resolve is not a reason to skip the card: its pieces are still
            // a collection, and the same fallback the screens have is the one the paper gets.
            ?: piecesSection(state, card, options)
        is CardDestination.Pieces, is CardDestination.Box -> piecesSection(state, card, options)
    }
}

private fun plateSection(
    state: CollectionState,
    curation: Curation,
    catalogId: String,
    options: NotebookOptions,
): PrintSection? {
    val plate = resolvePlate(state, curation, catalogId) as? PlateResult.Available
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
                // A hole keeps **its own** real diameter: the type of a member the collector is
                // missing is in the seeded cache like any other, so the empty mount is the size of
                // the coin that goes in it. Only a member no Numista type backs at all — announced,
                // unlisted — has nothing to be measured, and borrows the plate's.
                diameterMm = state.diameterOf(member.numistaTypeId),
                reverse = options.reverseOf(member.numistaTypeId, state),
                filled = owned,
            )
        },
    )
}

private fun piecesSection(
    state: CollectionState,
    card: IndexCard,
    options: NotebookOptions,
): PrintSection {
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
                reverse = options.reverseOf(piece.typeId, state),
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

/**
 * The reverse this cell prints, or nothing at all where the notebook prints no coins.
 *
 * A cell with no photograph asks for none, which is what makes the checklist of #231 export in
 * seconds and with no hole possible: the export waits on the pictures its pages carry, so removing
 * them here is what removes the wait.
 */
private fun NotebookOptions.reverseOf(typeId: Int?, state: CollectionState): CoinPhoto? =
    typeId?.takeIf { photographs }?.let { state.images[it]?.reverse }
