package com.jenarvaezg.coindex.domain

/** Troy ounce in grams, as used by Numista's `weight` field. */
const val GRAMS_PER_TROY_OUNCE: Double = 31.1034768

fun gramsToOunces(grams: Double): Double = grams / GRAMS_PER_TROY_OUNCE

private val COMMON_WEIGHTS_MILLIOZ = intArrayOf(250, 500, 1_000, 2_000, 5_000, 10_000)

/** How far a measured weight may sit from a snapping target and still be it. */
private const val SNAP_TOLERANCE_MILLIOZ = 10

/**
 * Normalizes a weight in ounces to milli-ounces, snapping to the common bullion weights
 * when within 10 milli-ounces. 31.1 g becomes exactly 1000; 30 g stays at 965 so that a
 * near-ounce piece is never conflated with a true ounce. The nearest target wins and the
 * smaller one breaks a tie, which no two common weights can currently produce: they sit at
 * least 250 apart and the tolerance is 10, so at most one is ever in range. The comparator
 * stays anyway, so that adding a common weight is a one-line change and not a silent
 * dependence on declaration order.
 *
 * The bullion weights are the only targets, because they are the only convention a coin can
 * belong to without anybody saying so. A weight a curated catalog declares is authority over
 * **its own members** (ADR 0016), and those never reach this function: their key comes from
 * the file. ADR 0012 also made declared weights global targets, back when every piece passed
 * through here — and once the catalog branch took its members out, all that reach was left
 * pointing at types no curator had ever weighed. It decided the variant of twenty-two of
 * them in silence, and got the Morgan dollar's true 26.73 g wrong (#288), so it is gone.
 */
fun normalizeWeightMillioz(weightOz: Double): Int? {
    if (!weightOz.isFinite() || weightOz <= 0.0) return null
    val measured = Math.round(weightOz * 1_000.0).toInt()
    if (measured <= 0) return null
    return COMMON_WEIGHTS_MILLIOZ
        .filter { target -> Math.abs(measured - target) <= SNAP_TOLERANCE_MILLIOZ }
        .minWithOrNull(compareBy({ target -> Math.abs(measured - target) }, { target -> target }))
        ?: measured
}

/**
 * A variant weight written out in troy ounces: `1000` reads «1 oz», `386` reads «0,386 oz».
 *
 * It lives here and not in `Labels.kt` because it is arithmetic and not copy — the variant key of
 * ADR 0018 said in its own unit — and because two owners of that arithmetic is how «0,386 oz» on a
 * card and «0,39 oz» in a name begin to disagree about one coin. `weightLabel` is the
 * collector-facing reading built on top of it and keeps the one sentence there is to word: the set
 * that spans denominations and has no single weight to show (ADR 0012).
 */
fun ounceLabel(weightMillioz: Int): String {
    val whole = weightMillioz / 1_000
    val fraction = (weightMillioz % 1_000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole oz" else "$whole,$fraction oz"
}
