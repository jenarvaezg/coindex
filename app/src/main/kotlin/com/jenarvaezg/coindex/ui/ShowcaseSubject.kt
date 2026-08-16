package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.WishedSlot

/**
 * One tile of «Explorar», whichever of the two populations it comes from (ADR 0030 §8).
 *
 * **One shape and not two**, which is the same answer ADR 0021 §2 gives the index: what varies between
 * a plate of the collector's and one of the shelf window is drawn from what the tile *has* — a fraction,
 * an amount, a count of marks — and never from a branch on which kind it is. A screen that asked would
 * be the second species of collection this shelf is careful not to create.
 *
 * @param footnote what the tile says under its name, and it is never empty: a plate of the window says
 *   how many casillas it is, or what entering it costs once it has been valued; one of the collector's
 *   says the fraction its card in the index says, because that is what it is.
 * @param marks «2 lo busco», on a plate of the collector's that is only here because of them. Null on a
 *   plate of the window, where every casilla is empty and the mark is not what put it on the shelf.
 * @param entryEur what entering costs, for the order that sorts by it — and null on a plate nobody has
 *   valued, which is what makes «por coste de entrar» an order that cannot be the default (§8).
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
)

/**
 * The whole shelf, worded once: the twenty and the collector's plates that hold a mark.
 *
 * Assembled here rather than in the screen for the reason `plateSubject` is: what a tile says is decided
 * by facts from three places — the curated file, the inventory and the price table — and a lazy grid that
 * asked for them per item would ask sixty times per scroll.
 *
 * **A plate of the collector's enters only through a mark** (§8 clause 1). It is not a second index: it
 * is the plate where something the collector is hunting is missing, and the shelf is the one screen that
 * says that about both régimes at once.
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
                // The fraction its own card prints, and the same one: this tile is that plate in a
                // second order, so a second measurement of it is a second thing to keep in step.
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
            // How many casillas it is until it has been valued, and what it costs afterwards. Never
            // `0/12`: a fraction of a plate you are not collecting reads as a reproach for not having
            // started (ADR 0030 §6).
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
 * The shelf in the order it was asked for, and narrowed by what was typed (ADR 0026 §8 clause 4).
 *
 * The search is the name and nothing else, which is what the twenty have: no country facet earns its
 * place — twelve countries with nine of them holding a single plate — and there is nothing else on a
 * tile to match against.
 *
 * **The marked plates lead only in the default order.** «Por coste de entrar» is #282's order and it
 * sorts what has been valued, dearest first, leaving everything with no amount behind it by casillas:
 * asked for that order, a collector is asking about money, and answering with the plates that have none
 * would be answering a different question.
 */
fun showcaseShelf(
    tiles: List<ShowcaseTile>,
    sort: ShowcaseSort,
    query: String,
): List<ShowcaseTile> {
    val narrowed = query.trim().takeIf { it.isNotEmpty() }?.let { needle ->
        tiles.filter { it.name.contains(needle, ignoreCase = true) }
    } ?: tiles
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
