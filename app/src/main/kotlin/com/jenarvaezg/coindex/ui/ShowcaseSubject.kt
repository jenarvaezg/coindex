package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.WishedSlot

/**
 * One tile of «Explorar», from either population (ADR 0030 §8). One shape, as ADR 0021 §2 does for
 * the index: what differs between a collector's plate and a shelf-window plate is drawn from what
 * the tile has (a fraction, an amount, marks), never from asking which kind it is.
 *
 * @param footnote never empty: a shelf-window plate says its casilla count, or its cost of entering
 *   once valued; a collector's plate says its index card's fraction.
 * @param marks «2 lo busco» when the plate has marked casillas, which is what puts a collector's
 *   plate on the shelf.
 * @param entryEur the cost of entering, for the order that sorts by it; null until valued, which is
 *   why «por coste de entrar» can't be the default (§8).
 * @param coverOwned whether the coin in the tile's hole is owned, which decides how the hole is
 *   drawn (#556). Read from here, not from [mine], so the grid doesn't look sorted by ownership
 *   (§8 clause 1).
 */
data class ShowcaseTile(
    val catalogId: String,
    val name: String,
    val typeId: Int?,
    val printedSide: PrintedSide,
    val mine: Boolean,
    val footnote: String,
    val marks: String? = null,
    val entryEur: Double? = null,
    val slots: Int = 0,
    val coverOwned: Boolean = false,
)

/**
 * The whole shelf, worded once: the shelf window and the collector's plates that hold a mark.
 * Assembled here, like `plateSubject`, so the lazy grid doesn't read the curated files, the
 * inventory and the price table per item. A collector's plate enters only through a mark
 * (§8 clause 1).
 */
fun showcaseTiles(
    window: List<ShowcasePlate>,
    cards: List<IndexCard>,
    wishes: List<WishedSlot>,
    state: CollectionState,
    book: PriceBook,
    nowMillis: Long,
): List<ShowcaseTile> {
    val marksByCatalog = wishes.groupingBy { it.catalog.id }.eachCount()
    val mine = cards
        .filterIsInstance<IndexCard.Derived>()
        .mapNotNull { card ->
            val catalogId = card.plateCatalogId ?: return@mapNotNull null
            val marks = marksByCatalog[catalogId] ?: return@mapNotNull null
            val coverage = card.coverage
            ShowcaseTile(
                catalogId = catalogId,
                name = card.name,
                typeId = card.cover?.typeId,
                printedSide = card.cover?.printedSide ?: PrintedSide.Reverse,
                mine = true,
                // Covered by its card's `IndexCover`, which is an owned coin.
                coverOwned = true,
                // The same fraction its index card prints.
                footnote = coverage?.let { "${it.owned}/${it.issued}" } ?: countLabel(
                    card.distinctTypes,
                    card.quantity,
                ),
                marks = showcaseWishedLabel(marks),
            )
        }
    val fromWindow = window.map { plate ->
        val money = showcaseMoney(plate, state, book)
        val cover = plate.album.members.firstOrNull { it.member.numistaTypeId != null }?.member
        ShowcaseTile(
            catalogId = plate.catalog.id,
            name = (plate.catalog.shortName ?: plate.catalog.name).weldUnits(),
            typeId = cover?.numistaTypeId,
            printedSide = plate.catalog.printedSide,
            mine = false,
            // The window is unowned (§1): its cover is a catalog member, not a piece.
            coverOwned = false,
            // The casilla count until valued, then the cost. Never `0/12`, which would read as a
            // reproach (ADR 0030 §6).
            footnote = money.entry
                ?.let { showcaseTileCostLabel(it, nowMillis) }
                ?: showcaseSlotsLabel(plate.slots),
            marks = marksByCatalog[plate.catalog.id]?.let(::showcaseWishedLabel),
            entryEur = money.entry?.eur,
            slots = plate.slots,
        )
    }
    return mine + fromWindow
}

/**
 * The shelf in the chosen order, narrowed by name (ADR 0026 §8 clause 4) with [fold] and
 * [matchesQuery], like the other two search boxes (#515).
 *
 * Marked plates lead only in the default order. «Por coste de entrar» (#282) sorts valued plates
 * dearest first and leaves the rest after them by casillas.
 */
fun showcaseShelf(
    tiles: List<ShowcaseTile>,
    sort: ShowcaseSort,
    query: String,
): List<ShowcaseTile> {
    val narrowed = tiles.filter { matchesQuery(fold(it.name), query) }
    return when (sort) {
        ShowcaseSort.ByCasillas -> narrowed.sortedWith(
            compareByDescending<ShowcaseTile> { it.marks != null }
                .thenBy { it.slots }
                .thenBy { it.name },
        )
        ShowcaseSort.ByEntryCost -> narrowed.sortedWith(
            compareBy<ShowcaseTile> { it.entryEur == null }
                .thenByDescending { it.entryEur ?: 0.0 }
                .thenBy { it.slots }
                .thenBy { it.name },
        )
    }
}
