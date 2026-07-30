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
 * near-ounce piece is never conflated with a true ounce.
 *
 * [curatedTargets] adds the weights declared by seeded collection catalogs (ADR 0012), so a
 * catalog's own figure wins over the arbitrary gram value Numista happens to record for one
 * member: the 13.96 g Porto 500 escudos joins its 14 g siblings at 450 instead of splitting
 * off at 449. The nearest target wins, and the smaller one breaks a tie.
 */
fun normalizeWeightMillioz(weightOz: Double, curatedTargets: Set<Int> = emptySet()): Int? {
    if (!weightOz.isFinite() || weightOz <= 0.0) return null
    val measured = Math.round(weightOz * 1_000.0).toInt()
    if (measured <= 0) return null
    val targets = COMMON_WEIGHTS_MILLIOZ.toMutableList().apply { addAll(curatedTargets) }
    return targets
        .filter { target -> Math.abs(measured - target) <= SNAP_TOLERANCE_MILLIOZ }
        .minWithOrNull(compareBy({ target -> Math.abs(measured - target) }, { target -> target }))
        ?: measured
}
