package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState

/**
 * The years the casillas of the evidenced plates name, read once and answered by two surfaces (#550).
 *
 * [years] is what the year axis paints a ghost on; [of] is which coin that ghost is a hole of, so
 * tapping it opens Monedas on a page that has the versions of the type the collector does own. One
 * walk and not two: a plate that names 1901 on the axis and is unknown to the shelf one tap later is
 * exactly the empty page this reading exists to prevent.
 */
class SlotYears internal constructor(
    val years: Set<Int>,
    private val byType: Map<Int, Set<Int>>,
) {
    /** The years some casilla of this type stands on, empty when no evidenced plate names it. */
    fun of(typeId: Int): Set<Int> = byType[typeId].orEmpty()

    companion object {
        /** No plate walked: what a surface with no catalogs to read says (the coin's own sheet). */
        val none = SlotYears(emptySet(), emptyMap())
    }
}

/**
 * The two indexes of one walk over the casillas the assembly resolved.
 *
 * It no longer opens the curated files (#538): which plates have evidence, which of their members
 * are a coin or a hole, and what year a casilla stands on are all decided once in the domain, so
 * this is a `groupBy` and not a second reading of what fills a slot. A casilla the files gave no
 * year is not a seat — nothing can paint a ghost on nowhere — and one with no type answers to no
 * coin, which is why each is skipped where it is missing rather than guessed at here.
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
