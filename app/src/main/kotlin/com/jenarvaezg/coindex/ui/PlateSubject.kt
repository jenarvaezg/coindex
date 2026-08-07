package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.ProgrammeStanding

/**
 * What the three drawers of a plate are looking at: the screen, the exported sheet, the notebook.
 *
 * **Built once from [PlateResult.Available] and consumed whole** (#218). The plate used to be taken
 * apart at the edge of the screen and its four pieces threaded by hand through four signatures of
 * six and eight parameters, which is how the same facts came to be recomputed three times over —
 * once in the body of a `LazyVerticalGrid`, with no `remember` under it — and how the exported
 * sheet ended up recomputing them again while the export was in flight. `PiecesSubject` had already answered this
 * for the collections without an issue list; this is the same answer for the ones with one.
 *
 * **Nothing here is a catalog and nothing here is an album.** What a drawer needs is prose and
 * pictures: a heading, a specification already worded, and cells that know what they say. The
 * counting and the lifting of shared facts happen once, in [plateSubject], and no drawer can do
 * either — which is what makes «Progreso» and the card's ratio one number instead of two.
 */
data class PlateSubject(
    /** Names the export file and keys the picture the sheet is recorded into. */
    val catalogId: String,
    val title: String,
    val source: String,
    /** Which face a cell prints when it prints one (#227). The plate declares it, never the cell. */
    val printedSide: PrintedSide,
    /** The specification block, in the order all three drawers print it. */
    val entries: List<Pair<String, String>>,
    val cells: List<DrawnCell>,
)

/**
 * One casilla of a plate as it is drawn, with everything it says already resolved.
 *
 * The parallel of [DrawnPiece], and for the same reason: what a cell has left to say under its own
 * title is decided by the whole plate — see [plateCellFootnote] — so a drawer that received the raw
 * member would have to ask the plate again, three times, with three chances to ask differently.
 *
 * [id] is the member's, and it is here because a lazy grid needs a stable key: the label of a cell
 * is what the collector reads, not a promise of uniqueness.
 */
data class DrawnCell(
    val id: String,
    val label: String,
    /** The Numista type behind it, or null for an announced or unlisted casilla. */
    val numistaTypeId: Int?,
    /** «Tengo · ×2», «Me falta», «Sin ficha», «Sin emitir». */
    val state: String,
    val footnote: String?,
    val owned: Boolean,
)

/**
 * The plate of one catalog, worded once.
 *
 * Takes the resolution and not its pieces so that the pieces cannot arrive apart: an album belongs
 * to the catalog it was built from, and the programmes to both.
 */
fun plateSubject(plate: PlateResult.Available): PlateSubject {
    val catalog = plate.catalog
    // Off the album and not off the catalog, so the heading is lifted out of the very cells the
    // plate is about to draw: the album is the plate as this collector has it.
    val common = plateCommonFacts(plate.album.members.map { it.member })
    return PlateSubject(
        catalogId = catalog.id,
        title = catalog.name,
        source = catalog.source,
        printedSide = catalog.printedSide,
        entries = plateEntries(catalog, plate.album, common, plate.programmes),
        cells = plate.album.members.map { albumMember ->
            DrawnCell(
                id = albumMember.member.id,
                label = albumMember.member.label,
                numistaTypeId = albumMember.member.numistaTypeId,
                state = plateMemberStateLabel(albumMember.status),
                footnote = plateCellFootnote(albumMember.member, common),
                owned = albumMember.status is CollectionCatalogMemberStatus.Owned,
            )
        },
    )
}

/**
 * The facts every member of a catalog shares, which therefore belong to the plate and not to
 * its cells.
 *
 * An issue run repeats the year across its cells, so a footnote built from it said the same thing
 * in twenty-one cells at once. The type is here for the same reason but is never handed back to a
 * cell: it either heads the whole plate or it is not on the plate at all (see [plateCellFootnote]).
 */
private data class PlateCommonFacts(val numistaTypeId: Int?, val year: Int?)

private fun plateCommonFacts(members: List<CollectionCatalogMember>): PlateCommonFacts {
    // An announced member has neither type nor, often, a year. An unlisted member does have a
    // real year, so it participates here even though its absent type is absorbed by mapNotNull.
    val issued = members.filterNot { it.isAnnounced }
    return PlateCommonFacts(
        numistaTypeId = issued.mapNotNull { it.numistaTypeId }.distinct().singleOrNull(),
        year = issued.mapNotNull { it.year }.distinct().singleOrNull(),
    )
}

/**
 * What one cell has left to say under its own title: the year, unless the title is already the
 * year or every cell shares it.
 *
 * Null when nothing is left, which is the common case of any catalog whose cells are titled with
 * their year.
 *
 * The Numista type is deliberately absent (issue #88). Lifting it only when every cell agreed left
 * it in the cells of the forty-three catalogs whose cells do not: twenty-one repetitions of one
 * identifier in the fuertes, and a hundred and twenty-one distinct ones — no norm, so no exception
 * to annotate — in the Russian personalities. A type identifier is not what a plate says under a
 * coin: on screen the cell title already links to its Numista page, and the exported sheet is a
 * picture the collection is shown with. Where the type does belong is the plate's own
 * specification, and only when the whole plate is that type — see [plateEntries].
 */
private fun plateCellFootnote(member: CollectionCatalogMember, common: PlateCommonFacts): String? {
    val year = member.year ?: return null
    if (common.year != null || member.label == year.toString()) return null
    return year.toString()
}

/** The state prose shared by the interactive plate and the exported sheet. */
private fun plateMemberStateLabel(status: CollectionCatalogMemberStatus): String = when (status) {
    CollectionCatalogMemberStatus.Missing -> "Me falta"
    CollectionCatalogMemberStatus.Unlisted -> "Sin ficha"
    CollectionCatalogMemberStatus.NotYetIssued -> "Sin emitir"
    is CollectionCatalogMemberStatus.Owned ->
        if (status.quantity > 1) "Tengo · ×${status.quantity}" else "Tengo"
}

/**
 * The plate's specification block, said once for the three drawers.
 *
 * Whatever [plateCommonFacts] lifts out of the cells lands here, once.
 *
 * **Every count is the album's** (#218). The divisor the plate prints is the divisor the card
 * divided by — `CollectionCatalogAlbum.issuedMembers` — and the two prose lines next to it count
 * the same statuses rather than the catalog's flags a second time.
 */
private fun plateEntries(
    catalog: CollectionCatalog,
    album: CollectionCatalogAlbum,
    common: PlateCommonFacts,
    programmes: List<ProgrammeStanding>,
): List<Pair<String, String>> = buildList {
    // The divisor is what the app can measure (#48), which is exactly the issued members.
    add("Progreso" to "${album.ownedMembers()} / ${album.issuedMembers()} emisiones")
    val announced = album.announcedMembers()
    if (announced > 0) {
        add("Sin emitir" to if (announced == 1) "1 anunciada" else "$announced anunciadas")
    }
    val unlisted = album.unlistedMembers()
    if (unlisted > 0) {
        add(
            "Sin ficha" to
                if (unlisted == 1) "1 emisión no medible" else "$unlisted emisiones no medibles",
        )
    }
    // The second reading (ADR 0022), after the plate's own progress and never mixed into it:
    // its denominator counts coins no catalog of this project claims.
    programmes.forEach { standing ->
        add(
            "Programa" to "${standing.programme.shortName} · " +
                "${standing.progress.owned} de ${standing.progress.total}",
        )
    }
    addAll(variantEntries(catalog.weightMillioz, catalog.finish))
    common.numistaTypeId?.let { typeId -> add("Tipo" to "Numista $typeId") }
    common.year?.let { year -> add("Año" to year.toString()) }
    add("Actualizado" to catalog.updatedAt)
}
