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

/**
 * Proposals derived from an inventory, plus every piece that produced none.
 *
 * [itemsByKey] is what each proposal is made of, in the same order the inventory had it: a
 * proposal is a summary, and the screen that opens it has to show the actual pieces.
 */
data class CollectionDerivation(
    val proposals: List<CollectionProposal>,
    val unclassified: List<UnclassifiedItem>,
    val itemsByKey: Map<CollectionProposalKey, List<CollectedItem>> = emptyMap(),
)

/**
 * Groups the collector's current pieces into proposals by exact variant key, and reports
 * every piece that could not be grouped.
 *
 * Only pieces currently owned participate. Families resolve in strict order of how specific
 * the claim is (ADR 0012, ADR 0013): a set catalog naming the exact types issued together,
 * then the real Numista family, then a catalog that lists the type (ADR 0009), then a curated
 * grouping that merely says these types belong together, then Numista's technical monetary
 * system. Only a type that none of them names stays unclassified.
 */
fun deriveCollection(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping> = emptyList(),
): CollectionDerivation {
    val setFamilies: Map<Int, String> = buildMap {
        for (catalog in catalogs.filter { it.isSet }) {
            for (member in catalog.members) {
                putIfAbsent(member.numistaTypeId, catalog.family)
            }
        }
    }
    /**
     * A catalog that is not a set is authoritative about the physical variant of its own
     * members: it declares exactly one weight and one finish, verified by hand, so Numista's
     * per-type gram value does not get to split them.
     *
     * Snapping alone cannot do this. The nineteen 1000 escudos of Portugal are one coin whose
     * weight Numista records as 27, 28 and 28.2 grams — 868, 900 and 907 milli-ounces, a spread
     * of 39 against a snap tolerance of 10 that is deliberately tight so a 30 g piece is never
     * read as an ounce. Without this, one curated catalog produced two cards and counted five
     * pieces in both.
     */
    val catalogsByType: Map<Int, CollectionCatalog> = buildMap {
        for (catalog in catalogs.filterNot { it.isSet }) {
            for (member in catalog.members) {
                putIfAbsent(member.numistaTypeId, catalog)
            }
        }
    }
    val groupingFamilies: Map<Int, String> = buildMap {
        for (grouping in groupings) {
            for (typeId in grouping.typeIds) {
                putIfAbsent(typeId, grouping.family)
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
        val catalog = catalogsByType[item.typeId]
        val curatedFamily = catalog?.family ?: groupingFamilies[item.typeId]
        val family = when {
            numistaFamily == null -> curatedFamily
            isTechnicalFamily(numistaFamily) -> curatedFamily ?: numistaFamily
            else -> numistaFamily
        }
        if (family == null) {
            unclassified += UnclassifiedItem(item, UnclassifiedReason.NoFamilyOrCatalog)
            continue
        }
        // Its own catalog names the variant, so no weight has to be inferred at all.
        if (catalog != null && catalog.family == family) {
            grouped.record(catalog.key(), item)
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

    val ordered = grouped.entries.sortedWith(
        compareBy(
            { (key, _) -> key.family },
            { (key, _) -> key.weightMillioz },
            { (key, _) -> finishOrder(key.finish) },
        ),
    )
    val proposals = ordered.map { (key, accumulator) ->
        CollectionProposal(
            family = key.family,
            weightMillioz = key.weightMillioz,
            finish = key.finish,
            distinctTypes = accumulator.typeIds.size,
            quantity = accumulator.quantity,
        )
    }
    val itemsByKey = ordered.associate { (key, accumulator) -> key to accumulator.items.toList() }
    return CollectionDerivation(proposals, unclassified, itemsByKey)
}

/** Convenience wrapper over [deriveCollection] for callers that only need the proposals. */
fun buildCollectionProposals(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    catalogs: List<CollectionCatalog>,
    groupings: List<CuratedGrouping> = emptyList(),
): List<CollectionProposal> =
    deriveCollection(items, typeMeta, catalogs, groupings).proposals

private class ProposalAccumulator {
    val typeIds = mutableSetOf<Int>()
    val items = mutableListOf<CollectedItem>()
    var quantity: Int = 0
}

private fun MutableMap<CollectionProposalKey, ProposalAccumulator>.record(
    key: CollectionProposalKey,
    item: CollectedItem,
) {
    val accumulator = getOrPut(key) { ProposalAccumulator() }
    accumulator.typeIds += item.typeId
    accumulator.items += item
    accumulator.quantity = saturatingAdd(accumulator.quantity, item.quantity)
}

/** Quantities come from a third-party catalog; a hostile total must not wrap around. */
internal fun saturatingAdd(left: Int, right: Int): Int {
    val total = left.toLong() + right.toLong()
    return if (total > Int.MAX_VALUE) Int.MAX_VALUE else total.toInt()
}
