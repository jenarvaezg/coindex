package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.cardCountry
import com.jenarvaezg.coindex.domain.placementYear

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

/**
 * A front sheet for owned pieces whose placement year falls outside the dated/slot calendar.
 *
 * Romans without an engraved year inherit 270 or 316; putting them on the calendar would paint
 * seventeen centuries of bare cardboard before the first Thaler. They open here under their
 * country instead — one island, not N empty centuries (atlas-315 / #340).
 */
data class YearAxisIslandCoin(
    val typeId: Int,
    val quantity: Int,
)

data class YearAxisIsland(
    val title: String,
    val coins: List<YearAxisIslandCoin>,
)

data class YearAxisDecade(
    val decade: Int,
    val cells: List<YearAxisCell>,
)

data class YearAxisCentury(
    val century: Int,
    val decades: List<YearAxisDecade>,
) {
    /** Atlas-315 paints arabic centuries («SIGLO 20»); BC keeps Roman so a minus sign never shows. */
    val label: String
        get() = when {
            century <= 0 -> "SIGLO ${roman(-century + 1)}"
            else -> "SIGLO $century"
        }
}

data class YearAxisModel(
    val cells: List<YearAxisCell>,
    val ownedYears: Int,
    val totalYears: Int,
    val islands: List<YearAxisIsland> = emptyList(),
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
 * places an undated piece when it falls inside that range (Portuguese escudos). An undated Roman
 * whose inherited year falls outside opens a front [YearAxisIsland] under its country — never the
 * 1,756-year calendar that belongs to «Las cifras».
 */
fun yearAxis(
    state: CollectionState,
    /** Catalog ids that survive the shelf, or null for every evidenced catalog. */
    keptCatalogIds: Set<String>? = null,
    /** Owned pieces that survive the shelf (by row id), or null for every piece. */
    keptItemIds: Set<Long>? = null,
): YearAxisModel {
    data class Placement(
        val typeId: Int,
        val quantity: Int,
        val year: Int,
        val dated: Boolean,
        val country: String,
    )

    val placements = mutableListOf<Placement>()
    val datedYears = sortedSetOf<Int>()
    for (piece in state.items) {
        if (piece.quantity <= 0) continue
        if (keptItemIds != null && piece.id !in keptItemIds) continue
        val meta = state.typeMeta[piece.typeId]
        val dated = listOfNotNull(piece.gregorianYear, piece.recordedYear).firstOrNull { it > 0 }
        val year = placementYear(piece, meta) ?: continue
        if (dated != null) datedYears.add(dated)
        val country = cardCountry(meta?.issuerCode, meta?.issuerName)
            ?: meta?.issuerName
            ?: "Sin país"
        placements.add(
            Placement(
                typeId = piece.typeId,
                quantity = piece.quantity,
                year = year,
                dated = dated != null,
                country = country,
            ),
        )
    }

    // The same walk the shelf of Monedas answers with (#550): the ghost this paints and the coin
    // that ghost opens come from one reading, so a seat cannot lead to a page that knows nothing
    // about the plate that drew it. It reads the casillas of the assembly (#538) like everyone else.
    val slotYears = slotYears(state, keptCatalogIds).years

    val rangeYears = (datedYears + slotYears).ifEmpty {
        // Only inherited years and no slots: the calendar is those years (no island needed).
        placements.map { it.year }.filter { it > 0 }.toSet()
    }
    if (rangeYears.isEmpty()) {
        return YearAxisModel(cells = emptyList(), ownedYears = 0, totalYears = 0)
    }
    val first = rangeYears.min()
    val last = rangeYears.max()

    val ownedByYear = linkedMapOf<Int, MutableList<Pair<Int, Int>>>()
    val islandPlacements = mutableListOf<Placement>()
    for (placement in placements) {
        if (placement.year in first..last) {
            ownedByYear.getOrPut(placement.year) { mutableListOf() }
                .add(placement.typeId to placement.quantity)
        } else {
            islandPlacements.add(placement)
        }
    }

    val cells = (first..last).map { year ->
        val owned = ownedByYear[year]
        val cellState = when {
            !owned.isNullOrEmpty() -> YearCellState.Coin(
                quantity = owned.sumOf { it.second },
                typeId = owned.first().first,
            )
            year in slotYears -> YearCellState.Ghost
            else -> YearCellState.Bare
        }
        YearAxisCell(year = year, state = cellState)
    }

    val islands = islandPlacements
        .groupBy { it.country }
        .entries
        .sortedBy { (_, coins) -> coins.minOf { it.year } }
        .map { (country, coins) ->
            YearAxisIsland(
                title = country,
                coins = coins
                    .groupBy { it.typeId }
                    .map { (typeId, group) ->
                        YearAxisIslandCoin(
                            typeId = typeId,
                            quantity = group.sumOf { it.quantity },
                        )
                    },
            )
        }

    return YearAxisModel(
        cells = cells,
        ownedYears = cells.count { it.state is YearCellState.Coin },
        totalYears = cells.size,
        islands = islands,
    )
}

/**
 * The century a year belongs to, by the decade convention: «Siglo 20» is 1900-1999 (#407).
 *
 * Strict centuries (1801-1900) split the round hundred across two headers, so 1900 closed a row
 * labelled «1900» under «Siglo 19» holding that year alone while «Siglo 20» opened with another
 * «1900» holding 1901-1909 — two rows under one label, each with nine dead seats. The notebook
 * reads by decade rows, so the row keeps its ten seats and the century takes the impurity.
 *
 * The BC branch is defensive: [yearAxis] only ever builds cells for years above zero.
 */
private fun centuryOf(year: Int): Int = when {
    year > 0 -> year / 100 + 1
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
