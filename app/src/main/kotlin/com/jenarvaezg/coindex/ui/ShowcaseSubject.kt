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
 * @param coverOwned whether the coin in the tile's hole is one the collector has, which is what the hole
 *   is drawn from (#556). It is the fact the screen used to get by asking [mine], and asking was the one
 *   thing this file says a tile must not do: with the ghost on every window tile the grid read as sorted
 *   by ownership, which is what §8 clause 1 mixed the two populations to avoid. A plate of the window
 *   owns nothing by §1, so its cover is a catalog member; a plate of the collector's covers itself with
 *   an [IndexCover], which is an owned coin by construction.
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
                // `IndexCover` is «the owned coin shown inside one index card's die-cut hole»: this
                // tile covers itself with the same coin its card does, and that coin is in the
                // collection.
                coverOwned = true,
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
            // The window is curated and unowned (§1), so its cover is a member of the catalog and not
            // a piece: the hole holds no coin of the collector's, whatever the plate holds.
            coverOwned = false,
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
 * **It matches the way the other two boxes do** (#515): the same [fold] and [matchesQuery] the index
 * and Monedas run on, so «britannia» finds «Britannia» and «panda plata» finds «Panda de plata». The
 * three boxes are one drawing, and this one used to be a bare `contains` — accent-sensitive, and
 * blind to two words in any order — which is a difference nothing on screen could have declared.
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
