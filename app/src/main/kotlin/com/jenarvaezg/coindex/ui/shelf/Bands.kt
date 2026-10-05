package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.GRAMS_PER_TROY_OUNCE
import com.jenarvaezg.coindex.ui.UNKNOWN_YEAR_LABEL

// Every band here is total: each has a member for rows without a value, so every row is reachable
// by some chip of every facet. For all bands but the years, chip counts also add up to the facet
// total. Years are the exception (#448): a coin held in three years is counted by three chips, each
// once, and the facet total still counts coins.

/**
 * A coin's weight band in grams, as Numista records it (the index's collection facet uses ounces).
 * [Unweighed] fills only between a sync and its fichas arriving, since every seeded type has a
 * weight. Upper bounds are inclusive, as the labels read: 25 g is «10 – 25 g».
 */
enum class GramBand(val label: String, private val upToGrams: Double) {
    UnderTen("Menos de 10 g", 10.0),
    TenToTwentyFive("10 – 25 g", 25.0),
    Ounce("Una onza (25 – 34 g)", 34.0),
    OverThirtyFour("Más de 34 g", Double.MAX_VALUE),
    Unweighed("Sin peso", Double.NaN),
    ;

    companion object {
        /** The band for a weight in troy ounces; a missing weight is [Unweighed]. */
        fun of(weightOz: Double?): GramBand {
            val grams = weightOz?.takeIf { it.isFinite() && it > 0.0 }
                ?.let { it * GRAMS_PER_TROY_OUNCE }
                ?: return Unweighed
            return entries.first { band -> grams <= band.upToGrams }
        }
    }
}

/**
 * The Monedas year facet: an exact year, or «Sin año». Exact so that a seat on the notebook's year
 * axis can open Monedas on that year.
 *
 * [Undated] stays even when no cached ficha needs it: Numista may have no issue for a piece, a type
 * may await review, or a ficha may not have been fetched yet.
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
        /** Every chip a coin matches: one per year it holds, or «Sin año» (#448). */
        fun of(years: List<Int>): List<YearFilter> =
            years.map(::Of).ifEmpty { listOf(Undated) }
    }
}

/**
 * A collection's weight band, in the ounces its card prints (ADR 0018).
 *
 * [Spanning] is a set catalog or a box covering several physical variants on purpose (ADR 0012,
 * ADR 0021 §11), not an unknown weight. Labelled «Varias onzas» so the chip answers the same
 * question as its neighbours and doesn't name the kind of collection (#516, ADR 0021 §2).
 */
enum class OunceBand(val label: String) {
    UnderHalf("Menos de ½ oz"),
    HalfToOne("½ – 1 oz"),
    OverOne("Más de 1 oz"),
    Spanning("Varias onzas"),
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
 * The era a collection starts in, from the earliest coin the collector owns of it, not the
 * catalog's first year. [Unknown] means no cached ficha yet.
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
