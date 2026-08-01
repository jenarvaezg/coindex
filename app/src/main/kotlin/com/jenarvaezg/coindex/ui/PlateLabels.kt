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

fun plateCommonFacts(members: List<CollectionCatalogMember>): PlateCommonFacts {
    // Read off the issued members alone: an announced one has neither type nor, often, a year,
    // and letting its blanks in would push a shared fact back down into every cell.
    val issued = members.filterNot { it.isAnnounced }
    return PlateCommonFacts(
        numistaTypeId = issued.mapNotNull { it.numistaTypeId }.distinct().singleOrNull(),
        year = issued.mapNotNull { it.year }.distinct().singleOrNull(),
    )
}

/**
 * What one cell has left to say under its own title: the year, unless the title is already the
 * year or every cell shares it, and the Numista type, unless the whole plate is one type.
 *
 * Null when nothing is left, which is the common case of a date run.
 */
fun plateCellFootnote(member: CollectionCatalogMember, common: PlateCommonFacts): String? {
    val parts = buildList {
        val year = member.year
        if (common.year == null && year != null && member.label != year.toString()) {
            add(year.toString())
        }
        if (common.numistaTypeId == null) {
            member.numistaTypeId?.let { typeId -> add("Numista $typeId") }
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
        // «Casillas» and not «emisiones»: a plate can draw a slot the mint has not struck, and
        // the progress line right above it counts only what was.
        0 -> "Lámina completa exportada · $members casillas"
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
    val announced = catalog.members.count { it.isAnnounced }
    return buildList {
        // Only what was struck is in the divisor (#31): an announced slot no money can buy would
        // publish a «me falta» that is nobody's hole to fill.
        add("Progreso" to "$ownedMembers / ${catalog.members.size - announced} emisiones")
        if (announced > 0) {
            add("Sin emitir" to if (announced == 1) "1 anunciada" else "$announced anunciadas")
        }
        addAll(variantEntries(catalog.weightMillioz, catalog.finish))
        common.numistaTypeId?.let { typeId -> add("Tipo" to "Numista $typeId") }
        common.year?.let { year -> add("Año" to year.toString()) }
        add("Actualizado" to catalog.updatedAt)
    }
}
