package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.saturatingAdd
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.PlateValue
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.weldUnits
import com.jenarvaezg.coindex.ui.wishCensusLabel
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.countSentence
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.pieceName
import com.jenarvaezg.coindex.ui.piecesSubject
import com.jenarvaezg.coindex.ui.plateAmountLabel
import com.jenarvaezg.coindex.ui.plateSubject

/**
 * The whole notebook as pages: one section per card, in the order they were handed over. Which page
 * a card gets is decided by [destinationOf] alone, so the paper matches what a tap opens: a plate,
 * or the pieces of a card without an issue list, the box included (ADR 0021 §9).
 *
 * [cards] is what the index is showing, filters and search included, rather than
 * [CollectionState.index]: what stays out of the notebook is the index's decision (#147). [options]
 * shapes the cells as well as the millimetres (#228): a code (#234), a second face (#230), or no
 * photographs at all (#231). [unclaimed] (#275) arrives already filtered by the index screen for
 * the same reason, and prints last.
 */
fun notebookSections(
    state: CollectionState,
    cards: List<IndexCard>,
    unclaimed: List<CollectedItem>,
    curation: Curation,
    options: NotebookOptions,
    /**
     * What a plate is worth, or null for every plate when the money switch is off (#228,
     * ADR 0021 §13). The caller applies the switch by passing the default, so no code below can
     * print an amount it was never given.
     */
    plateValue: (PlateResult.Available) -> PlateValue? = { null },
    /**
     * The casillas the collector marked, so the marks reach the paper (ADR 0026 §4, ADR 0029 §7).
     * The mark prints in the cell's otherwise unused `state` line, so it costs no millimetre. Not
     * behind the money switch: «lo busco» is not an amount.
     */
    wished: Set<WishKey> = emptySet(),
): List<PrintSection> = cards.map { card ->
    when (val destination = destinationOf(card)) {
        is CardDestination.Plate ->
            plateSection(state, curation, destination.catalogId, options, plateValue, wished)
            // An unresolvable plate falls back to its pieces, as on screen.
            ?: piecesSection(state, card, options)
        is CardDestination.Pieces, is CardDestination.Box -> piecesSection(state, card, options)
    }
} + listOfNotNull(unclaimedSection(state, unclaimed, options).takeIf { options.unclaimed })

private fun plateSection(
    state: CollectionState,
    curation: Curation,
    catalogId: String,
    options: NotebookOptions,
    plateValue: (PlateResult.Available) -> PlateValue?,
    wished: Set<WishKey>,
): PrintSection? {
    val resolved = resolvePlate(state, curation, catalogId) as? PlateResult.Available
        ?: return null
    // The same plate subject the screen and the exported sheet draw (#218), so heading,
    // specification and cells are worded once.
    //
    // Money is the one thing paper words for itself: the screen's amount as a «Valor» row, with
    // «Tasación» under it (#493, #594). The closing cost and the price in each hole don't reach
    // paper (ADR 0021 §13).
    val amount = plateValue(resolved)
    val plate = plateSubject(resolved, wished = wished)
    return PrintSection(
        eyebrow = PLATE_SECTION_EYEBROW,
        title = plate.title,
        subtitle = null,
        // The printed page has no header to raise a figure into, so value and ratio are rows here
        // (`plateEntriesBesideRatio` is deliberately not called).
        facts = plate.entries + listOfNotNull(
            amount?.let { VALUE_FACT_LABEL to plateAmountLabel(it) },
            // Read off the amount, so the money switch withholds the date with it (#594).
            amount?.catalogReadAt?.let { VALUATION_FACT_LABEL to printedValuationLabel(it) },
        ),
        source = plate.source,
        // The stamp is a state, so it reaches the PDF (ADR 0026 §4, #371) with the subject's ratio;
        // paper never recounts it from cells.
        ratio = plate.ratio,
        complete = plate.complete,
        cells = plate.cells.map { cell ->
            PrintCell(
                curatedLabel = cell.label,
                // Only «lo busco» on the marked holes (ADR 0029 §7); a printed plate shows no other
                // state, so the page isn't «TENGO» under every coin.
                state = WishLabels.MARK_WORD.takeIf { cell.wished },
                footnote = cell.footnote,
                // A hole keeps its own type's diameter; only a member with no Numista type
                // (announced, unlisted) borrows the plate's.
                diameterMm = state.diameterOf(cell.numistaTypeId),
                faces = state.facesOf(cell.numistaTypeId, options, plate.printedSide),
                filled = cell.owned,
                numistaUrl = state.qrUrlOf(cell.numistaTypeId, options),
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
        eyebrow = PIECES_SECTION_EYEBROW,
        title = subject.title,
        subtitle = subject.variant,
        facts = buildList {
            subject.issuer?.let { issuer -> add(COUNTRY_FACT_LABEL to issuer) }
            add(PIECES_FACT_LABEL to subject.countSentence)
        },
        source = INVENTORY_SECTION_SOURCE,
        cells = subject.pieces.map { piece ->
            val item = piece.item
            val name = pieceName(state, item)
            PrintCell(
                name = name,
                state = null,
                footnote = pieceLine(piece),
                diameterMm = state.diameterOf(item.typeId),
                // No plate to declare a face (#227), so the reverse.
                faces = state.facesOf(item.typeId, options, PrintedSide.Reverse),
                // Never a hole: without an issue list nothing can be missing, and a box can't hold
                // one (ADR 0020, ADR 0021 §11).
                filled = true,
                numistaUrl = state.qrUrlOf(item.typeId, options),
            )
        },
    )
}

/**
 * The last lámina: every coin no collection claims, so the notebook is the whole collection (#275).
 * An ordinary lámina obeying the same switches (#228), with a cell per inventory row, since being
 * claimed is decided per row (ADR 0019). No cell says why it is unclaimed: that reason lives in the
 * field report (ADR 0021 §12). Null on an empty list, so no folio holds a bare heading.
 */
private fun unclaimedSection(
    state: CollectionState,
    unclaimed: List<CollectedItem>,
    options: NotebookOptions,
): PrintSection? {
    if (unclaimed.isEmpty()) return null
    return PrintSection(
        eyebrow = UNCLAIMED_SECTION_EYEBROW,
        title = UNCLAIMED_SECTION_TITLE,
        subtitle = null,
        // No «País»: the page spans many. [countLabel] is what `countSentence` reduces to for coins
        // with no issue list, so the count matches the screen's (#226).
        facts = listOf(
            PIECES_FACT_LABEL to countLabel(
                distinctTypes = unclaimed.distinctBy { it.typeId }.size,
                quantity = unclaimed.fold(0) { total, it -> saturatingAdd(total, it.quantity) },
            ),
        ),
        source = INVENTORY_SECTION_SOURCE,
        cells = unclaimed.map { item ->
            val name = pieceName(state, item)
            PrintCell(
                name = name,
                state = null,
                // Usually null, but a catalog keyed on issues can label a row it doesn't claim.
                footnote = pieceLine(DrawnPiece(item, state.emissionLabels[item.id])),
                diameterMm = state.diameterOf(item.typeId),
                // No plate to declare a face (#227), so the reverse.
                faces = state.facesOf(item.typeId, options, PrintedSide.Reverse),
                filled = true,
                numistaUrl = state.qrUrlOf(item.typeId, options),
            )
        },
    )
}

/**
 * «La lista de lo que busco» on paper: the marked casillas of every plate in one lámina, with the
 * same cells and switches as any other (ADR 0029 §7). Not an index card, so it is exported from the
 * annex through this function. It is the only way to print marks on «Explorar» plates, which have
 * no «Exportar» (#282, decision 8).
 *
 * The cells carry no «lo busco»: every casilla here is marked, so by the frequency rule of
 * ADR 0026 §5 the word would distinguish nothing. Null on an empty list, so no folio holds a bare
 * heading.
 */
fun wishSections(
    state: CollectionState,
    slots: List<WishedSlot>,
    options: NotebookOptions,
): List<PrintSection> = listOfNotNull(wishSection(state, slots, options))

private fun wishSection(
    state: CollectionState,
    slots: List<WishedSlot>,
    options: NotebookOptions,
): PrintSection? {
    if (slots.isEmpty()) return null
    return PrintSection(
        eyebrow = WISH_SECTION_EYEBROW,
        title = WishLabels.DESTINATION,
        subtitle = null,
        // The screen's census, from the same function.
        facts = listOf(
            WISH_SECTION_COUNT_LABEL to wishCensusLabel(
                slots = slots.size,
                plates = slots.distinctBy { it.catalog.id }.size,
            ),
        ),
        source = WISH_SECTION_SOURCE,
        cells = slots.map { slot ->
            PrintCell(
                curatedLabel = slot.member.label.weldUnits(),
                // The footnote line, which on a plate holds the year, names the casilla's lámina;
                // the year is already in nearly every date run's label.
                state = null,
                footnote = slot.catalog.shortName.weldUnits(),
                diameterMm = state.diameterOf(slot.typeId),
                faces = state.facesOf(slot.typeId, options, slot.catalog.printedSide),
                // Every cell is a coin the collector doesn't have.
                filled = false,
                numistaUrl = state.qrUrlOf(slot.typeId, options),
            )
        },
    )
}

/** Numista's `size` for one type, in millimetres, or null where nobody recorded one. */
private fun CollectionState.diameterOf(typeId: Int?): Float? =
    typeId?.let { typeMeta[it]?.sizeMillimetres?.toFloat() }

/**
 * The faces this cell prints: none (#231), the declared one, or the obverse and the reverse (#230).
 *
 * Which single face is the caller's [printedSide], with no default (#227). How many depends only on
 * [options]: a type missing from the cache gets empty slots rather than fewer, so the cells of a
 * plate line up. With the photographs off the list is empty, so `notebookPhotographs` has nothing
 * to download and the export can't report missing pictures.
 */
private fun CollectionState.facesOf(
    typeId: Int?,
    options: NotebookOptions,
    printedSide: PrintedSide,
): List<CoinPhoto> {
    if (!options.photographs) return emptyList()
    val sides = typeId?.let { images[it] } ?: TypeImages()
    if (options.bothFaces) return listOf(sides.obverse, sides.reverse)
    return listOf(
        when (printedSide) {
            PrintedSide.Obverse -> sides.obverse
            PrintedSide.Reverse -> sides.reverse
        },
    )
}

/**
 * The Numista page a cell's code points at: its type's page (#234). Numista has no per-issue URL
 * (an issue is only an element `id` in the type page's collection widget, and no `?issue=`
 * parameter is read), so members split by `numista_issue_ids` (ADR 0019) share one code. Null when
 * the switch is off, or for a member no Numista type backs.
 */
private fun CollectionState.qrUrlOf(typeId: Int?, options: NotebookOptions): String? =
    typeId?.takeIf { options.numistaQr }?.let { typeMeta[it]?.numistaUrl }
