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
import com.jenarvaezg.coindex.domain.saturatingAdd
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.DrawnPiece
import com.jenarvaezg.coindex.ui.PiecesSubject
import com.jenarvaezg.coindex.ui.PlateValue
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.countSentence
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.pieceLine
import com.jenarvaezg.coindex.ui.pieceName
import com.jenarvaezg.coindex.ui.piecesSubject
import com.jenarvaezg.coindex.ui.plateSubject

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
 * **[options] reaches the cells and not only the millimetres** (#228). What a cell *is* depends on the
 * configuration: «QR de Numista» gives it a code to point a phone at (#234), «ambas caras» gives it a
 * second face (#230), and printing no photographs will give it neither (#231). That is the door #228
 * laid, and the ticket that opens one does not have to re-thread the ViewModel and the index screen
 * to get here.
 *
 * **[unclaimed] is the one thing on paper that is not a card** (#275), and it arrives already chosen
 * for the same reason [cards] does: which coins no collection claims is measured against the whole
 * index, and which of them survive the filter is the index screen's answer and not the printer's. It
 * comes in as pieces rather than as a switch over the state so that this function keeps having no
 * opinion about what belongs in the notebook — it only knows where it goes, which is last.
 */
fun notebookSections(
    state: CollectionState,
    cards: List<IndexCard>,
    unclaimed: List<CollectedItem>,
    curation: Curation,
    options: NotebookOptions,
    /**
     * What a plate is worth, or null for every plate when the money switch is off (#228, ADR 0021 §13).
     *
     * The switch is answered **here and once**, by handing this function nothing rather than by asking
     * the options again further down: a drawer that has no amount cannot print one, which is what makes
     * «apagarlo no deja escapar ninguna cifra derivada de dinero» a property of the code rather than a
     * promise about it.
     */
    plateValue: (PlateResult.Available) -> PlateValue? = { null },
): List<PrintSection> = cards.map { card ->
    when (val destination = destinationOf(card)) {
        is CardDestination.Plate ->
            plateSection(state, curation, destination.catalogId, options, plateValue)
            // A plate that will not resolve is not a reason to skip the card: its pieces are still
            // a collection, and the same fallback the screens have is the one the paper gets.
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
): PrintSection? {
    val resolved = resolvePlate(state, curation, catalogId) as? PlateResult.Available
        ?: return null
    // The same plate the screen and the exported sheet draw (#218), so the three cannot word one
    // catalog three ways: the heading, the specification and what every cell says arrive settled.
    val plate = plateSubject(resolved, plateValue(resolved))
    return PrintSection(
        // The same claim the exported sheet makes, and for the same reason: the paper outlives the
        // app, and a page that says «curado» about a list nobody curated cannot be taken back.
        eyebrow = "COINDEX · CATÁLOGO CURADO",
        title = plate.title,
        subtitle = null,
        // The value joins the specification rather than the heading, because the printed page has no
        // header to raise a figure into — which is the same reason the ratio reaches paper as a row
        // (`plateEntriesBesideRatio` is deliberately not called here).
        facts = plate.entries + listOfNotNull(plate.value?.let { VALUE_LABEL to it }),
        source = plate.source,
        // The stamp travels to the PDF because it is a state (ADR 0026 §4 / #371): the subject
        // already knew, and until now the section threw the bit away.
        complete = plate.complete,
        cells = plate.cells.map { cell ->
            PrintCell(
                curatedLabel = cell.label,
                state = null,
                footnote = cell.footnote,
                // A hole keeps **its own** real diameter: the type of a member the collector is
                // missing is in the seeded cache like any other, so the empty mount is the size of
                // the coin that goes in it. Only a member no Numista type backs at all — announced,
                // unlisted — has nothing to be measured, and borrows the plate's.
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
        eyebrow = "COINDEX · COLECCIÓN",
        title = subject.title,
        subtitle = subject.variant,
        facts = buildList {
            subject.issuer?.let { issuer -> add("País" to issuer) }
            add("Piezas" to subject.countSentence)
        },
        source = "tu colección en Numista",
        cells = subject.pieces.map { piece ->
            val item = piece.item
            val name = pieceName(state, item)
            PrintCell(
                name = name,
                state = null,
                footnote = pieceLine(piece),
                diameterMm = state.diameterOf(item.typeId),
                // The reverse, spelled out: a piece printed off a card with no issue list has no
                // plate to declare a face (#227), so the honest thing is what it printed yesterday.
                faces = state.facesOf(item.typeId, options, PrintedSide.Reverse),
                // Never a hole: a collection with no issue list has nothing to be missing from,
                // and a box cannot contain one by construction (ADR 0020, ADR 0021 §11).
                filled = true,
                numistaUrl = state.qrUrlOf(item.typeId, options),
            )
        },
    )
}

/**
 * The last lámina: every coin no collection claims, so the notebook is the whole collection (#275).
 *
 * **It is a lámina and not a page of its own kind.** Same eyebrow, same heading, same cells with
 * their photograph and their real diameter, and it obeys the five switches of #228 like every other
 * page — the alternative, a compact list that always fits one folio, would be the only page of the
 * notebook that does not look like the notebook.
 *
 * **A cell per row**, as in [piecesSection] and for the sharper reason: being claimed is decided per
 * row, so the leftover row of a type whose sibling fills a member of its plate (ADR 0019) is what
 * prints, not the coin entire.
 *
 * **No cell says why it is here.** ADR 0021 §12 took the reason line off the screen and sent the why
 * to the field report, which is where the curator looks; «sin familia en Numista» under a thaler is
 * jargon in the one notebook that leaves the house.
 *
 * Null on an empty list, so no folio is ever spent on a heading with nothing under it.
 */
private fun unclaimedSection(
    state: CollectionState,
    unclaimed: List<CollectedItem>,
    options: NotebookOptions,
): PrintSection? {
    if (unclaimed.isEmpty()) return null
    return PrintSection(
        // Not «COLECCIÓN», which is the eyebrow of a page that is one: this is the page of the coins
        // that are in none, and the header is where that is said once instead of cell by cell.
        eyebrow = "COINDEX · SIN COLECCIÓN",
        title = "Sin colección",
        subtitle = null,
        // No «País»: a page that spans twenty of them has none to name, and the countries are the
        // order the coins are already in.
        //
        // [countLabel] and not the expression spelled out, which is the whole of #226: these coins
        // have no issue list and therefore no ratio, so this is exactly what `countSentence` reduces
        // to for them — the same function, reached without inventing a subject to hang it on.
        facts = listOf(
            "Piezas" to countLabel(
                distinctTypes = unclaimed.distinctBy { it.typeId }.size,
                quantity = unclaimed.fold(0) { total, it -> saturatingAdd(total, it.quantity) },
            ),
        ),
        source = "tu colección en Numista",
        cells = unclaimed.map { item ->
            val name = pieceName(state, item)
            PrintCell(
                name = name,
                state = null,
                // The emission label of a coin no catalog claims is normally nothing, but it is
                // asked for rather than assumed: which emission a coin is is a fact about the coin,
                // and a catalog keyed on issues can name a row it does not claim.
                footnote = pieceLine(DrawnPiece(item, state.emissionLabels[item.id])),
                diameterMm = state.diameterOf(item.typeId),
                // The reverse, spelled out, exactly as a piece printed off a card with no issue
                // list: there is no plate here to declare a face (#227).
                faces = state.facesOf(item.typeId, options, PrintedSide.Reverse),
                // Never a hole: every one of these is a coin the collector owns.
                filled = true,
                numistaUrl = state.qrUrlOf(item.typeId, options),
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
 * **Which one, when it is one, is the plate's declaration and not this function's** (#227). Every
 * caller says which, with no default to fall through: a piece printed off a card with no issue list
 * has no plate to declare anything and asks for [PrintedSide.Reverse] out loud, so the day that
 * residue gets a face of its own — #216 is emptying it — the place to write it is the call and not a
 * silent parameter.
 *
 * **How many is the configuration's answer and not the cache's.** A type the cache has never seen —
 * an announced member, an unlisted one — gets its slots empty rather than fewer of them: the cells
 * of a plate have to line up, and one coin printed where its neighbours print a pair reads as a
 * misprint. What an empty slot draws is the renderer's business, and it is what it already drew for
 * a reverse nobody had.
 *
 * **An empty list is what makes «sin fotos» the export that cannot come out incomplete.** No face is
 * no candidate URL, so `notebookPhotographs` has nothing to warm, no page waits for a decode, and the
 * closing message divides by a denominator of zero photographs — it cannot claim that three of them
 * failed to load in a notebook that never asked for one.
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
 * The Numista page a cell's code points at, which is the page of its **type** (#234).
 *
 * **There is no URL per issue, and it was looked for.** The five paquillos are five members of one
 * type qualified by `numista_issue_ids` (ADR 0019), so this hands all five the same code — and the
 * alternative does not exist: a type page marks each issue only with the `id` of the empty row its
 * collection widget fills in (`collec_line8508`), no link on Numista points at one, and no `?issue=`
 * of any shape is read. The fragment that id would make is 42 characters, which is a version 3, and
 * because the caption is a constant of the layout the largest code in the notebook is what every page
 * pays for. So the promise the code makes is «esta moneda en Numista», and the ficha of a paquillo is
 * the ficha of the type.
 *
 * Null when the switch is off, so a default notebook holds no URL at all, and null for a member no
 * Numista type backs — an announced one, an unlisted one — because a code that leads nowhere is worse
 * than no code.
 */
private fun CollectionState.qrUrlOf(typeId: Int?, options: NotebookOptions): String? =
    typeId?.takeIf { options.numistaQr }?.let { typeMeta[it]?.numistaUrl }

/** What the value row is called on paper. Same word the screen uses over the plate. */
private const val VALUE_LABEL = "Valor"
