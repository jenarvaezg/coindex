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
