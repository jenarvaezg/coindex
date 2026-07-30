package com.jenarvaezg.coindex.domain

/** Troy ounce in grams, as used by Numista's `weight` field. */
const val GRAMS_PER_TROY_OUNCE: Double = 31.1034768

fun gramsToOunces(grams: Double): Double = grams / GRAMS_PER_TROY_OUNCE

private val COMMON_WEIGHTS_MILLIOZ = intArrayOf(250, 500, 1_000, 2_000, 5_000, 10_000)

/**
 * Normalizes a weight in ounces to milli-ounces, snapping to the common bullion weights
 * when within 10 milli-ounces. 31.1 g becomes exactly 1000; 30 g stays at 965 so that a
 * near-ounce piece is never conflated with a true ounce.
 */
fun normalizeWeightMillioz(weightOz: Double): Int? {
    if (!weightOz.isFinite() || weightOz <= 0.0) return null
    val measured = Math.round(weightOz * 1_000.0).toInt()
    if (measured <= 0) return null
    return COMMON_WEIGHTS_MILLIOZ.firstOrNull { common ->
        Math.abs(measured - common) <= 10
    } ?: measured
}
