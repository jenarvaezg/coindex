package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember

/**
 * The facts every member of a catalog shares, which therefore belong to the plate and not to
 * its cells.
 *
 * A date run repeats one Numista type across years and an issue run repeats the year as well, so
 * a footnote built from both said the same thing in twenty-one cells at once.
 */
data class PlateCommonFacts(val numistaTypeId: Int?, val year: Int?)

fun plateCommonFacts(members: List<CollectionCatalogMember>): PlateCommonFacts =
    PlateCommonFacts(
        numistaTypeId = members.map { it.numistaTypeId }.distinct().singleOrNull(),
        year = members.map { it.year }.distinct().singleOrNull(),
    )

/**
 * What one cell has left to say under its own title: the year, unless the title is already the
 * year or every cell shares it, and the Numista type, unless the whole plate is one type.
 *
 * Null when nothing is left, which is the common case of a date run.
 */
fun plateCellFootnote(member: CollectionCatalogMember, common: PlateCommonFacts): String? {
    val parts = buildList {
        if (common.year == null && member.label != member.year.toString()) {
            add(member.year.toString())
        }
        if (common.numistaTypeId == null) {
            add("Numista ${member.numistaTypeId}")
        }
    }
    return parts.joinToString(" · ").ifEmpty { null }
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
        0 -> "Lámina completa exportada · $members emisiones"
        1 -> "Lámina exportada, pero una foto no llegó a cargar"
        else -> "Lámina exportada, pero $absent fotos no llegaron a cargar"
    }
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
): List<Pair<String, String>> {
    return buildList {
        add("Progreso" to "$ownedMembers / ${catalog.members.size} emisiones")
        addAll(variantEntries(catalog.weightMillioz, catalog.finish))
        common.numistaTypeId?.let { typeId -> add("Tipo" to "Numista $typeId") }
        common.year?.let { year -> add("Año" to year.toString()) }
        add("Actualizado" to catalog.updatedAt)
    }
}
