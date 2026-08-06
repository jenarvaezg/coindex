package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CoinPhoto
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.TypeImages
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
 * **[options] reaches the cells and not only the millimetres** (#228). What a cell *is* depends on the
 * configuration: «QR de Numista» gives it a code to point a phone at (#234), «ambas caras» gives it a
 * second face (#230), and printing no photographs will give it neither (#231). That is the door #228
 * laid, and the ticket that opens one does not have to re-thread the ViewModel and the index screen
 * to get here.
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
                faces = state.facesOf(member.numistaTypeId, options),
                filled = owned,
                numistaUrl = state.qrUrlOf(member.numistaTypeId, options),
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
                faces = state.facesOf(piece.typeId, options),
                // Never a hole: a collection with no issue list has nothing to be missing from,
                // and a box cannot contain one by construction (ADR 0020, ADR 0021 §11).
                filled = true,
                numistaUrl = state.qrUrlOf(piece.typeId, options),
            )
        },
    )
}

/** Numista's `size` for one type, in millimetres, or null where nobody recorded one. */
private fun CollectionState.diameterOf(typeId: Int?): Float? =
    typeId?.let { typeMeta[it]?.sizeMillimetres?.toFloat() }

/**
 * The faces this cell prints: none (#231), the reverse, or the obverse and the reverse (#230).
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
private fun CollectionState.facesOf(typeId: Int?, options: NotebookOptions): List<CoinPhoto> {
    if (!options.photographs) return emptyList()
    val sides = typeId?.let { images[it] } ?: TypeImages()
    return if (options.bothFaces) listOf(sides.obverse, sides.reverse) else listOf(sides.reverse)
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
