package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.VariantKey

/** Where a tap on a card of the index lands. One card, one destination (ADR 0021 §9). */
sealed interface CardDestination {
    data class Plate(val catalogId: String) : CardDestination

    data class Pieces(val key: VariantKey) : CardDestination

    data class Box(val boxId: Long) : CardDestination
}

/**
 * A card opens its plate when it has one and its list of pieces otherwise, whatever species of
 * collection it is (ADR 0021 §3). The plate shows everything the list would, and more.
 *
 * The test is `plateCatalogId`, not whether there is a catalog: a plate opens only on evidence
 * (ADR 0021 §7), so a catalog with no owned issued member has none yet. Such a card still counts
 * `0 de 12` on the way in (see `PiecesSubject.coverage`).
 */
fun destinationOf(card: IndexCard): CardDestination = when (card) {
    is IndexCard.Derived -> card.plateCatalogId
        ?.let(CardDestination::Plate)
        ?: CardDestination.Pieces(card.key)
    is IndexCard.Box -> CardDestination.Box(card.box.id)
}
