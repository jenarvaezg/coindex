package com.jenarvaezg.coindex.domain

/** Why a currently owned piece produced no derived collection. Nothing is dropped silently. */
sealed interface UnclassifiedReason {
    /** The type has never been fetched, so its family, weight and finish are unknown. */
    data object MissingTypeMetadata : UnclassifiedReason

    /** Numista records no family and no seeded catalog references the type (ADR 0009). */
    data object NoFamilyOrCatalog : UnclassifiedReason

    /** Catalogs qualify this type by issue, but none claims the piece's recorded issue. */
    data object IssueNotClaimedByCatalog : UnclassifiedReason

    /** Numista records no usable weight, so the physical variant cannot be identified. */
    data class UnknownWeight(val family: String) : UnclassifiedReason

    /**
     * The Numista page is still a submission awaiting a referee, so its fields are whatever the
     * contributor half-typed and no collection is derived from them (#186).
     */
    data object UnpublishedType : UnclassifiedReason
}

/**
 * One entry of the unclassified residue: inventory rows collapsed by type, except when the
 * reason is [UnclassifiedReason.IssueNotClaimedByCatalog], where the unit is the issue —
 * two unmatched issues of the same type are two leftovers, not a ×2 (#93).
 *
 * [item] is the first row encountered (title, photos, year). [quantity] is the saturating sum
 * across collapsed rows; [rowCount] is how many rows were fused. Drawing the ×N is the screen
 * map's job; this type only delivers the numbers.
 */
data class UnclassifiedItem(
    val item: CollectedItem,
    val reason: UnclassifiedReason,
    val quantity: Int,
    val rowCount: Int,
)

/**
 * Every derived collection an inventory produces, plus every piece that produced none.
 *
 * [itemsByKey] is what each one is made of, in the same order the inventory had it:
 * a derived collection is a summary, and the screen that opens it has to show the actual pieces.
 */
data class CollectionDerivation(
    val derivedCollections: List<DerivedCollection>,
    val unclassified: List<UnclassifiedItem>,
    val itemsByKey: Map<VariantKey, List<CollectedItem>> = emptyMap(),
)

/**
 * Groups the collector's current pieces into derived collections by exact variant key,
 * and reports every piece that could not be grouped.
 *
 * Only pieces currently owned participate. Families resolve in strict order of how specific
 * the claim is: a set catalog naming the exact types issued together, then a collection catalog
 * that claims the piece, then the real Numista family, then a curated
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
                member.numistaTypeId?.let { putIfAbsent(it, catalog.family) }
            }
        }
    }
    /**
     * A catalog that is not a set is authoritative about the complete variant key of the
     * types it claims: family, weight, finish and metal are verified by hand, so Numista's per-type
     * family or gram value does not get to split them.
     *
     * Snapping alone cannot do this. The nineteen 1000 escudos of Portugal are one coin whose
     * weight Numista records as 27, 28 and 28.2 grams — 868, 900 and 907 milli-ounces, a spread
     * of 39 against a snap tolerance of 10 that is deliberately tight so a 30 g piece is never
     * read as an ounce. Without this, one curated catalog produced two cards and counted five
     * pieces in both.
     */
    val catalogsByType: Map<Int, List<CollectionCatalog>> = buildMap<Int, MutableList<CollectionCatalog>> {
        for (catalog in catalogs.filterNot { it.isSet }) {
            for (member in catalog.members) {
                member.numistaTypeId?.let { typeId ->
                    getOrPut(typeId) { mutableListOf() }.add(catalog)
                }
            }
        }
    }.mapValues { (_, catalogsForType) -> catalogsForType.distinctBy { it.id } }
    val groupingFamilies: Map<Int, String> = buildMap {
        for (grouping in groupings) {
            for (typeId in grouping.typeIds) {
                putIfAbsent(typeId, grouping.family)
            }
        }
    }
    val grouped = LinkedHashMap<VariantKey, DerivedCollectionAccumulator>()
    val unclassifiedGrouped = LinkedHashMap<UnclassifiedGroupKey, UnclassifiedAccumulator>()

    for (item in items.filter { it.quantity > 0 }) {
        val setFamily = setFamilies[item.typeId]
        if (setFamily != null) {
            // The set is the collectible unit: no part of the variant enters its key.
            grouped.record(VariantKey(setFamily, null, null, null), item)
            continue
        }
        val metadata = typeMeta[item.typeId]
        if (metadata == null) {
            unclassifiedGrouped.record(item, UnclassifiedReason.MissingTypeMetadata)
            continue
        }
        // A family made only of articles is the beginning of a name and not a name, so it is read
        // as the absent field it is (#404). No reason of its own: «sin familia en Numista» is what
        // the residue will say, and for a «The» that is the truth — whereas a card called «The»
        // was not. A curated file still outranks it, as it does everywhere else, because
        // [curatedFamily] answers precisely when Numista has nothing to give.
        val numistaFamily = metadata.family?.let(::normalizeFamily)?.takeUnless(::isPlaceholderFamily)
        val candidateCatalogs = catalogsByType[item.typeId].orEmpty()
        val issueQualifiedClaim = candidateCatalogs.any { candidate ->
            candidate.members.any { member ->
                member.numistaTypeId == item.typeId && member.numistaIssueIds.isNotEmpty()
            }
        }
        val matchingCatalogs = candidateCatalogs.filter { candidate ->
            candidate.members.any { member -> candidate.memberMatches(member, item) }
        }
        check(matchingCatalogs.size <= 1) {
            val catalogIds = matchingCatalogs.map { it.id }.sorted().joinToString("`, `")
            "collected item type `${item.typeId}` and issue `${item.issueId}` " +
                "matches multiple collection catalogs: `$catalogIds`"
        }
        val catalog = matchingCatalogs.singleOrNull()
            ?: candidateCatalogs.firstOrNull().takeUnless { issueQualifiedClaim }
        if (catalog == null && issueQualifiedClaim) {
            unclassifiedGrouped.record(item, UnclassifiedReason.IssueNotClaimedByCatalog)
            continue
        }
        // A submission in review is not verifiable, and what a curator could not sign does not get
        // to invent a card either (#186). Two limits keep the check to the damage it repairs:
        //
        // - A curated file outranks it. If a versioned file names the type, someone verified it by
        //   hand and the file rules (ADR 0016), so no plate slot is lost to a half-typed field.
        // - Only a declared family triggers it. Without one the piece is already in the residue for
        //   want of a family, and that reason does not lie — whereas «unpublished» would, for a
        //   published type nobody has dated.
        if (catalog == null && numistaFamily != null && metadata.looksUnpublished) {
            unclassifiedGrouped.record(item, UnclassifiedReason.UnpublishedType)
            continue
        }
        val curatedFamily = catalog?.family ?: groupingFamilies[item.typeId]
        val family = when {
            catalog != null -> catalog.family
            numistaFamily == null -> curatedFamily
            isTechnicalFamily(numistaFamily) -> curatedFamily ?: numistaFamily
            else -> numistaFamily
        }
        if (family == null) {
            unclassifiedGrouped.record(item, UnclassifiedReason.NoFamilyOrCatalog)
            continue
        }
        // Its own catalog names the family and variant, so neither has to be inferred.
        if (catalog != null) {
            grouped.record(catalog.key(), item)
            continue
        }
        // Numista's own grams, snapped to the common bullion weights and to nothing a catalog
        // declares: past this point the type is one no curated file has a weight for (#288). The
        // shelf's loose row weighs that same coin by the very same property and no longer by a
        // second call of its own (#540).
        val weightMillioz = metadata.weightMillioz
        if (weightMillioz == null) {
            unclassifiedGrouped.record(item, UnclassifiedReason.UnknownWeight(family))
            continue
        }
        grouped.record(
            VariantKey(family, weightMillioz, metadata.finish, metadata.metal),
            item,
        )
    }

    val ordered = grouped.entries.sortedWith(
        compareBy(
            { (key, _) -> key.family },
            { (key, _) -> key.weightMillioz },
            { (key, _) -> finishOrder(key.finish) },
            { (key, _) -> metalOrder(key.metal) },
        ),
    )
    val derivedCollections = ordered.map { (key, accumulator) ->
        DerivedCollection(
            family = key.family,
            weightMillioz = key.weightMillioz,
            finish = key.finish,
            metal = key.metal,
            distinctTypes = accumulator.typeIds.size,
            quantity = accumulator.quantity,
        )
    }
    val itemsByKey = ordered.associate { (key, accumulator) -> key to accumulator.items.toList() }
    val unclassified = unclassifiedGrouped.values.map { accumulator ->
        UnclassifiedItem(
            item = accumulator.item,
            reason = accumulator.reason,
            quantity = accumulator.quantity,
            rowCount = accumulator.rowCount,
        )
    }
    return CollectionDerivation(derivedCollections, unclassified, itemsByKey)
}

private class DerivedCollectionAccumulator {
    val typeIds = mutableSetOf<Int>()
    val items = mutableListOf<CollectedItem>()
    var quantity: Int = 0
}

private fun MutableMap<VariantKey, DerivedCollectionAccumulator>.record(
    key: VariantKey,
    item: CollectedItem,
) {
    val accumulator = getOrPut(key) { DerivedCollectionAccumulator() }
    accumulator.typeIds += item.typeId
    accumulator.items += item
    accumulator.quantity = saturatingAdd(accumulator.quantity, item.quantity)
}

/**
 * Collapse key for the unclassified residue. Type is enough for every reason except
 * [UnclassifiedReason.IssueNotClaimedByCatalog], which scopes to the recorded issue.
 */
private sealed interface UnclassifiedGroupKey {
    data class ByType(val typeId: Int) : UnclassifiedGroupKey
    data class ByIssue(val typeId: Int, val issueId: Int?) : UnclassifiedGroupKey
}

private class UnclassifiedAccumulator(
    val item: CollectedItem,
    val reason: UnclassifiedReason,
) {
    var quantity: Int = 0
    var rowCount: Int = 0
}

private fun unclassifiedGroupKey(
    item: CollectedItem,
    reason: UnclassifiedReason,
): UnclassifiedGroupKey = when (reason) {
    UnclassifiedReason.IssueNotClaimedByCatalog ->
        UnclassifiedGroupKey.ByIssue(item.typeId, item.issueId)
    else -> UnclassifiedGroupKey.ByType(item.typeId)
}

private fun MutableMap<UnclassifiedGroupKey, UnclassifiedAccumulator>.record(
    item: CollectedItem,
    reason: UnclassifiedReason,
) {
    val key = unclassifiedGroupKey(item, reason)
    val accumulator = getOrPut(key) { UnclassifiedAccumulator(item, reason) }
    check(accumulator.reason == reason) {
        "collected item type `${item.typeId}` collapsed under conflicting unclassified reasons: " +
            "`${accumulator.reason}` and `$reason`"
    }
    accumulator.quantity = saturatingAdd(accumulator.quantity, item.quantity)
    accumulator.rowCount += 1
}

/**
 * Quantities come from a third-party catalog; a hostile total must not wrap around.
 *
 * Public because every list that adds up pieces has to add them up the same way: a derived
 * collection, a box, and the rows of Coins (ADR 0021 §1) all sum quantities Numista supplied.
 */
fun saturatingAdd(left: Int, right: Int): Int {
    val total = left.toLong() + right.toLong()
    return if (total > Int.MAX_VALUE) Int.MAX_VALUE else total.toInt()
}
