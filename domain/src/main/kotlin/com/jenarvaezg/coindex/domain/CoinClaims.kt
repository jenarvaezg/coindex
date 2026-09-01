package com.jenarvaezg.coindex.domain

/**
 * Which collections claim which coin, read once off the index of one assembly (#540).
 *
 * It is the link back ADR 0021 §1 promises Coins — «a coin links back to its collections, as a
 * list» — and it lives here rather than in the shelf that draws it because four surfaces ask it:
 * the rows of Coins, the sheet a casilla opens, the «Sin colección» set of the notebook and the
 * country axis. Four readings of one index that disagreed about which coins are loose would be four
 * truths, which is exactly what a single assembly exists to prevent.
 *
 * **Two answers and not one, because they are asked at different grains.** [byType] is what a coin
 * links back to, and a type may be claimed by more than one collection (ADR 0021 §10): membership
 * is declared on the type, so the collector's three Morgans all enter together. [rowIds] is which
 * inventory **rows** were actually placed, which is finer — a catalog that qualifies its members by
 * issue claims one row of a type and leaves its sibling in the residue (ADR 0019). Collapsing them
 * into one would have to pick a grain, and both readings are load-bearing: the American Silver
 * Eagle N#298883 is two rows of one type, one filling a casilla and one belonging to no collection
 * at all, so the type is in a collection and one of those two coins is not.
 *
 * What is deliberately **not** here is where a tap lands: a claim says which collection and with
 * what card, and turning a card into a destination is navigation (ADR 0021 §9), which the domain
 * knows nothing about. The card is the identity, and the UI translates it.
 */
data class CoinClaims(
    /**
     * The cards that claim each Numista type, in the one order of the first level (ADR 0021 §6).
     *
     * The order arrives already right because this is built by walking the index rather than the
     * curated files: what the coin links back to is what the collector saw one screen ago, and a
     * card the inventory no longer derives cannot be linked to at all.
     */
    val byType: Map<Int, List<IndexCard>> = emptyMap(),
    /** The inventory rows some card of the index placed. */
    val rowIds: Set<Long> = emptySet(),
) {
    /** Every collection that claims this type, and an empty list where none does. */
    fun of(typeId: Int): List<IndexCard> = byType[typeId].orEmpty()

    /**
     * Whether a card placed **this row**, which is not the same as its type being in a collection.
     *
     * The row is the grain at which being claimed is decided: it is what the last lámina of the
     * notebook prints a cell per, and what the «Sin colección» chip counts (ADR 0021 §12).
     */
    fun claimed(row: CollectedItem): Boolean = row.id in rowIds

    /**
     * How many of these pieces no collection claims, which is **not** always zero when the type is
     * claimed — see the Silver Eagle of the class documentation.
     */
    fun unclaimedPieces(pieces: List<CollectedItem>): Int = pieces.count { !claimed(it) }
}

/**
 * The claims of one assembled index, walked card by card.
 *
 * The index and not the catalogs, for the same reason [CoinClaims.byType] is ordered by it: a
 * curated file that claims a type the collector owns nothing of produced no card, and a coin cannot
 * link back to a card that is not there. A box is read from the pieces it carries and a derived
 * card from the pieces its key was derived from, which are the same two lists the screens open.
 */
internal fun coinClaimsOf(
    index: List<IndexCard>,
    itemsByKey: Map<VariantKey, List<CollectedItem>>,
): CoinClaims {
    val byType = LinkedHashMap<Int, MutableList<IndexCard>>()
    val rowIds = mutableSetOf<Long>()
    for (card in index) {
        val pieces = when (card) {
            is IndexCard.Derived -> itemsByKey[card.key].orEmpty()
            is IndexCard.Box -> card.box.items
        }
        pieces.forEach { piece -> rowIds += piece.id }
        for (typeId in pieces.mapTo(LinkedHashSet()) { it.typeId }) {
            byType.getOrPut(typeId) { mutableListOf() }.add(card)
        }
    }
    return CoinClaims(byType, rowIds)
}
