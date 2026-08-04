package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.ProgrammeStanding

/**
 * The facts every member of a catalog shares, which therefore belong to the plate and not to
 * its cells.
 *
 * An issue run repeats the year across its cells, so a footnote built from it said the same thing
 * in twenty-one cells at once. The type is here for the same reason but is never handed back to a
 * cell: it either heads the whole plate or it is not on the plate at all (see [plateCellFootnote]).
 */
data class PlateCommonFacts(val numistaTypeId: Int?, val year: Int?)

fun plateCommonFacts(members: List<CollectionCatalogMember>): PlateCommonFacts {
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
fun plateCellFootnote(member: CollectionCatalogMember, common: PlateCommonFacts): String? {
    val year = member.year ?: return null
    if (common.year != null || member.label == year.toString()) return null
    return year.toString()
}

/**
 * What the collector is told once the sheet has been handed to the share sheet.
 *
 * The exported plate **is** the product: it gets sent to whoever the collection is being shown
 * to, holes included. The old message called any sheet complete as long as every picture had
 * reported back, and a picture that failed reported back exactly like one that arrived — so a
 * sheet with twelve empty cells announced itself as «lámina completa» (issue #67). Counting the
 * ones that actually painted is what makes the sentence true.
 */
fun plateExportMessage(members: Int, expectedPhotos: Int, loadedPhotos: Int): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    return when (absent) {
        // «Casillas» and not «emisiones»: a plate can draw a slot the mint has not struck, and
        // the progress line right above it counts only what was.
        0 -> "Lámina completa exportada · $members casillas"
        1 -> "Lámina exportada, pero una foto no llegó a cargar"
        else -> "Lámina exportada, pero $absent fotos no llegaron a cargar"
    }
}

/** The state prose shared by the interactive plate and the exported sheet. */
fun plateMemberStateLabel(status: CollectionCatalogMemberStatus): String = when (status) {
    CollectionCatalogMemberStatus.Missing -> "Me falta"
    CollectionCatalogMemberStatus.Unlisted -> "Sin ficha"
    CollectionCatalogMemberStatus.NotYetIssued -> "Sin emitir"
    is CollectionCatalogMemberStatus.Owned ->
        if (status.quantity > 1) "Tengo · ×${status.quantity}" else "Tengo"
}

/**
 * The plate's specification block, shared by the screen and the exported sheet so both say the
 * same things in the same order.
 *
 * Whatever [plateCommonFacts] lifts out of the cells lands here, once.
 */
fun plateEntries(
    catalog: CollectionCatalog,
    ownedMembers: Int,
    common: PlateCommonFacts = plateCommonFacts(catalog.members),
    programmes: List<ProgrammeStanding> = emptyList(),
): List<Pair<String, String>> {
    val announced = catalog.members.count { it.isAnnounced }
    val unlisted = catalog.members.count { it.isUnlisted }
    return buildList {
        // The divisor is what the app can measure (#48), which is exactly the issued members.
        val measurable = catalog.members.count { it.isIssued }
        add("Progreso" to "$ownedMembers / $measurable emisiones")
        if (announced > 0) {
            add("Sin emitir" to if (announced == 1) "1 anunciada" else "$announced anunciadas")
        }
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
}
