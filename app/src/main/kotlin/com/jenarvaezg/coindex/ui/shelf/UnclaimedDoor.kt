package com.jenarvaezg.coindex.ui.shelf

/** The row at the foot of the plate axis that opens Monedas on «Sin colección» (#634). */
data class UnclaimedDoor(val shelf: CoinsShelf, val types: Int)

/**
 * Only the index's country travels: its other chips have no counterpart in Monedas. The count is
 * the chip's, so it is measured by narrowing to the shelf the door opens. Null at zero
 * (ADR 0026 §8 clause 3).
 */
fun unclaimedDoor(rows: List<CoinRow>, index: IndexShelf): UnclaimedDoor? {
    val shelf = CoinsShelf(issuer = index.issuer, membership = Membership.InNone)
    val types = shelf.narrow(rows, "").size
    return if (types > 0) UnclaimedDoor(shelf, types) else null
}
