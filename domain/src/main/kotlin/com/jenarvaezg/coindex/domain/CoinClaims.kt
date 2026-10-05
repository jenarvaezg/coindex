package com.jenarvaezg.coindex.domain

/**
 * Which collections claim which coin, read once off the index of one assembly (#540): the link back
 * from a coin to its collections that ADR 0021 §1 promises. The rows of Coins, the casilla sheet,
 * the notebook's «Sin colección» set and the country axis all read it, so they agree on which coins
 * are loose.
 *
 * Two grains. [byType] is what a coin links back to; membership is declared on the type, and a
 * type may belong to several collections (ADR 0021 §10). [rowIds] is which inventory rows were
 * placed, which is finer: an issue-qualified catalog claims one row of a type and leaves its
 * sibling in the residue (ADR 0019). The American Silver Eagle N#298883 has two rows of one type,
 * only one of them in a casilla.
 *
 * Where a tap lands is navigation (ADR 0021 §9) and stays in the UI; a claim only names the card.
 */
data class CoinClaims(
    /**
     * The cards that claim each Numista type, in the order of the first level (ADR 0021 §6). Built
     * from the index, not the curated files, so a coin links only to cards the collector can see.
     */
    val byType: Map<Int, List<IndexCard>> = emptyMap(),
    /** The inventory rows some card of the index placed. */
    val rowIds: Set<Long> = emptySet(),
) {
    /** Every collection that claims this type, and an empty list where none does. */
    fun of(typeId: Int): List<IndexCard> = byType[typeId].orEmpty()

    /**
     * Whether a card placed this row, which is not the same as its type being in a collection. The
     * row is the grain the notebook's last lámina prints and the «Sin colección» chip counts (ADR
     * 0021 §12).
     */
    fun claimed(row: CollectedItem): Boolean = row.id in rowIds

    /** How many of these pieces no collection claims; not always zero when the type is claimed. */
    fun unclaimedPieces(pieces: List<CollectedItem>): Int = pieces.count { !claimed(it) }
}

/**
 * The claims of one assembled index, walked card by card: a curated file whose types the collector
 * does not own makes no card, and a coin cannot link to a missing card. Each card is read from the
 * same pieces its screen opens.
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
