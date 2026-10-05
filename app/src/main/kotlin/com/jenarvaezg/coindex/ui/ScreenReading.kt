package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.wishCallsPerMonth
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.VariantKey
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.showcasePlate
import com.jenarvaezg.coindex.domain.showcasePlates
import com.jenarvaezg.coindex.domain.wishedSlots
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.shelf.coinRowOf

/**
 * The snapshot read against the curated files: the half of a [ScreenReading] that prices don't
 * affect (#218, #537). Prices land row by row during a pass, so plates, the shelf window and card
 * names live here, out of their reach.
 *
 * Everything is `by lazy` and the instance is the memo: [CoindexViewModel.reading] keeps it until
 * `equals` over the two fields says otherwise.
 */
data class CollectionReading(
    /** The curated files, which are constant for the life of the process (#217). */
    val curation: Curation,
    /** The assembly the snapshot was read into, with the photographs and the ficha dates. */
    val collection: CollectionState,
) {
    /**
     * The curated catalogs the country and year axes walk (ADR 0026 §9), rather than the cards: a
     * member's country isn't the card's (#170), and the year axis needs every measurable slot.
     */
    val catalogs: List<CollectionCatalog> get() = curation.catalogs

    /**
     * Every name a curated file claims, so the one name a collector types cannot repeat one
     * (ADR 0021 §4).
     */
    val curatedNames: Set<String> by lazy { curation.titles.curatedNames() }

    /** The shelf window: the curated plates this collector holds nothing of (ADR 0030 §1). */
    val showcase: List<ShowcasePlate> by lazy {
        showcasePlates(
            catalogs = curation.catalogs,
            albums = collection.albums,
            evidencedCatalogIds = collection.evidencedCatalogIds,
        )
    }

    /**
     * The short name of one curated catalog, for its plate's masthead: the plate prints the full
     * `name` just below (#511).
     */
    fun catalogName(catalogId: String?): String? = catalogOf(catalogId)?.shortName

    /** The name of a derived collection, off the card the route opened (#22, #565). */
    fun derivedName(key: VariantKey): String? = derivedCard(key)?.name

    /** The name of one of the collector's boxes, which is whatever they typed (ADR 0021 §11). */
    fun boxName(groupingId: Long): String? = boxCard(groupingId)?.name

    /** The pieces one card holds, as the screen one tap in draws them. */
    fun pieces(card: IndexCard): PiecesSubject = piecesSubject(collection, card)

    /**
     * The card a pieces route points at, or null once it no longer exists (see [piecesCardFor]).
     * The card rather than its subject, because the printer draws a sheet from it (ADR 0021 §9).
     */
    fun derivedCard(key: VariantKey): IndexCard.Derived? = collection.piecesCardFor(key)

    /** The card of one of the collector's boxes, or null once it has been undone. */
    fun boxCard(groupingId: Long): IndexCard.Box? = collection.piecesCardForBox(groupingId)

    /** One coin as the sheet of #508 draws it; walks the inventory and the index. */
    fun coin(typeId: Int): CoinRow = coinRowOf(collection, typeId)

    /** When this phone fetched one ficha, for the age a card shows (#185). */
    fun fichaFetchedAt(typeId: Int): Long? = collection.fichaFetchedAt[typeId]

    /**
     * A coin's Numista page, for every link that leads there (#508): the URL Numista handed over,
     * in the ficha's language (`TypeMeta.numistaUrl`), or the type's address with no ficha.
     */
    fun numistaUrl(typeId: Int): String =
        collection.typeMeta[typeId]?.numistaUrl ?: numistaTypeUrl(typeId)

    /**
     * One plate, resolved against this assembly (#537, #539) rather than by the screen, which
     * recomposes for scrolls and exports that leave the plate unchanged.
     */
    fun plate(catalogId: String): PlateResult = resolvePlate(collection, curation, catalogId)

    /**
     * One plate of the shelf window by id, or null if this catalog isn't one (ADR 0030 §1).
     * Resolved from the catalog rather than looked up in [showcase].
     */
    fun showcasePlateOf(catalogId: String): ShowcasePlate? {
        val catalog = catalogOf(catalogId) ?: return null
        val album = collection.albums[catalog] ?: return null
        return showcasePlate(catalog, album, collection.evidencedCatalogIds)
    }

    /** One curated catalog by the id a route carries, which is how every id on screen is spelled. */
    private fun catalogOf(catalogId: String?): CollectionCatalog? =
        catalogs.firstOrNull { it.id == catalogId }
}

/**
 * Everything a screen reads and nobody stores: the collection's reading, plus the market and the
 * marks. What is kept is [UiState]; this is a function of it and of the curated shelf (#217).
 * Plain Kotlin, testable on the JVM, and not rebuilt in a composable body on every recomposition.
 *
 * The instance is the memo: fields are `by lazy`, and [CoindexViewModel.reading] returns the same
 * instance while the constructor's `equals` holds. So the inputs are the slices the derivations
 * read, never the whole [UiState]: a ficha refresh, a chooser or a running pass's count moves none
 * of them. Screens key `remember` on this one value.
 *
 * Split in two by how often each side changes: what only the assembly and curated files decide is
 * [of]'s, so a price landing mid-pass leaves the plates, the names and the shelf window alone.
 *
 * @param of the snapshot read against the curated files, the half prices don't affect.
 * @param book every price and the spot (ADR 0028), whole, so two surfaces agree on when a figure
 *   is from.
 * @param marks the wish table as stored (ADR 0029 §3); [livingWishes] is where it meets the
 *   inventory.
 * @param settled whether the market has finished arriving, which gates every amount (ADR 0028 §7).
 * @param waiting whether that absence gets a line (#519), as the pass reads it.
 * @param loading whether the first snapshot is still being read, so the sewn edge is absent rather
 *   than zero (#418).
 * @param pricesArrivedAt when this price book reached the phone: the «now» every age on screen is
 *   measured against (ADR 0030 §4). Stamped once per arrival, so it doesn't move while read.
 */
data class ScreenReading(
    val of: CollectionReading,
    val book: PriceBook,
    val marks: List<Wish>,
    val settled: Boolean,
    val waiting: Boolean,
    val loading: Boolean,
    val pricesArrivedAt: Long,
) {
    /** The assembly under this reading, for the screens that draw a snapshot and not a derivation. */
    val collection: CollectionState get() = of.collection
    val catalogs: List<CollectionCatalog> get() = of.catalogs
    val curatedNames: Set<String> get() = of.curatedNames
    val showcase: List<ShowcasePlate> get() = of.showcase

    /**
     * The marked casillas that are still empty, resolved against the curated shelf (ADR 0029 §2).
     * Derived, never stored, since it crosses the wish table and the inventory. The annex, its
     * door, «Este teléfono», the printer and the pass all read it from here.
     */
    val livingWishes: List<WishedSlot> by lazy {
        wishedSlots(wishes = marks, catalogs = of.catalogs, items = of.collection.items)
    }

    /**
     * The keys as the table holds them, dead marks included, for the plate: the album decides
     * whether a mark shows (ADR 0029 §2), so a sold coin brings its mark back with no write.
     */
    val wishedKeys: Set<WishKey> by lazy { marks.mapTo(mutableSetOf()) { it.key } }

    /**
     * The same marks as rows for the index's door, which draws the first few (#520). Built from
     * [livingWishes] so the door and the list agree; no prices.
     */
    val wishedRows: List<DrawnWish> by lazy { wishSubject(livingWishes).rows }

    /**
     * The annex: the marked casillas with what each would cost (ADR 0029). No prices until the
     * market lands (ADR 0028 §7).
     */
    val wishAnnex: WishSubject by lazy {
        wishSubject(
            slots = livingWishes,
            costs = if (!settled) emptyMap() else wishCosts(livingWishes, collection, book),
        )
    }

    /**
     * What the marks cost a month, printed only on «Este teléfono», with the budget (ADR 0029 §5).
     * The marking gesture says a fixed «+2 consultas al mes» per casilla instead.
     */
    val wishCalls: Int by lazy { wishCallsPerMonth(livingWishes) }

    /**
     * Everything «Las cifras» draws, and the bottom bar's weight. Walks the whole inventory, once
     * per instance.
     */
    val figures: FiguresSubject by lazy {
        figuresSubject(state = collection, book = book, settled = settled, waiting = waiting)
    }

    /**
     * The sewn edge's census for every root (#400): collections from the index, pieces and types
     * from [figures], so the roots and the bottom bar agree. Null while loading (#418).
     */
    val sewnEdge: SewnEdgeCounts? by lazy {
        if (loading) {
            null
        } else {
            SewnEdgeCounts(
                collections = collection.index.size,
                pieces = figures.figures.pieces,
                types = figures.figures.types,
            )
        }
    }

    /**
     * The tiles of «Explorar»: the shelf window's plates and the collector's plates that hold a
     * mark (ADR 0030 §8).
     */
    val tiles: List<ShowcaseTile> by lazy {
        showcaseTiles(
            window = showcase,
            cards = collection.index,
            wishes = livingWishes,
            state = collection,
            book = book,
            nowMillis = pricesArrivedAt,
        )
    }

    fun catalogName(catalogId: String?): String? = of.catalogName(catalogId)

    fun derivedName(key: VariantKey): String? = of.derivedName(key)

    fun boxName(groupingId: Long): String? = of.boxName(groupingId)

    fun pieces(card: IndexCard): PiecesSubject = of.pieces(card)

    fun derivedCard(key: VariantKey): IndexCard.Derived? = of.derivedCard(key)

    fun boxCard(groupingId: Long): IndexCard.Box? = of.boxCard(groupingId)

    fun coin(typeId: Int): CoinRow = of.coin(typeId)

    fun fichaFetchedAt(typeId: Int): Long? = of.fichaFetchedAt(typeId)

    fun numistaUrl(typeId: Int): String = of.numistaUrl(typeId)

    fun plate(catalogId: String): PlateResult = of.plate(catalogId)

    fun showcasePlateOf(catalogId: String): ShowcasePlate? = of.showcasePlateOf(catalogId)

    /**
     * What one coin is worth, shared by every surface that prices a single piece. Null until the
     * market lands (ADR 0028 §7).
     */
    fun coinValue(typeId: Int): CoinValue? =
        if (!settled) null else coinValue(typeId, collection, book)

    /**
     * What one plate's own coins are worth, for the header the printer draws (#228). Null until the
     * market lands, as on screen. The export's money switch is applied by the caller
     * (ADR 0021 §13).
     */
    fun plateValue(album: CollectionCatalogAlbum): PlateValue? =
        if (!settled) null else plateValue(album, collection, book)

    /**
     * A plate's money and what tasar it would spend (#541). The screen supplies the two commands:
     * start a pass, and report a press that had nothing to ask.
     */
    fun plateFinance(
        onValue: (catalogId: String) -> Unit,
        onMessage: (UiNotice) -> Unit,
    ): PlateFinance = PlateFinance(
        showcase = showcase,
        state = collection,
        book = book,
        settled = settled,
        waiting = waiting,
        wished = wishedKeys,
        nowMillis = pricesArrivedAt,
        onValue = onValue,
        onMessage = onMessage,
    )
}

/**
 * An empty [Curation] for when the curated files couldn't be read. Readings then answer «nothing»
 * instead of raising the parse error again on every touch of `repository.curation` while
 * `fatalError` is on screen.
 */
val NO_CURATION: Curation = Curation(catalogs = emptyList())

/**
 * The reading of one state. The valuation arrives as its two booleans, not whole: its running
 * count (`VALUATION_PROGRESS_EVERY`) would rebuild the reading many times per pass.
 */
fun UiState.reading(of: CollectionReading): ScreenReading = ScreenReading(
    of = of,
    book = prices,
    marks = wishes,
    settled = valuation.settled,
    waiting = valuation.waiting,
    loading = loading,
    pricesArrivedAt = pricesArrivedAt,
)
