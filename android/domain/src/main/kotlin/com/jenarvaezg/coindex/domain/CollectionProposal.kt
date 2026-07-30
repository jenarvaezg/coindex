package com.jenarvaezg.coindex.domain

/**
 * The exact canonical tuple that identifies a physical variant. Proposal grouping, per-user
 * dispositions and curated catalogs all key off this. Editorial family aliases never alter
 * it.
 */
data class CollectionProposalKey(
    val family: String,
    val weightMillioz: Int,
    val finish: Finish?,
) {
    fun finishCode(): String = finishCode(finish)

    companion object {
        /**
         * Rebuilds a key from persisted parts, rejecting anything that is not already
         * canonical: an unnormalized family, an out-of-range weight or an unknown finish
         * code. Used when reading back a stored disposition.
         */
        fun fromCanonicalParts(
            family: String,
            weightMillioz: Int,
            finishCode: String,
        ): CollectionProposalKey? {
            val normalized = normalizeFamily(family) ?: return null
            if (normalized != family || family.length > 256 || weightMillioz !in 1..1_000_000) {
                return null
            }
            val parsed = finishFromCode(finishCode) ?: return null
            return CollectionProposalKey(normalized, weightMillioz, parsed.finish)
        }
    }
}

/**
 * A provisional grouping of currently owned pieces sharing one exact Numista family and
 * physical variant. It suggests an organization; it never claims catalog coverage and can
 * never report a missing piece.
 */
data class CollectionProposal(
    val family: String,
    val weightMillioz: Int,
    val finish: Finish?,
    val distinctTypes: Int,
    val quantity: Int,
) {
    fun key(): CollectionProposalKey = CollectionProposalKey(family, weightMillioz, finish)
}

/** The collector's durable intent about one proposal variant key (ADR 0008). */
enum class ProposalDisposition {
    Followed,
    Ignored,
    ;

    fun asCode(): String = when (this) {
        Followed -> "followed"
        Ignored -> "ignored"
    }

    companion object {
        fun fromCode(code: String): ProposalDisposition? = when (code) {
            "followed" -> Followed
            "ignored" -> Ignored
            else -> null
        }
    }
}

data class CollectionProposalPreference(
    val key: CollectionProposalKey,
    val disposition: ProposalDisposition,
)

data class ClassifiedCollectionProposals(
    val followed: List<CollectionProposal> = emptyList(),
    val available: List<CollectionProposal> = emptyList(),
    val ignored: List<CollectionProposal> = emptyList(),
) {
    val isEmpty: Boolean get() = followed.isEmpty() && available.isEmpty() && ignored.isEmpty()
}

/**
 * Splits current proposals by the collector's stored dispositions. A preference with no
 * current evidence stays dormant: it never materializes a proposal.
 */
fun classifyCollectionProposals(
    proposals: List<CollectionProposal>,
    preferences: List<CollectionProposalPreference>,
): ClassifiedCollectionProposals {
    val byKey = preferences.associate { preference -> preference.key to preference.disposition }
    val followed = mutableListOf<CollectionProposal>()
    val available = mutableListOf<CollectionProposal>()
    val ignored = mutableListOf<CollectionProposal>()
    for (proposal in proposals) {
        when (byKey[proposal.key()]) {
            ProposalDisposition.Followed -> followed += proposal
            ProposalDisposition.Ignored -> ignored += proposal
            null -> available += proposal
        }
    }
    return ClassifiedCollectionProposals(followed, available, ignored)
}
