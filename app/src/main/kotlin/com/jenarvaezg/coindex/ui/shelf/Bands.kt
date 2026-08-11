package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.GRAMS_PER_TROY_OUNCE
import com.jenarvaezg.coindex.ui.UNKNOWN_YEAR_LABEL

// Every band in this file is **total**: each has a member for the rows that have no value, so no row
// of a facet is unreachable. That is not tidiness — a row reachable by no chip of a facet is a row the
// collector can only find by *not* using that facet, and nothing on screen would say so. The uncached
// type, the coin nobody weighed and the box of types no ficha has arrived for are exactly the rows
// worth reaching.
//
// Totality is «no row without a chip», and for every band here except the years it also means the
// per-chip counts add up to the facet's total. The years are the exception since #448: a coin held in
// three years is counted by three chips, because each of them has to find it. A chip still never
// counts a row twice, and the facet's total is still the number of *coins*.

/**
 * The weight of a coin, in the grams Numista records rather than in the ounces a card prints.
 *
 * Grams because this facet is about a *piece* and not about a variant: the collector reading Coins is
 * holding the thing, and «una onza» is the answer the ounce bands of the index already give. `weight`
 * covers every seeded type, so [Unweighed] is empty on a synced phone and fills only
 * between a sync landing and the fichas arriving.
 *
 * The upper bound of each band is inclusive, which is how its label reads: 25 g — the module of the
 * Venezuelan fuertes — belongs to «10 – 25 g» and not to the ounce.
 */
enum class GramBand(val label: String, private val upToGrams: Double) {
    UnderTen("Menos de 10 g", 10.0),
    TenToTwentyFive("10 – 25 g", 25.0),
    Ounce("Una onza (25 – 34 g)", 34.0),
    OverThirtyFour("Más de 34 g", Double.MAX_VALUE),
    Unweighed("Sin peso", Double.NaN),
    ;

    companion object {
        /** Where a weight in troy ounces falls. Nobody's weight is [Unweighed], never the smallest. */
        fun of(weightOz: Double?): GramBand {
            val grams = weightOz?.takeIf { it.isFinite() && it > 0.0 }
                ?.let { it * GRAMS_PER_TROY_OUNCE }
                ?: return Unweighed
            return entries.first { band -> grams <= band.upToGrams }
        }
    }
}

/**
 * The year facet of Coins: one year on the coin, or «Sin año».
 *
 * Exact years, not eras — the year axis of the notebook is a calendar of seats, and tapping one has
 * to open Monedas on that year, not on a band that swallows decades beside it. [Undated] stays a
 * chip of its own: the two types the seeded cache has no year for are the unpublished submissions
 * of #186, and dropping them would hide the pieces waiting on a referee.
 */
sealed interface YearFilter {
    val label: String

    data class Of(val year: Int) : YearFilter {
        override val label: String get() = year.toString()
    }

    data object Undated : YearFilter {
        override val label: String get() = UNKNOWN_YEAR_LABEL
    }

    companion object {
        /**
         * Every chip one coin answers to: one per year it holds, or «Sin año» (#448).
         *
         * A list because a coin is one card however many years of it the collector has, and each of
         * those years is a chip that has to find it. It is the only reading of [YearFilter] there is:
         * the row is what is being filtered, and the row has years in the plural.
         */
        fun of(years: List<Int>): List<YearFilter> =
            years.map(::Of).ifEmpty { listOf(Undated) }
    }
}

/**
 * The weight of a *collection*, in the ounces its card prints (ADR 0018).
 *
 * [Spanning] is not «unknown»: a set catalog and a collector's box both cover several physical
 * variants on purpose (ADR 0012, ADR 0021 §11), so they have one weight less than the others rather
 * than one weight missing. Giving them a chip of their own is what keeps them reachable from a shelf
 * that would otherwise only ever answer about ounces.
 */
enum class OunceBand(val label: String) {
    UnderHalf("Menos de ½ oz"),
    HalfToOne("½ – 1 oz"),
    OverOne("Más de 1 oz"),
    Spanning("Conjunto o caja"),
    ;

    companion object {
        fun of(weightMillioz: Int?): OunceBand = when {
            weightMillioz == null -> Spanning
            weightMillioz < 500 -> UnderHalf
            weightMillioz <= 1_000 -> HalfToOne
            else -> OverOne
        }
    }
}

/**
 * The era a collection starts in, read off the earliest coin the collector owns of it.
 *
 * Deliberately measured and not declared: a catalog's first member year would answer about the
 * curation, and this facet is asked while looking at an index of what is in the house. [Unknown] is
 * a collection whose pieces have no cached ficha yet — one sync away from having a date, and no
 * reason to be unreachable in the meantime.
 */
enum class StartBand(val label: String, private val upToYear: Int) {
    BeforeFifty("Antes de 1950", 1_949),
    FiftyToNinetyNine("1950 – 1999", 1_999),
    SinceTwoThousand("Desde 2000", Int.MAX_VALUE),
    Unknown("Sin fecha", Int.MIN_VALUE),
    ;

    companion object {
        fun of(year: Int?): StartBand =
            year?.let { entries.first { band -> it <= band.upToYear } } ?: Unknown
    }
}
