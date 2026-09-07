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
 * The snapshot read against the curated files, and nothing that arrives by another door.
 *
 * The inner half of a [ScreenReading], and it is a value of its own for one reason: **the collection
 * changes far less often than the market does.** A price lands row by row while a pass runs, and a
 * plate resolved against the album, the shelf window and the name of a card have nothing to do with
 * any of it — so they hang here, where an arriving price cannot reach them, exactly as they hung on
 * `remember(state.collection)` before this ticket (#218, #537).
 *
 * Everything here is `by lazy` and the instance is the memo: [CoindexViewModel.reading] keeps the
 * one it built until the assembly or the curated files move, which is what `equals` over these two
 * fields decides.
 */
data class CollectionReading(
    /** The curated files, which are constant for the life of the process (#217). */
    val curation: Curation,
    /** The assembly the snapshot was read into, with the photographs and the ficha dates. */
    val collection: CollectionState,
) {
    /**
     * The curated catalogs the country and year axes walk (ADR 0026 §9).
     *
     * The index of cards is not enough: a member's country is not the card's (#170), and the year
     * axis needs every measurable slot, not only the ones that opened a plate today.
     */
    val catalogs: List<CollectionCatalog> get() = curation.catalogs

    /**
     * Every name a curated file claims, so the one name a collector types cannot repeat one
     * (ADR 0021 §4).
     */
    val curatedNames: Set<String> by lazy { curation.titles.curatedNames() }

    /**
     * The shelf window: the plates of the curated shelf this collector holds nothing of (ADR 0030 §1).
     *
     * One crossing and not two. It was computed in the ViewModel **and** in the root composable, with
     * a comment on each side promising the two could not disagree; what makes that true now is that
     * there is one of them.
     */
    val showcase: List<ShowcasePlate> by lazy {
        showcasePlates(
            catalogs = curation.catalogs,
            albums = collection.albums,
            evidencedCatalogIds = collection.evidencedCatalogIds,
        )
    }

    /**
     * The **card-sized** name of one curated catalog, for the masthead of its plate.
     *
     * `name` is the editorial scope and runs to 200 characters; the plate prints it whole two lines
     * below, so a masthead carrying it too said the same sentence twice on one screen (#511).
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
     *
     * The card and not its subject, because the screen needs the card itself twice over: the printer
     * takes one to draw a sheet (ADR 0021 §9), and a box's upkeep addresses the box behind it
     * (ADR 0021 §11).
     */
    fun derivedCard(key: VariantKey): IndexCard.Derived? = collection.piecesCardFor(key)

    /** The card of one of the collector's boxes, or null once it has been undone. */
    fun boxCard(groupingId: Long): IndexCard.Box? = collection.piecesCardForBox(groupingId)

    /** One coin as the sheet of #508 draws it, which is a walk of the inventory and of the index. */
    fun coin(typeId: Int): CoinRow = coinRowOf(collection, typeId)

    /** When this phone brought one ficha, which is what a card says «hace ocho meses» from (#185). */
    fun fichaFetchedAt(typeId: Int): Long? = collection.fichaFetchedAt[typeId]

    /**
     * The ficha of one coin on Numista, for **every** label of the app that leaves for it (#508).
     *
     * One rule and not four: the URL Numista itself handed over, in the language the ficha was asked
     * in (`TypeMeta.numistaUrl`), and the type's own address where this phone holds no ficha at all.
     */
    fun numistaUrl(typeId: Int): String =
        collection.typeMeta[typeId]?.numistaUrl ?: numistaTypeUrl(typeId)

    /**
     * One plate, resolved against this assembly (#537, #539).
     *
     * Resolved here and not by the screen for the reason every other field is: it divides the album
     * the assembly already built, and a screen recomposes for reasons — a scroll, an export in
     * flight — that leave the plate unchanged.
     */
    fun plate(catalogId: String): PlateResult = resolvePlate(collection, curation, catalogId)

    /**
     * One plate of the shelf window by id, or null if this catalog is not one (ADR 0030 §1).
     *
     * Resolved from the catalog rather than looked up in [showcase], which is the order the gesture
     * of «Tasar esta lámina» has always asked in: what it needs is this plate's own holes, and the
     * window is the shelf's reading of the same fact.
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
 * Everything a screen reads and nobody stores: the collection's own reading, plus the market and the
 * marks.
 *
 * The app has exactly two kinds of value. What is **kept** is [UiState] — the snapshot, the price
 * book, the marks, the shelves, what is in flight — and what is **read** is this, which is a
 * function of it and of the curated shelf (#217). Everything here used to be written in the body of
 * the root composable, and being there broke it twice over, which is the same pair of failures
 * [PlateFinance] was pulled out for one ticket earlier: nothing could be answered without a device,
 * and a value rebuilt in a composable body is a **new object on every recomposition**, so the
 * `remember` of whatever screen received it never hit.
 *
 * **The instance is the memo.** The fields below are `by lazy`, so building one costs seven
 * references and walks nothing; what makes a walk happen once is that [CoindexViewModel.reading]
 * hands back the same instance until one of the readings this is made of actually changes — which is
 * what the constructor's own `equals` decides. That is why the inputs are the *slices* the
 * derivations read and never the whole [UiState]: a ficha being refreshed, a chooser opening or a
 * plate being valued moves none of them, and the pass's running count moves neither [settled] nor
 * [waiting]. A screen keys its own `remember` on this one value instead of listing four fields of
 * the state and hoping the list stays right.
 *
 * **Two values and not one**, and the seam is how often each side moves: what only the assembly and
 * the curated files decide is [of]'s, so a price landing mid-pass — which happens row by row — leaves
 * the shelf window, the plates and the names exactly where they were.
 *
 * Nothing here is a composable and nothing here touches Android: every field is answerable in a JVM
 * test.
 *
 * @param of the snapshot read against the curated files, which is the half a price cannot move.
 * @param book every price and the spot (ADR 0028), whole rather than sliced, so two surfaces cannot
 *   disagree about *when* a figure is from.
 * @param marks the wish table exactly as it is stored (ADR 0029 §3): nothing that reads inventory
 *   joins it, and [livingWishes] is the one crossing.
 * @param settled whether the market has finished arriving, which gates every amount (ADR 0028 §7).
 * @param waiting whether that absence is worth a line (#519). The pass's own reading and not a
 *   second guess at [settled].
 * @param loading whether the first snapshot is still being read, which is what keeps the sewn edge
 *   absent rather than zero (#418).
 * @param pricesArrivedAt when this price book reached the phone, which is the «now» every age on
 *   screen is measured against (ADR 0030 §4). Stamped once per arrival and never per frame: what it
 *   dates is a figure that never expires, so it must not move while the collector reads it.
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
     *
     * The crossing of two things that change at different times — the table and the inventory — and
     * therefore never a third field of the state: a stored reading is the one that could disagree
     * with both. Read by the annex, by the door that counts it, by «Este teléfono», by the printer
     * and by the pass that prices it, all from here.
     */
    val livingWishes: List<WishedSlot> by lazy {
        wishedSlots(wishes = marks, catalogs = of.catalogs, items = of.collection.items)
    }

    /**
     * The keys as the table holds them, dead marks included, which is what a plate draws with.
     *
     * A mark is only ever painted on an empty casilla, so the album is what says whether it shows
     * (ADR 0029 §2) — and a plate whose coin was sold shows the mark again without the table being
     * written to.
     */
    val wishedKeys: Set<WishKey> by lazy { marks.mapTo(mutableSetOf()) { it.key } }

    /**
     * The same marks as rows, for the door of the index (#520).
     *
     * It draws the first few of them, so it needs the type and the face each casilla rests on. Built
     * off [livingWishes] and not off the table, so the row and the list it opens cannot disagree
     * about what is marked — and with no prices, which are the one thing a door has no room for.
     */
    val wishedRows: List<DrawnWish> by lazy { wishSubject(livingWishes).rows }

    /**
     * The annex itself: the marked casillas with what each of them would cost (ADR 0029).
     *
     * The same gate as every other amount in the app: while the market has not landed there is no
     * price to say (ADR 0028 §7), and then no row carries one.
     */
    val wishAnnex: WishSubject by lazy {
        wishSubject(
            slots = livingWishes,
            costs = if (!settled) emptyMap() else wishCosts(livingWishes, collection, book),
        )
    }

    /**
     * What the marks cost a month, for the one screen that prints it: «Este teléfono», where the
     * budget already lives (ADR 0029 §5).
     *
     * The other place the figure is said is the gesture, and that one is a constant sentence —
     * «+2 consultas al mes» per casilla — because it is a promise and not a total.
     */
    val wishCalls: Int by lazy { wishCallsPerMonth(livingWishes) }

    /**
     * Everything «Las cifras» draws, and the weight the bottom bar carries on every screen.
     *
     * It walks the whole inventory, so it is read once per collection and per price book and never
     * per frame — which is what the instance being the memo buys.
     */
    val figures: FiguresSubject by lazy {
        figuresSubject(state = collection, book = book, settled = settled, waiting = waiting)
    }

    /**
     * One census for the sewn edge of every root (#400).
     *
     * Collections from the index, pieces and types from the same [figures] «La materia» already uses
     * — so the three tabs cannot invent three totals, and the HierarchyBar's type count is the same
     * number. Absent while still reading (#418): zeros here would claim the collection is empty
     * before the snapshot lands.
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
     * Every tile of «Explorar», the twenty of the window and the collector's plates that hold a mark
     * (ADR 0030 §8).
     *
     * Worded once per crossing of the four things a tile is made of — the curated files, the
     * inventory, the marks and the price table — and never once per scroll.
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
     * What one coin is worth, for the three surfaces that print money about a single piece — the
     * ficha, the header of its plate and «Las cifras» — so none of them can disagree about it.
     *
     * The market has to have landed: while it has not there is no amount to give anybody
     * (ADR 0028 §7).
     */
    fun coinValue(typeId: Int): CoinValue? =
        if (!settled) null else coinValue(typeId, collection, book)

    /**
     * What one plate's own coins are worth, for the header the printer draws (#228).
     *
     * The same gate as every amount on screen, said once for paper too: a total at 60 % is false on
     * paper as well, and paper cannot be taken back. The export's money switch is **not** here — that
     * one is the notebook's and is answered where the sheet is configured (ADR 0021 §13).
     */
    fun plateValue(album: CollectionCatalogAlbum): PlateValue? =
        if (!settled) null else plateValue(album, collection, book)

    /**
     * Everything a plate says about money and what tasar it would spend (#541).
     *
     * The two commands are the screen's half of it and the only thing this cannot answer alone: what
     * a press does is start a pass and say out loud when it bought nothing.
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
 * No curated files at all, for the one state that has none: the collection that could not be read.
 *
 * A [Curation] over three empty lists rather than a nullable field, so nothing downstream has to ask
 * whether there is a shelf. What a reading of it answers is what is true then — no catalog, no name,
 * no plate — and it answers instead of raising the parse error a second time, which is what an
 * unreadable asset does on every touch of `repository.curation`: the screen that has to draw at that
 * moment is `fatalError`'s own.
 */
val NO_CURATION: Curation = Curation(catalogs = emptyList())

/**
 * The reading of one state, against the curated files it was assembled with.
 *
 * The valuation arrives as its two answers and not whole, and that is the difference between a
 * reading that is rebuilt once a month and one rebuilt eighteen times per pass: the status carries a
 * running count that moves every twenty-five issues (`VALUATION_PROGRESS_EVERY`), and nothing here
 * reads it.
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
