package com.jenarvaezg.coindex.domain

/** Troy ounce in grams, as used by Numista's `weight` field. */
const val GRAMS_PER_TROY_OUNCE: Double = 31.1034768

fun gramsToOunces(grams: Double): Double = grams / GRAMS_PER_TROY_OUNCE

/**
 * The weights a measured weight snaps to. `internal` so `MatchingDigestTest` can publish them in
 * the matching digest, against which the copy in `scripts/weight-deviations.py` is compared.
 */
internal val COMMON_WEIGHTS_MILLIOZ = intArrayOf(250, 500, 1_000, 2_000, 5_000, 10_000)

/** How far a measured weight may sit from a snapping target and still be it. */
internal const val SNAP_TOLERANCE_MILLIOZ = 10

/**
 * Normalizes a weight in ounces to milli-ounces, snapping to the common bullion weights when within
 * [SNAP_TOLERANCE_MILLIOZ]: 31.1 g becomes exactly 1000, while 30 g stays 965 so a near-ounce piece
 * is never read as an ounce. The nearest target wins and the smaller breaks a tie; no tie is
 * possible today, but the comparator keeps a new target from depending on declaration order.
 *
 * Only bullion weights are targets. A curated catalog's declared weight governs its own members,
 * which never come through here (ADR 0016). Declared weights used to be global targets too (ADR
 * 0012), which misread uncurated types such as the Morgan dollar's 26.73 g; #288 removed that.
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
 * A variant weight in troy ounces: `1000` reads «1 oz», `386` reads «0,386 oz». Here and not in
 * `Labels.kt` because it is arithmetic on the variant key (ADR 0018), kept in one place so a card
 * and a name cannot round differently. `weightLabel` builds the collector-facing wording on top.
 */
fun ounceLabel(weightMillioz: Int): String {
    val whole = weightMillioz / 1_000
    val fraction = (weightMillioz % 1_000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole oz" else "$whole,$fraction oz"
}
