package com.jenarvaezg.coindex.domain

/**
 * One magnitude of the collection, with how much of the collection it was measured over.
 *
 * The coverage travels with the number because one of these is **extrapolated and the others are
 * not**: `thickness` is missing in a third of the types, so the stack is measured over the pieces
 * that have one and scaled to all of them, while the weight and the row are measured over 99 %
 * (`docs/ux/cifras-316.md`). A figure that had dropped its denominator would make those two look
 * like the same kind of claim.
 *
 * @param measuredPieces how many pieces carried the datum.
 * @param pieces how many there are in total.
 */
data class Magnitude(val value: Double, val measuredPieces: Int, val pieces: Int) {
    val complete: Boolean get() = measuredPieces == pieces

    /**
     * The magnitude the whole collection would have if the unmeasured pieces were like the measured
     * ones.
     *
     * The one figure the app gives extrapolated, and it says so: «unos 94 cm». Zero measured pieces
     * extrapolate to nothing rather than to zero.
     */
    val extrapolated: Double?
        get() = when {
            measuredPieces <= 0 -> null
            complete -> value
            else -> value * pieces / measuredPieces
        }
}

/** The mass of one metal in the collection, and what share of the measured mass it is. */
data class MetalMass(val metal: Metal, val grams: Double)

/**
 * How the collection's mass divides between metals, **by mass and never by coin**.
 *
 * By coin it is a bar of one colour — 565 of his 574 pieces are silver — and by mass it says that
 * almost a kilo of the collection is not silver, because a .835 coin is 16,5 % copper
 * (`docs/ux/cifras-326.md`). That is the whole content of the figure.
 *
 * Three rules, and they are what make it grow on its own the day another metal arrives:
 *
 * - A piece of a precious metal contributes its **fine** mass to that metal, and the remainder of its
 *   alloy to copper — which is what Numista's own texts say the rest is: «Plata 835 (Copper .165)»,
 *   «Plata 925 (92.5 % silver, 7.5 % copper)».
 * - A piece of a **copper alloy** — copper, bronze, brass, cupronickel — contributes its whole mass
 *   to copper, which is what it mostly is. Every other base metal contributes to itself.
 * - A piece with **no dominant metal** (bimetallic, clad) or with a composition these rules do not
 *   read contributes to no metal at all. It is left out of [measuredGrams] rather than guessed at,
 *   which is 0,1 % of his collection.
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
                // No fineness declared is not «pure»: the whole mass goes to the metal named, because
                // that is all the ficha supports, and nothing is credited to copper it did not say.
                val fineness = meta.fineness ?: 1.0
                val fine = mass * fineness
                masses.merge(metal, fine, Double::plus)
                // The remainder and not `mass × (1 − fineness)`: the two are the same number and only
                // one of them is 16,5 rather than 16,500000000000004 on the label.
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
        // Heaviest first, and on a tie the precious metal: two metals of the same mass would otherwise
        // swap places between launches with the order of the inventory.
        masses = masses.map { (metal, grams) -> MetalMass(metal, grams) }
            .sortedWith(compareByDescending<MetalMass> { it.grams }.thenBy { metalOrder(it.metal) }),
        measuredGrams = measured,
        grams = total,
    )
}

/**
 * The oldest and the newest piece, and the years between them.
 *
 * The **Gregorian** year of each piece (ADR 0026 §9), which is what makes this an arc and not a
 * nonsense: two of his pieces are dated in Hijri years — 1316 and 1375 — and read literally they
 * stretched the axis to 711 years.
 *
 * And the 23 pieces that carry **no year at all** inherit their type's minimum. Without that rule the
 * arc is 246 years instead of 1.756: the undated Portuguese escudos and the Roman denarius are exactly
 * the pieces that make it long (#326).
 */
data class YearArc(val oldest: Int, val newest: Int) {
    val years: Int get() = newest - oldest
}

fun yearArc(items: List<CollectedItem>, typeMeta: TypeMetaIndex): YearArc? {
    val years = items.mapNotNull { item -> placementYear(item, typeMeta[item.typeId]) }
    val oldest = years.minOrNull() ?: return null
    return YearArc(oldest, years.max())
}

/** One piece drawn at its real diameter, which is what the size block is: a drawing, not a figure. */
data class DiameterExtreme(val item: CollectedItem, val meta: TypeMeta, val millimetres: Double)

/**
 * The smallest and the largest coin of the collection, to be drawn at the same scale.
 *
 * **On a tie, the older piece**, and that tie is not hypothetical: four of his pieces are 42 mm, and
 * what the block is worth showing is the Maria Theresa thaler of 1780 rather than whichever 42 mm coin
 * the inventory happened to list last. `size` is present in 100 % of the types, so this costs nothing
 * (#326).
 *
 * Null when fewer than two pieces have a diameter: one coin drawn against itself is not a comparison.
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
 * One of the four figures «al margen»: a count of pieces, out of a total, with something to open.
 *
 * Nobody asked for these. They come out of looking for what else the ficha already in the APK says,
 * and they are the reason the page is a field guide and not a dashboard: 75 % of his coins are no
 * longer money anywhere, 246 were engraved by the same hand, 296 came out of Paris, 210 are dated 1960.
 *
 * @param pieces how many pieces the figure counts.
 * @param subject the name the figure is about — a hand, a mint, a year — or null where it has none.
 */
data class MarginFigure(val pieces: Int, val outOf: Int, val subject: String? = null)

/**
 * The four figures at the margin, each measured over the whole collection.
 *
 * @param demonetized pieces Numista marks as no longer legal tender. Its denominator is the whole
 *   collection and not the types Numista answered for: a percentage over a moving denominator is a
 *   figure nobody can check.
 * @param sameHand the hand that drew or engraved the most pieces.
 * @param mostMinted the mint that struck the most pieces.
 * @param distinctMints how many mints the collection has come out of, which is what makes the mint
 *   figure say something: 296 from Paris **of 51 mints**.
 * @param commonestYear the year the most pieces carry.
 */
data class MarginFigures(
    val demonetized: MarginFigure,
    val sameHand: MarginFigure?,
    val mostMinted: MarginFigure?,
    val distinctMints: Int,
    val commonestYear: MarginFigure?,
)

fun marginFigures(items: List<CollectedItem>, typeMeta: TypeMetaIndex): MarginFigures {
    var pieces = 0
    var demonetized = 0
    val hands = mutableMapOf<String, Int>()
    val mints = mutableMapOf<String, Int>()
    val years = mutableMapOf<Int, Int>()
    for (item in items) {
        val quantity = item.quantity.coerceAtLeast(1)
        pieces = saturatingAdd(pieces, quantity)
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
    )
}

/**
 * The winner, with the smaller key breaking a tie.
 *
 * Deterministic on purpose: two hands with the same count would otherwise swap places between
 * launches for no reason the collector can see.
 */
private fun <K : Comparable<K>> Map<K, Int>.commonest(): Pair<K, Int>? = entries
    .sortedWith(compareByDescending<Map.Entry<K, Int>> { it.value }.thenBy { it.key })
    .firstOrNull()
    ?.let { (key, count) -> key to count }

/**
 * Everything «Las cifras» draws that comes out of the APK, with not a single call (ADR 0028 §7).
 *
 * The money is deliberately **not** here. It arrives late, it is the only thing on the page that
 * does, and a figure set that carried a nullable total would be the half-done total this page exists
 * not to show.
 */
data class CollectionFigures(
    val pieces: Int,
    val types: Int,
    val issuers: Int,
    /** Grams. The count the bottom bar's third cell prints, and never money. */
    val weight: Magnitude,
    /** Fine silver in grams, which is the figure spot multiplies and the metal bar's first bar. */
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

/** How many A4 sheets the collection spread out would cover, which is what 0,35 m² means. */
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

/**
 * One running total and how many pieces have gone into it.
 *
 * Five of these instead of five pairs of local variables, because every one of them has the same
 * denominator problem and only one of them is allowed to be extrapolated.
 */
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
