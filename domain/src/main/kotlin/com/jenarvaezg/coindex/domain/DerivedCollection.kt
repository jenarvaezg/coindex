package com.jenarvaezg.coindex.domain

/**
 * Persisted stand-in for an absent weight (ADR 0012). Deliberately not zero: a truncated or
 * defaulted row stays an invalid weight and is ignored rather than read as a set.
 */
const val SPANNING_VARIANTS_WEIGHT: Int = -1

/**
 * The exact canonical tuple that identifies a physical variant; derived collections and curated
 * catalogs both key off it. It persists nothing (ADR 0021 §5): it is the identity only of cards no
 * file names, and of the route that opens one.
 *
 * A null [weightMillioz] means a set issued as a set, whose members span physical variants (ADR
 * 0012). A null [metal] only means Numista recorded no readable composition (#40, ADR 0018).
 */
data class VariantKey(
    val family: String,
    val weightMillioz: Int?,
    val finish: Finish?,
    val metal: Metal?,
) {
    fun finishCode(): String = finishCode(finish)

    fun metalCode(): String = metalCode(metal)

    /** The weight as persisted, mapping the absent weight to its sentinel. */
    fun storedWeightMillioz(): Int = weightMillioz ?: SPANNING_VARIANTS_WEIGHT

    companion object {
        /**
         * Rebuilds a key from its parts, rejecting anything that is not already canonical: an
         * unnormalized family, an out-of-range weight or an unknown finish or metal code. Used when
         * reading back the route of a card no file names.
         */
        fun fromCanonicalParts(
            family: String,
            weightMillioz: Int,
            finishCode: String,
            metalCode: String,
        ): VariantKey? {
            val normalized = normalizeFamily(family) ?: return null
            val spanning = weightMillioz == SPANNING_VARIANTS_WEIGHT
            if (normalized != family || family.length > 256) return null
            if (!spanning && weightMillioz !in 1..1_000_000) return null
            val parsed = finishFromCode(finishCode) ?: return null
            val parsedMetal = metalFromCode(metalCode) ?: return null
            // A set spans finishes and metals as well as weights, so it carries none of them.
            if (spanning && (parsed.finish != null || parsedMetal.metal != null)) return null
            return VariantKey(
                normalized,
                weightMillioz.takeUnless { spanning },
                parsed.finish,
                parsedMetal.metal,
            )
        }
    }
}

/**
 * A provisional grouping of currently owned pieces sharing one exact resolved family and
 * physical variant. A selected catalog declares the complete key; without one, the remaining
 * precedence ladder resolves it. It never claims catalog coverage or reports a gap.
 */
data class DerivedCollection(
    val family: String,
    val weightMillioz: Int?,
    val finish: Finish?,
    val metal: Metal?,
    val distinctTypes: Int,
    val quantity: Int,
) {
    fun key(): VariantKey = VariantKey(family, weightMillioz, finish, metal)
}
