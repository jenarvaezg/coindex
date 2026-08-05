package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.GRAMS_PER_TROY_OUNCE

/**
 * Every band of every facet is **total**: each has a member for the rows that have no value, so the
 * per-chip counts always add up to the facet's own total.
 *
 * That is not tidiness. A row reachable by no chip of a facet is a row the collector can only find by
 * *not* using that facet, and there is nothing on screen that would say so — the counts would simply
 * fall short of the tally and nobody would notice. The uncached type, the coin nobody weighed and the
 * box of types no ficha has arrived for are exactly the rows worth reaching.
 */

/**
 * The weight of a coin, in the grams Numista records rather than in the ounces a card prints.
 *
 * Grams because this facet is about a *piece* and not about a variant: the collector reading Coins is
 * holding the thing, and «una onza» is the answer the ounce bands of the index already give. `weight`
 * covers 100 % of the 829 seeded types, so [Unweighed] is empty on a synced phone and fills only
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
 * When a coin is from, as the four eras the two collections actually span plus the undated.
 *
 * [Undated] is a chip and not an omission: the two types the seeded cache has no year for are the two
 * unpublished submissions of #186, and a facet that dropped them would hide from the collector exactly
 * the pieces waiting on a referee.
 */
enum class YearBand(val label: String, private val upToYear: Int) {
    BeforeNineteenHundred("Antes de 1900", 1_899),
    ThroughTheSeventies("1900 – 1979", 1_979),
    EightiesAndNineties("1980 – 1999", 1_999),
    SinceTwoThousand("Desde 2000", Int.MAX_VALUE),
    Undated("Sin año", Int.MIN_VALUE),
    ;

    companion object {
        fun of(year: Int?): YearBand =
            year?.let { entries.first { band -> it <= band.upToYear } } ?: Undated
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
