package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum

/**
 * Three states of a year on the year axis (ADR 0026 §9) — coin, ghost hole, bare cardboard.
 *
 * The third is what shows sixty-two consecutive empty years without a word: nobody names them and
 * the collector owns nothing there.
 */
sealed interface YearCellState {
    data class Coin(val quantity: Int, val typeId: Int?) : YearCellState
    data object Ghost : YearCellState
    data object Bare : YearCellState
}

data class YearAxisCell(
    val year: Int,
    val state: YearCellState,
)

data class YearAxisDecade(
    val decade: Int,
    val cells: List<YearAxisCell>,
)

data class YearAxisCentury(
    val century: Int,
    val decades: List<YearAxisDecade>,
) {
    val label: String
        get() = when {
            century <= 0 -> "SIGLO ${roman(-century + 1)}"
            else -> "SIGLO ${roman(century)}"
        }
}

data class YearAxisModel(
    val cells: List<YearAxisCell>,
    val ownedYears: Int,
    val totalYears: Int,
) {
    val centuries: List<YearAxisCentury>
        get() = cells
            .groupBy { centuryOf(it.year) }
            .toSortedMap()
            .map { (century, yearCells) ->
                YearAxisCentury(
                    century = century,
                    decades = yearCells
                        .groupBy { it.year / 10 * 10 }
                        .toSortedMap()
                        .map { (decade, decadeCells) ->
                            YearAxisDecade(decade = decade, cells = decadeCells)
                        },
                )
            }
}

/**
 * The year axis of the notebook (ADR 0026 §9).
 *
 * Owned pieces are placed by [placementYear] — Gregorian first — so Hijri engraved years do not
 * stretch the arc. A year a plate names and the collector does not own is a ghost; a year nobody
 * names and nobody owns is bare cardboard.
 */
fun yearAxis(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    /** Catalog ids that survive the shelf, or null for every evidenced catalog. */
    keptCatalogIds: Set<String>? = null,
    /** Owned pieces that survive the shelf (by row id), or null for every piece. */
    keptItemIds: Set<Long>? = null,
): YearAxisModel {
    val ownedByYear = linkedMapOf<Int, MutableList<Pair<Int, Int>>>()
    for (piece in state.items) {
        if (piece.quantity <= 0) continue
        if (keptItemIds != null && piece.id !in keptItemIds) continue
        val year = placementYear(piece, state.typeMeta[piece.typeId]) ?: continue
        ownedByYear.getOrPut(year) { mutableListOf() }
            .add(piece.typeId to piece.quantity)
    }

    val slotYears = linkedSetOf<Int>()
    for (catalog in catalogs) {
        if (catalog.id !in state.evidencedCatalogIds) continue
        if (keptCatalogIds != null && catalog.id !in keptCatalogIds) continue
        val album = buildCollectionCatalogAlbum(catalog, state.items)
        for (albumMember in album.members) {
            val status = albumMember.status
            if (
                status !is CollectionCatalogMemberStatus.Owned &&
                status !is CollectionCatalogMemberStatus.Missing
            ) {
                continue
            }
            val year = axisSlotYear(catalog, albumMember.member.year, state) ?: continue
            slotYears.add(year)
        }
    }

    val years = (ownedByYear.keys + slotYears)
    if (years.isEmpty()) {
        return YearAxisModel(cells = emptyList(), ownedYears = 0, totalYears = 0)
    }
    val first = years.min()
    val last = years.max()
    val cells = (first..last).map { year ->
        val owned = ownedByYear[year]
        val state = when {
            !owned.isNullOrEmpty() -> YearCellState.Coin(
                quantity = owned.sumOf { it.second },
                typeId = owned.first().first,
            )
            year in slotYears -> YearCellState.Ghost
            else -> YearCellState.Bare
        }
        YearAxisCell(year = year, state = state)
    }
    return YearAxisModel(
        cells = cells,
        ownedYears = cells.count { it.state is YearCellState.Coin },
        totalYears = cells.size,
    )
}

/**
 * The year a missing or owned slot paints on the axis.
 *
 * Date-run members carry the year of the casilla. When the collector owns the piece, that year
 * must agree with [placementYear] so a filled Hijri casilla does not leave a ghost beside its
 * Gregorian twin — ownership already covers the year through the inventory walk above, and the
 * slot year here is what paints the ghost for holes. Catalog years in curated files are Gregorian
 * in practice; engraved Hijri years arrive on loose pieces, not on members.
 */
private fun axisSlotYear(
    catalog: CollectionCatalog,
    memberYear: Int?,
    state: CollectionState,
): Int? {
    if (memberYear != null) return memberYear
    // A one-type plate with no year on the member still spans the type's known range when the
    // ficha is here: otherwise undated members would leave no ghost at all.
    val typeId = catalog.members.mapNotNull { it.numistaTypeId }.singleOrNull() ?: return null
    return state.typeMeta[typeId]?.minYear
}

private fun centuryOf(year: Int): Int = when {
    year > 0 -> (year - 1) / 100 + 1
    else -> year / 100
}

private fun roman(n: Int): String {
    val values = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
    )
    var rest = n
    val out = StringBuilder()
    for ((value, glyph) in values) {
        while (rest >= value) {
            out.append(glyph)
            rest -= value
        }
    }
    return out.toString()
}
