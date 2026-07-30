package com.jenarvaezg.coindex.domain

/** Why a currently owned piece produced no collection proposal. Nothing is dropped silently. */
sealed interface UnclassifiedReason {
    /** The type has never been fetched, so its family, weight and finish are unknown. */
    data object MissingTypeMetadata : UnclassifiedReason

    /** Numista records no family and no seeded catalog references the type (ADR 0009). */
    data object NoFamilyOrCatalog : UnclassifiedReason

    /** Numista records no usable weight, so the physical variant cannot be identified. */
    data class UnknownWeight(val family: String) : UnclassifiedReason
}

data class UnclassifiedItem(
    val item: CollectedItem,
    val reason: UnclassifiedReason,
)

/** Proposals derived from an inventory, plus every piece that produced none. */
data class CollectionDerivation(
    val proposals: List<CollectionProposal>,
    val unclassified: List<UnclassifiedItem>,
)

/**
 * Groups the collector's current pieces into proposals by exact variant key, and reports
 * every piece that could not be grouped.
 *
 * Only pieces currently owned participate. Families resolve in strict order of how specific
 * the claim is (ADR 0012): a set catalog naming the exact types issued together, then the
 * real Numista family, then a catalog that lists the type (ADR 0009), then Numista's
 * technical monetary system. Only a type with no family and no catalog stays unclassified.
 */
fun deriveCollection(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    catalogs: List<CollectionCatalog>,
): CollectionDerivation {
    val setFamilies: Map<Int, String> = buildMap {
        for (catalog in catalogs.filter { it.isSet }) {
            for (member in catalog.members) {
                putIfAbsent(member.numistaTypeId, catalog.family)
            }
        }
    }
    val catalogFamilies: Map<Int, String> = buildMap {
        for (catalog in catalogs.filterNot { it.isSet }) {
            for (member in catalog.members) {
                putIfAbsent(member.numistaTypeId, catalog.family)
            }
        }
    }
    val curatedWeights: Set<Int> = catalogs.mapNotNullTo(mutableSetOf()) { it.weightMillioz }
    val grouped = LinkedHashMap<CollectionProposalKey, ProposalAccumulator>()
    val unclassified = mutableListOf<UnclassifiedItem>()

    for (item in items.filter { it.quantity > 0 }) {
        val setFamily = setFamilies[item.typeId]
        if (setFamily != null) {
            // The set is the collectible unit: neither weight nor finish enters its key.
            grouped.record(CollectionProposalKey(setFamily, null, null), item)
            continue
        }
        val metadata = typeMeta[item.typeId]
        if (metadata == null) {
            unclassified += UnclassifiedItem(item, UnclassifiedReason.MissingTypeMetadata)
            continue
        }
        val numistaFamily = metadata.family?.let(::normalizeFamily)
        val family = when {
            numistaFamily == null -> catalogFamilies[item.typeId]
            isTechnicalFamily(numistaFamily) -> catalogFamilies[item.typeId] ?: numistaFamily
            else -> numistaFamily
        }
        if (family == null) {
            unclassified += UnclassifiedItem(item, UnclassifiedReason.NoFamilyOrCatalog)
            continue
        }
        val weightMillioz = metadata.weightOz?.let { weightOz ->
            normalizeWeightMillioz(weightOz, curatedWeights)
        }
        if (weightMillioz == null) {
            unclassified += UnclassifiedItem(item, UnclassifiedReason.UnknownWeight(family))
            continue
        }
        grouped.record(CollectionProposalKey(family, weightMillioz, metadata.finish), item)
    }

    val proposals = grouped.entries
        .sortedWith(
            compareBy(
                { (key, _) -> key.family },
                { (key, _) -> key.weightMillioz },
                { (key, _) -> finishOrder(key.finish) },
            ),
        )
        .map { (key, accumulator) ->
            CollectionProposal(
                family = key.family,
                weightMillioz = key.weightMillioz,
                finish = key.finish,
                distinctTypes = accumulator.typeIds.size,
                quantity = accumulator.quantity,
            )
        }
    return CollectionDerivation(proposals, unclassified)
}

/** Convenience wrapper over [deriveCollection] for callers that only need the proposals. */
fun buildCollectionProposals(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    catalogs: List<CollectionCatalog>,
): List<CollectionProposal> = deriveCollection(items, typeMeta, catalogs).proposals

private class ProposalAccumulator {
    val typeIds = mutableSetOf<Int>()
    var quantity: Int = 0
}

private fun MutableMap<CollectionProposalKey, ProposalAccumulator>.record(
    key: CollectionProposalKey,
    item: CollectedItem,
) {
    val accumulator = getOrPut(key) { ProposalAccumulator() }
    accumulator.typeIds += item.typeId
    accumulator.quantity = saturatingAdd(accumulator.quantity, item.quantity)
}

/** Quantities come from a third-party catalog; a hostile total must not wrap around. */
internal fun saturatingAdd(left: Int, right: Int): Int {
    val total = left.toLong() + right.toLong()
    return if (total > Int.MAX_VALUE) Int.MAX_VALUE else total.toInt()
}
