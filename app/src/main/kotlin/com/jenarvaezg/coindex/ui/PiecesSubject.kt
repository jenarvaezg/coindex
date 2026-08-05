package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.VariantKey

/**
 * What `PiecesScreen` is looking at: one collection without an issue list, or a box.
 *
 * The merge of ADR 0021 §9 lives in this type. Two screens became one because the cases differ in
 * what they **have** — a physical variant, an upkeep — and not in what they are, so the screen is
 * handed one shape and reads the differences off its fields. The alternative, a screen that asks
 * which case it is, would be the word of provenance §2 removed from the card, said one level down.
 *
 * It is built from the same [IndexCard] the index drew, so the header of the screen and the card
 * that opened it cannot drift: same name, same country, same count.
 */
data class PiecesSubject(
    val title: String,
    /** The country, unsaid when nothing can name it without claiming more than it knows. */
    val issuer: String?,
    /** The physical variant, or null for a box: it spans whatever the collector put in it. */
    val variant: String?,
    /**
     * The ratio the card showed, where there was one.
     *
     * Only a collection whose catalog it owns no issued member of yet can arrive here carrying one
     * — with evidence the card opens its plate instead (ADR 0021 §7, §9) — and it has to keep
     * saying «0 de 12 · te faltan 12», because the same collection cannot count one way on the
     * card and another one tap later.
     */
    val coverage: CoverageRatio?,
    val distinctTypes: Int,
    val quantity: Int,
    val pieces: List<CollectedItem>,
    /** The box this screen maintains, or null when there is nothing to maintain. */
    val boxId: Long?,
)

/** The three things that can be done to a box, and to nothing else (ADR 0021 §9, §11). */
data class BoxUpkeep(
    val onRename: (name: String) -> Unit,
    val onRemoveType: (typeId: Int) -> Unit,
    val onDelete: () -> Unit,
)

fun piecesSubject(state: CollectionState, card: IndexCard): PiecesSubject = when (card) {
    is IndexCard.Derived -> PiecesSubject(
        title = card.name,
        issuer = card.issuer,
        variant = variantLabel(
            card.collection.weightMillioz,
            card.collection.finish,
            card.collection.metal,
        ),
        coverage = card.coverage,
        distinctTypes = card.distinctTypes,
        quantity = card.quantity,
        pieces = state.itemsByKey[card.key].orEmpty().inReadingOrder(),
        boxId = null,
    )
    is IndexCard.Box -> PiecesSubject(
        title = card.name,
        issuer = card.issuer,
        variant = null,
        coverage = card.coverage,
        distinctTypes = card.distinctTypes,
        quantity = card.quantity,
        pieces = card.box.items.inReadingOrder(),
        boxId = card.box.id,
    )
}

/**
 * What to call a piece: the catalog title if its type is cached, else what the row itself says.
 *
 * Shared by the three lists that draw a coin — a collection's pieces, its exported sheet, and Coins
 * (ADR 0021 §1) — so the same coin cannot be called two things one tap apart.
 */
internal fun pieceTitle(state: CollectionState, item: CollectedItem): String =
    state.typeMeta[item.typeId]?.displayTitle
        ?: item.title
        ?: "Pieza ${item.id}"

/**
 * The order pieces are read in: the year first, because it is what usually tells two rows of the
 * same type apart, and a row without one goes last — undated is the least identified a row gets.
 */
private fun List<CollectedItem>.inReadingOrder(): List<CollectedItem> = sortedWith(
    compareBy({ it.recordedYear ?: Int.MAX_VALUE }, { it.title.orEmpty() }, { it.id }),
)

/**
 * The card a pieces route points at, or null if it no longer exists.
 *
 * A derived collection can vanish under the screen while it is open — a piece sold on Numista and
 * synced away leaves the route valid and its subject gone — and a box can be undone from the screen
 * itself. Neither is guessed at.
 */
fun CollectionState.piecesCardFor(key: VariantKey): IndexCard.Derived? =
    index.filterIsInstance<IndexCard.Derived>().firstOrNull { it.key == key }

fun CollectionState.piecesCardForBox(boxId: Long): IndexCard.Box? =
    index.filterIsInstance<IndexCard.Box>().firstOrNull { it.box.id == boxId }
