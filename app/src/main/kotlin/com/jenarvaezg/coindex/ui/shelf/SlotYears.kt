package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus

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
 * Walks the evidenced plates once, collecting the year of every casilla that is a coin or a hole.
 *
 * `Unlisted` and `NotYetIssued` stay out for the reason the axis leaves them out: neither is a seat
 * the collector can act on — one has no Numista type to answer with, and the other is a coin no
 * money can buy yet.
 *
 * @param keptCatalogIds the plates that survive the shelf, or null for every evidenced one.
 */
fun slotYears(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    keptCatalogIds: Set<String>? = null,
): SlotYears {
    val years = linkedSetOf<Int>()
    val byType = linkedMapOf<Int, MutableSet<Int>>()
    for (catalog in catalogs) {
        if (catalog.id !in state.evidencedCatalogIds) continue
        if (keptCatalogIds != null && catalog.id !in keptCatalogIds) continue
        // The assembly's album (#537) and not one built here: the casilla this reads is the same
        // casilla the plate draws, armed once for every surface that asks.
        val album = state.albums[catalog] ?: continue
        val onlyType = catalog.members.mapNotNull { it.numistaTypeId }.singleOrNull()
        for (albumMember in album.members) {
            val status = albumMember.status
            if (
                status !is CollectionCatalogMemberStatus.Owned &&
                status !is CollectionCatalogMemberStatus.Missing
            ) {
                continue
            }
            val year = slotYear(albumMember.member.year, onlyType, state) ?: continue
            if (year <= 0) continue
            years.add(year)
            val typeId = albumMember.member.numistaTypeId ?: onlyType ?: continue
            byType.getOrPut(typeId) { linkedSetOf() }.add(year)
        }
    }
    return SlotYears(years, byType)
}

/**
 * The year a missing or owned slot stands on.
 *
 * Date-run members carry the year of the casilla. When the collector owns the piece, that year must
 * agree with `placementYear` so a filled Hijri casilla does not leave a ghost beside its Gregorian
 * twin — ownership already covers the year through the inventory walk of the axis, and the slot year
 * here is what paints the ghost for holes. Catalog years in curated files are Gregorian in practice;
 * engraved Hijri years arrive on loose pieces, not on members.
 */
private fun slotYear(memberYear: Int?, onlyType: Int?, state: CollectionState): Int? {
    if (memberYear != null) return memberYear
    // A one-type plate with no year on the member still spans the type's known range when the ficha
    // is here: otherwise undated members would leave no ghost at all.
    return state.typeMeta[onlyType ?: return null]?.minYear
}
