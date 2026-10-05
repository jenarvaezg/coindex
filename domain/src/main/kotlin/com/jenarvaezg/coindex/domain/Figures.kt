package com.jenarvaezg.coindex.domain

/**
 * One magnitude of the collection, with how many pieces it was measured over. The coverage travels
 * with the number because `thickness` is often missing, so the stack is extrapolated while the
 * other magnitudes are near complete (`docs/ux/cifras-316.md`).
 *
 * @param measuredPieces how many pieces carried the datum.
 * @param pieces how many there are in total.
 */
data class Magnitude(val value: Double, val measuredPieces: Int, val pieces: Int) {
    val complete: Boolean get() = measuredPieces == pieces

    /**
     * The magnitude if the unmeasured pieces were like the measured ones; the screen marks it as
     * approximate. Null when nothing was measured.
     */
    val extrapolated: Double?
        get() = when {
            measuredPieces <= 0 -> null
            complete -> value
            else -> value * pieces / measuredPieces
        }
}

/** The mass of one metal in the collection, in grams; [MetalSplit.shareOf] gives its share. */
data class MetalMass(val metal: Metal, val grams: Double)

/**
 * How the collection's mass divides between metals, by mass and never by coin: counted by coin a
 * silver collection is one colour, while by mass the copper in its alloys shows
 * (`docs/ux/cifras-326.md`).
 *
 * - A precious-metal piece gives its fine mass to that metal and the rest of its alloy to copper,
 *   which is what Numista's texts say the rest is («Plata 835 (Copper .165)»).
 * - A copper-alloy piece (copper, bronze, brass, cupronickel) gives its whole mass to copper. Any
 *   other base metal gives it to itself.
 * - A piece with no dominant metal (bimetallic, clad) or an unreadable composition gives to none
 *   and is left out of [measuredGrams].
 */
data class MetalSplit(val masses: List<MetalMass>, val measuredGrams: Double, val grams: Double) {
    fun shareOf(mass: MetalMass): Double =
        if (measuredGrams <= 0.0) 0.0 else mass.grams / measuredGrams
}

private val COPPER_ALLOYS = setOf(Metal.Copper, Metal.Bronze, Metal.Brass, Metal.Cupronickel)

private val PRECIOUS = setOf(Metal.Silver, Metal.Gold, Metal.Platinum, Metal.Palladium)

fun metalSplit(items: List<CollectedItem>, typeMeta: TypeMetaIndex): MetalSplit {
    val masses = mutableMapOf<Metal, Double>()
    var total = 0.0
    var measured = 0.0
    for (item in items) {
        val meta = typeMeta[item.typeId] ?: continue
        val grams = meta.weightGrams ?: continue
        val mass = grams * item.quantity.coerceAtLeast(1)
        total += mass
        val metal = meta.metal
        when {
            metal == null || metal == Metal.Other -> Unit
            metal in PRECIOUS -> {
                // No declared fineness: the whole mass goes to the named metal, since the ficha
                // supports nothing for copper.
                val fineness = meta.fineness ?: 1.0
                val fine = mass * fineness
                masses.merge(metal, fine, Double::plus)
                // Subtracting, not `mass × (1 − fineness)`, avoids labels like 16,500000000000004.
                val alloy = mass - fine
                if (alloy > 0.0) masses.merge(Metal.Copper, alloy, Double::plus)
                measured += mass
            }
            metal in COPPER_ALLOYS -> {
                masses.merge(Metal.Copper, mass, Double::plus)
                measured += mass
            }
            else -> {
                masses.merge(metal, mass, Double::plus)
                measured += mass
            }
        }
    }
    return MetalSplit(
        // Heaviest first, the precious metal on a tie, so the order is stable across launches.
        masses = masses.map { (metal, grams) -> MetalMass(metal, grams) }
            .sortedWith(compareByDescending<MetalMass> { it.grams }.thenBy { metalOrder(it.metal) }),
        measuredGrams = measured,
        grams = total,
    )
}

/**
 * The oldest and the newest piece by [placementYear], and the years between them: Gregorian years,
 * so Hijri dates do not stretch the arc, and undated pieces at their type's earliest year (#326).
 */
data class YearArc(val oldest: Int, val newest: Int) {
    val years: Int get() = newest - oldest
}

fun yearArc(items: List<CollectedItem>, typeMeta: TypeMetaIndex): YearArc? {
    val years = items.mapNotNull { item -> placementYear(item, typeMeta[item.typeId]) }
    val oldest = years.minOrNull() ?: return null
    return YearArc(oldest, years.max())
}

/** One piece and its diameter, to be drawn at real scale. */
data class DiameterExtreme(val item: CollectedItem, val meta: TypeMeta, val millimetres: Double)

/**
 * The smallest and the largest coin of the collection, drawn at the same scale (#326). Ties go to
 * the older piece rather than to inventory order. Null when fewer than two pieces have a diameter.
 */
data class SizeComparison(val smallest: DiameterExtreme, val largest: DiameterExtreme)

fun sizeComparison(items: List<CollectedItem>, typeMeta: TypeMetaIndex): SizeComparison? {
    val measured = items.mapNotNull { item ->
        val meta = typeMeta[item.typeId] ?: return@mapNotNull null
        val size = meta.sizeMillimetres ?: return@mapNotNull null
        DiameterExtreme(item, meta, size)
    }
    if (measured.size < 2) return null
    val byYear = compareBy<DiameterExtreme> { placementYear(it.item, it.meta) ?: Int.MAX_VALUE }
    val smallest = measured.sortedWith(compareBy<DiameterExtreme> { it.millimetres }.then(byYear))
    val largest =
        measured.sortedWith(compareByDescending<DiameterExtreme> { it.millimetres }.then(byYear))
    return SizeComparison(smallest.first(), largest.first())
}

/**
 * One of the four figures «al margen»: a count of pieces out of a total, with something to open.
 *
 * @param pieces how many pieces the figure counts.
 * @param subject the name the figure is about (a hand, a mint, a year), or null where it has none.
 */
data class MarginFigure(val pieces: Int, val outOf: Int, val subject: String? = null)

/**
 * The four figures at the margin, each measured over the whole collection.
 *
 * @param demonetized pieces Numista marks as no longer legal tender, over the whole collection
 *   rather than the types Numista answered for, so the denominator does not move.
 * @param sameHand the hand that drew or engraved the most pieces.
 * @param mostMinted the mint that struck the most pieces.
 * @param distinctMints how many mints the collection comes from, the context for [mostMinted].
 * @param commonestYear the year the most pieces carry.
 * @param uncirculated pieces the collector graded `unc` or `au`, the one figure taken from the
 *   collector's grading rather than a ficha. Null when there are none; an ungraded piece counts
 *   only in the denominator.
 */
data class MarginFigures(
    val demonetized: MarginFigure,
    val sameHand: MarginFigure?,
    val mostMinted: MarginFigure?,
    val distinctMints: Int,
    val commonestYear: MarginFigure?,
    val uncirculated: MarginFigure?,
)

/** Grades of a piece that has not circulated, «sin circular» and «casi»; both are in use (#491). */
private val UNCIRCULATED_GRADES = setOf(UNCIRCULATED, "au")

fun marginFigures(items: List<CollectedItem>, typeMeta: TypeMetaIndex): MarginFigures {
    var pieces = 0
    var demonetized = 0
    var uncirculated = 0
    val hands = mutableMapOf<String, Int>()
    val mints = mutableMapOf<String, Int>()
    val years = mutableMapOf<Int, Int>()
    for (item in items) {
        val quantity = item.quantity.coerceAtLeast(1)
        pieces = saturatingAdd(pieces, quantity)
        // Before the ficha: the grade is the collector's own, so it counts even without one.
        if (item.grade?.lowercase() in UNCIRCULATED_GRADES) {
            uncirculated = saturatingAdd(uncirculated, quantity)
        }
        val meta = typeMeta[item.typeId] ?: continue
        if (meta.demonetized == true) demonetized = saturatingAdd(demonetized, quantity)
        // Distinct within the type: a hand credited on both faces is one hand, and the same mint
        // listed twice is one mint.
        meta.hands.distinct().forEach { hand -> hands.merge(hand, quantity, ::saturatingAdd) }
        meta.mints.distinct().forEach { mint -> mints.merge(mint, quantity, ::saturatingAdd) }
        placementYear(item, meta)?.let { year -> years.merge(year, quantity, ::saturatingAdd) }
    }
    return MarginFigures(
        demonetized = MarginFigure(demonetized, pieces),
        sameHand = hands.commonest()?.let { (hand, count) -> MarginFigure(count, pieces, hand) },
        mostMinted = mints.commonest()?.let { (mint, count) -> MarginFigure(count, pieces, mint) },
        distinctMints = mints.size,
        commonestYear = years.commonest()
            ?.let { (year, count) -> MarginFigure(count, pieces, year.toString()) },
        uncirculated = MarginFigure(uncirculated, pieces).takeIf { uncirculated > 0 },
    )
}

/** The most frequent key; the smaller key breaks a tie, so the winner is stable across launches. */
private fun <K : Comparable<K>> Map<K, Int>.commonest(): Pair<K, Int>? = entries
    .sortedWith(compareByDescending<Map.Entry<K, Int>> { it.value }.thenBy { it.key })
    .firstOrNull()
    ?.let { (key, count) -> key to count }

/**
 * Everything «Las cifras» draws from the APK alone, without any call (ADR 0028 §7). Money is not
 * here: it arrives later, and a nullable total here would invite showing a partial one.
 */
data class CollectionFigures(
    val pieces: Int,
    val types: Int,
    val issuers: Int,
    /** Grams; the bottom bar's third cell. */
    val weight: Magnitude,
    /** Fine silver in grams: what the spot multiplies, and the metal chart's first bar. */
    val fineSilver: Magnitude,
    /** Metres, laid side by side. */
    val row: Magnitude,
    /** Centimetres, stacked. The one extrapolated figure. */
    val stack: Magnitude,
    /** Square metres, spread out. */
    val area: Magnitude,
    val metals: MetalSplit,
    val arc: YearArc?,
    val size: SizeComparison?,
    val margins: MarginFigures,
)

private const val SQUARE_METRES_PER_A4 = 0.06237

/** How many A4 sheets the collection would cover, spread out. */
fun Magnitude.a4Sheets(): Double = value / SQUARE_METRES_PER_A4

fun collectionFigures(items: List<CollectedItem>, typeMeta: TypeMetaIndex): CollectionFigures {
    var pieces = 0
    val accumulator = MagnitudeSums()
    for (item in items) {
        val quantity = item.quantity.coerceAtLeast(1)
        pieces = saturatingAdd(pieces, quantity)
        val meta = typeMeta[item.typeId]
        accumulator.add(meta, quantity)
    }
    return CollectionFigures(
        pieces = pieces,
        types = items.map { it.typeId }.distinct().size,
        issuers = items.mapNotNull { item -> typeMeta[item.typeId]?.issuerCode }.distinct().size,
        weight = accumulator.weight.magnitude(pieces),
        fineSilver = accumulator.fineSilver.magnitude(pieces),
        row = accumulator.row.magnitude(pieces),
        stack = accumulator.stack.magnitude(pieces),
        area = accumulator.area.magnitude(pieces),
        metals = metalSplit(items, typeMeta),
        arc = yearArc(items, typeMeta),
        size = sizeComparison(items, typeMeta),
        margins = marginFigures(items, typeMeta),
    )
}

/** One running total and how many pieces went into it. */
private class MagnitudeSum {
    var value: Double = 0.0
    var measured: Int = 0

    fun add(amount: Double?, quantity: Int) {
        if (amount == null || !amount.isFinite() || amount <= 0.0) return
        value += amount * quantity
        measured = saturatingAdd(measured, quantity)
    }

    fun magnitude(pieces: Int) = Magnitude(value, measured, pieces)
}

private class MagnitudeSums {
    val weight = MagnitudeSum()
    val fineSilver = MagnitudeSum()
    val row = MagnitudeSum()
    val stack = MagnitudeSum()
    val area = MagnitudeSum()

    fun add(meta: TypeMeta?, quantity: Int) {
        weight.add(meta?.weightGrams, quantity)
        fineSilver.add(fineSilverGrams(meta), quantity)
        // Millimetres to metres, and the diameter is the length a coin takes in a row.
        row.add(meta?.sizeMillimetres?.let { it / 1_000.0 }, quantity)
        stack.add(meta?.thicknessMillimetres?.let { it / 10.0 }, quantity)
        area.add(
            meta?.sizeMillimetres?.let { size ->
                val radius = size / 2_000.0
                Math.PI * radius * radius
            },
            quantity,
        )
    }
}
