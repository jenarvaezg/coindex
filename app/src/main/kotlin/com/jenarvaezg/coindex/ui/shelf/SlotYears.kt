package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState

/**
 * The years named by casillas of evidenced plates, from one walk (#550). [years] is where the year
 * axis paints holes; [of] maps a type to its casilla years so tapping such a hole opens Monedas on
 * the versions of that type the collector owns. One walk, so the axis and the shelf agree.
 */
class SlotYears internal constructor(
    val years: Set<Int>,
    private val byType: Map<Int, Set<Int>>,
) {
    /** The years of this type's casillas; empty when no evidenced plate names it. */
    fun of(typeId: Int): Set<Int> = byType[typeId].orEmpty()

    companion object {
        /** No plates walked, e.g. for the coin sheet. */
        val none = SlotYears(emptySet(), emptyMap())
    }
}

/**
 * Groups the casillas the assembly already resolved (#538). Casillas without a year are skipped,
 * and those without a type don't map to a coin.
 *
 * @param keptCatalogIds the plates that survive the shelf, or null for every evidenced one.
 */
fun slotYears(
    state: CollectionState,
    keptCatalogIds: Set<String>? = null,
): SlotYears {
    val years = linkedSetOf<Int>()
    val byType = linkedMapOf<Int, MutableSet<Int>>()
    for (slot in state.slots) {
        if (keptCatalogIds != null && slot.catalogId !in keptCatalogIds) continue
        val year = slot.year ?: continue
        years.add(year)
        val typeId = slot.typeId ?: continue
        byType.getOrPut(typeId) { linkedSetOf() }.add(year)
    }
    return SlotYears(years, byType)
}
