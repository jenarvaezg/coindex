package com.jenarvaezg.coindex.domain

/**
 * A grouping the collector made: a heading of their own and the types under it (ADR 0021 §11). Neither
 * a derived collection (what Numista says the pieces are) nor a catalog (an editorial claim about
 * a sequence): it is the collector saying «these go together», which neither can overrule.
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
 * Fills each own grouping with the owned pieces of its types. An extra view, not a move: a grouped
 * piece stays in its derived collection.
 *
 * A grouping whose types have all left the collection still comes back, empty, in its place (ADR
 * 0021 §11): it is the one thing the collector typed, and losing it when a coin is sold would read
 * as data loss.
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
