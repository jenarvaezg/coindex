package com.jenarvaezg.coindex.domain

/**
 * Persisted stand-in for an absent weight (ADR 0012). Deliberately not zero: a truncated or
 * defaulted row stays an invalid weight and is ignored rather than read as a set.
 */
const val SPANNING_VARIANTS_WEIGHT: Int = -1

/**
 * The exact canonical tuple that identifies a physical variant. Derived collections, per-user
 * dispositions and curated catalogs all key off this. Editorial family aliases never alter
 * it.
 *
 * A null [weightMillioz] means the family is a set issued as a set, whose members span
 * physical variants, so no single weight identifies it (ADR 0012).
 *
 * [metal] is the fourth component (#40, ADR 0018) and the only one that can be absent without
 * meaning anything special: a type Numista records no composition for is simply unread.
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
         * Rebuilds a key from persisted parts, rejecting anything that is not already
         * canonical: an unnormalized family, an out-of-range weight or an unknown finish
         * or metal code. Used when reading back a stored disposition.
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

/** The collector's durable intent about one variant key (ADR 0008). */
enum class DerivedCollectionDisposition {
    Followed,
    Ignored,
    ;

    fun asCode(): String = when (this) {
        Followed -> "followed"
        Ignored -> "ignored"
    }

    companion object {
        fun fromCode(code: String): DerivedCollectionDisposition? = when (code) {
            "followed" -> Followed
            "ignored" -> Ignored
            else -> null
        }
    }
}

data class DerivedCollectionPreference(
    val key: VariantKey,
    val disposition: DerivedCollectionDisposition,
)

data class ClassifiedDerivedCollections(
    val followed: List<DerivedCollection> = emptyList(),
    val available: List<DerivedCollection> = emptyList(),
    val ignored: List<DerivedCollection> = emptyList(),
) {
    val isEmpty: Boolean get() = followed.isEmpty() && available.isEmpty() && ignored.isEmpty()
}

/**
 * Splits current derived collections by the collector's stored dispositions. A preference with no
 * current evidence stays dormant: it never materializes a derived collection.
 */
fun classifyDerivedCollections(
    derivedCollections: List<DerivedCollection>,
    preferences: List<DerivedCollectionPreference>,
): ClassifiedDerivedCollections {
    val byKey = preferences.associate { preference -> preference.key to preference.disposition }
    val followed = mutableListOf<DerivedCollection>()
    val available = mutableListOf<DerivedCollection>()
    val ignored = mutableListOf<DerivedCollection>()
    for (collection in derivedCollections) {
        when (byKey[collection.key()]) {
            DerivedCollectionDisposition.Followed -> followed += collection
            DerivedCollectionDisposition.Ignored -> ignored += collection
            null -> available += collection
        }
    }
    return ClassifiedDerivedCollections(followed, available, ignored)
}
