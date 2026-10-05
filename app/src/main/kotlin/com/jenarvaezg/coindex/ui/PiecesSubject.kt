package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.VariantKey

/**
 * One piece as it is drawn, with what identifies it already resolved (#225). The screen card, the
 * exported sheet and the notebook page all take this rather than a bare [CollectedItem], since the
 * emission label can be the piece's identity (the star where the row only says 1966).
 * [emissionLabel] has no default on purpose: as an optional parameter it was once silently
 * dropped from all three.
 */
data class DrawnPiece(
    val item: CollectedItem,
    /** «Estrella 67», where a catalog keyed on issues names the emission. Null everywhere else. */
    val emissionLabel: String?,
)

/**
 * What `PiecesScreen` is looking at: one collection without an issue list, or a box (ADR 0021 §9).
 * One shape, whose fields say what each case has (a variant, an upkeep), so the screen never asks
 * which case it is. Built from the [IndexCard] the index drew, so the header matches the card.
 */
data class PiecesSubject(
    val title: String,
    /** Null when no single country can be named. */
    val issuer: String?,
    /** The physical variant, or null for a box: it spans whatever the collector put in it. */
    val variant: String?,
    /**
     * The ratio the card showed, if any. Only a catalog with no owned issued member arrives here
     * with one, since with evidence the card opens its plate (ADR 0021 §7, §9).
     */
    val coverage: CoverageRatio?,
    val distinctTypes: Int,
    val quantity: Int,
    val pieces: List<DrawnPiece>,
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
        pieces = state.drawnPieces(state.itemsByKey[card.key].orEmpty()),
        boxId = null,
    )
    is IndexCard.Box -> PiecesSubject(
        title = card.name,
        issuer = card.issuer,
        variant = null,
        coverage = card.coverage,
        distinctTypes = card.distinctTypes,
        quantity = card.quantity,
        pieces = state.drawnPieces(card.box.items),
        boxId = card.box.id,
    )
}

/**
 * What to call a piece: the catalog title if its type is cached, else what the row itself says.
 * Shared by every list that draws a coin, so it has one name everywhere (ADR 0021 §1).
 */
fun pieceName(state: CollectionState, item: CollectedItem): CoinName =
    coinName(pieceRawTitle(state, item))

/** Full Numista title, kept separately from the album name for search and the ficha. */
fun pieceRawTitle(state: CollectionState, item: CollectedItem): String =
    state.typeMeta[item.typeId]?.title
        ?: state.typeMeta[item.typeId]?.displayTitle
        ?: item.title
        ?: pieceFallbackTitle(item)

private fun pieceFallbackTitle(item: CollectedItem): String = "Pieza ${item.id}"

internal fun pieceTitle(state: CollectionState, item: CollectedItem): String =
    pieceName(state, item).text

/**
 * The pieces of one collection, by year with undated rows last, each with its emission label. The
 * label is a fact about the coin, so it is the same whichever card the collector came through.
 */
private fun CollectionState.drawnPieces(items: List<CollectedItem>): List<DrawnPiece> = items
    .sortedWith(compareBy({ it.recordedYear ?: Int.MAX_VALUE }, { it.title.orEmpty() }, { it.id }))
    .map { item -> DrawnPiece(item, emissionLabels[item.id]) }

/**
 * The card a pieces route points at, or null if it no longer exists: a derived collection can
 * vanish after a sync while open, and a box can be undone from its own screen.
 */
fun CollectionState.piecesCardFor(key: VariantKey): IndexCard.Derived? =
    index.filterIsInstance<IndexCard.Derived>().firstOrNull { it.key == key }

fun CollectionState.piecesCardForBox(boxId: Long): IndexCard.Box? =
    index.filterIsInstance<IndexCard.Box>().firstOrNull { it.box.id == boxId }
