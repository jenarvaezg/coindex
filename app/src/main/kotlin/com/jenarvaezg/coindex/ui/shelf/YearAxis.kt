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
 *
 * **The range is dated pieces and slots, not undated inheritances.** The type minimum (#326) still
 * places an undated piece when it falls inside that range (Portuguese escudos). Letting a Roman
 * denarius of year 270 open the axis would paint 1,700 years of bare cardboard before the first
 * Thaler — the opposite of the 1.62-screen sheet atlas-315 measured. The 1,756-year figure belongs
 * to «Las cifras»'s arco, not to this sheet.
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
    val datedYears = sortedSetOf<Int>()
    for (piece in state.items) {
        if (piece.quantity <= 0) continue
        if (keptItemIds != null && piece.id !in keptItemIds) continue
        val meta = state.typeMeta[piece.typeId]
        val dated = listOfNotNull(piece.gregorianYear, piece.recordedYear).firstOrNull { it > 0 }
        val year = placementYear(piece, meta) ?: continue
        if (dated != null) datedYears.add(dated)
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
            if (year > 0) slotYears.add(year)
        }
    }

    val rangeYears = (datedYears + slotYears).ifEmpty {
        // A collection of only undated pieces still deserves a sheet: fall back to inherited years.
        ownedByYear.keys.filter { it > 0 }.toSet()
    }
    if (rangeYears.isEmpty()) {
        return YearAxisModel(cells = emptyList(), ownedYears = 0, totalYears = 0)
    }
    val first = rangeYears.min()
    val last = rangeYears.max()
    // Undated pieces whose inherited year falls outside the dated/slot span stay off this sheet;
    // the country axis still shows them.
    val ownedInRange = ownedByYear.filterKeys { it in first..last }
    val cells = (first..last).map { year ->
        val owned = ownedInRange[year]
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
    year < 0 -> year / 100 // e.g. -50 → century 0 in the BC labelling branch
    else -> 1 // year 0 does not occur after [placementYear]; keep labels unique if it did
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
