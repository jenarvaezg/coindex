package com.jenarvaezg.coindex.domain

/**
 * A grouping the collector made themselves: a heading of their own and the types under it
 * (ADR 0013).
 *
 * It is not a derived collection and not a catalog. A derived collection comes from what
 * Numista says the pieces are, and a catalog is an editorial claim about a sequence; this is
 * neither, it is the collector saying «these go together» — which is the one authority neither
 * of the other two can overrule.
 */
data class OwnGrouping(
    val id: Long,
    val name: String,
    val typeIds: List<Int>,
)

/** One own grouping with the pieces it currently gathers. */
data class OwnGroupingView(
    val grouping: OwnGrouping,
    val items: List<CollectedItem>,
) {
    val id: Long get() = grouping.id
    val name: String get() = grouping.name
    val distinctTypes: Int get() = items.mapTo(mutableSetOf()) { it.typeId }.size
    val quantity: Int get() = items.fold(0) { total, item -> saturatingAdd(total, item.quantity) }
}

/**
 * Fills each own grouping with the pieces currently owned of its types.
 *
 * An extra view, not a move: a grouped piece stays in the collection it was derived into, so
 * nothing disappears from the index for having been organized. Only currently owned pieces
 * count, as everywhere else.
 *
 * A grouping whose types have all left the collection still comes back, empty. Unlike a
 * disposition — which is dormant intent about a derived thing and materializes nothing — this is
 * something the collector typed, and having it vanish because a coin was sold would read as data
 * loss rather than as an empty shelf.
 */
fun buildOwnGroupingViews(
    groupings: List<OwnGrouping>,
    items: List<CollectedItem>,
): List<OwnGroupingView> {
    val owned = items.filter { it.quantity > 0 }
    return groupings.map { grouping ->
        val members = grouping.typeIds.toSet()
        OwnGroupingView(grouping, owned.filter { it.typeId in members })
    }
}
