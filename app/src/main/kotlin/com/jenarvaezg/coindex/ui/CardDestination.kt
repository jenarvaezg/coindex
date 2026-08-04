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
 * The destination of a card, chosen by the capability of ADR 0021 §3 — having an issue list to
 * open — and never by which species of collection the card is.
 *
 * The plate wins wherever there is one because it shows everything the list of pieces would and
 * more: measured over the 1033 curated slots of `data/`, the pieces that fall in a card with a
 * catalog and in no slot of its plate are **0**. The second destination was 38 rows to reach the
 * same thing.
 *
 * The bit asked is `plateCatalogId` and not «does it have a catalog», because ADR 0021 §7 opens a
 * plate **on evidence**: a catalog the collector owns no issued member of yet has no plate to show,
 * and sending them there would be one tap to a screen whose only content is why it is empty. Such a
 * card still counts `0 de 12` on the way in — see `PiecesSubject.coverage`.
 */
fun destinationOf(card: IndexCard): CardDestination = when (card) {
    is IndexCard.Derived -> card.plateCatalogId
        ?.let(CardDestination::Plate)
        ?: CardDestination.Pieces(card.key)
    is IndexCard.Box -> CardDestination.Box(card.box.id)
}
