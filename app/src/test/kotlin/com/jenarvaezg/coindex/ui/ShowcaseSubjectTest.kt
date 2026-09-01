package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.PriceKey
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.IndexCover
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.showcasePlate
import com.jenarvaezg.coindex.domain.wishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_786_400_000_000L
private const val DAY = 24L * 60 * 60 * 1_000

/**
 * What a plate of the shelf window says about money, and what its tile says (ADR 0030 §6, §8).
 *
 * Two rules carry this file. **A plate nobody has valued says nothing**, even though its silver floor
 * costs no API call at all — otherwise the collector reads a floor as the price. And a total whose parts
 * arrived on different days is dated by its **oldest**, which is the case #494 opened.
 */
class ShowcaseSubjectTest {
    /**
     * The gate is whether this phone asked, and not whether an amount could be worked out.
     *
     * The spot is two keyless calls and the weight is in every seeded ficha, so `holeValue` would answer
     * for all three casillas of this plate without a single call to Numista. It is not asked: what a
     * floor-only figure says is «entrar cuesta al menos esto», which the collector cannot tell from the
     * price.
     */
    @Test
    fun `a plate nobody has valued has no figure, even where its metal could be priced`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))

        val money = showcaseMoney(plate, state(), book())

        assertNull(money.entry)
        assertEquals(emptyMap(), money.holeCosts)
        // And the plate's own two figures stay absent: it holds nothing, so there is nothing to value.
        assertNull(money.value)
        assertNull(money.cost)
    }

    /** Valued, it adds up the casillas it could price and stamps each one of them. */
    @Test
    fun `once valued the plate says what entering costs and how many casillas that covers`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))

        val money = showcaseMoney(plate, state(), book(listings = listings(), readAt = readAll(NOW)))

        val entry = requireNotNull(money.entry)
        // Three casillas of type 2, priced at 40 in `unc` — the floor of this fixture's silver is lower.
        assertEquals(120.0, entry.eur)
        assertEquals(3, entry.holes)
        assertEquals(3, entry.slots)
        assertEquals(3, money.holeCosts.size)
    }

    /**
     * A total with two ages is dated by its **oldest** (#494).
     *
     * The case is real and this block creates it: a marked casilla of a plate of the window is refreshed
     * by the monthly pass (ADR 0029 §4) while the rest of the plate keeps the date the gesture wrote. A
     * date over a total is a promise about all of it, so the promise is the weakest part of it.
     */
    @Test
    fun `a total whose parts were read on different days carries the oldest of them`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))
        val august = NOW - 30 * DAY

        // The 1991 is the marked one, refreshed today; the other two are August's.
        val reads = readAll(august) + ((PRICED_TYPE to 71) to NOW)

        val money = showcaseMoney(plate, state(), book(listings = listings(), readAt = reads))

        assertEquals(august, requireNotNull(money.entry).readAt)
    }

    /** A casilla with nothing to address a price to adds nothing, and the figure stays a floor. */
    @Test
    fun `a casilla whose issue nobody knows is not counted`() {
        val members = dateRunMembers("mixed", 1_990..1_991) + CollectionCatalogMember(
            id = "mixed-1992",
            label = "1992",
            year = 1_992,
            // No type at all: an unlisted coin has nothing to price (ADR 0029 §1).
            numistaTypeId = null,
        )
        val plate = showcase(catalog("mixed", members))

        val money = showcaseMoney(plate, state(), book(listings = listings(), readAt = readAll(NOW)))

        assertEquals(2, requireNotNull(money.entry).holes)
        // And the figure is a **floor**: what the plate is made of is three casillas, and the amount
        // covers the two it could address a price to. The same shape `PlateCost` has on your own plate.
        assertEquals(3, money.entry?.slots)
    }

    /**
     * Valued is a state of its own, and it is **asked** and not **priced** (ADR 0028 §4).
     *
     * «Numista has no price for this» is a datum rather than a failure, so a plate that was asked about is
     * not a plate nobody has touched — its gesture says «Volver a tasar» instead of offering to buy the
     * same silence again. Two things follow, and the second one is the interesting one:
     *
     * - Numista having no catalogue price does **not** leave the plate without a figure: the silver floor
     *   is sayable precisely because the phone did ask (§4's third state, and `holeValue`'s two prices).
     * - The figure is absent only where there is neither — a type with no metal to weigh — and that is the
     *   one case «Numista no da precio de ninguna de estas casillas» is for.
     */
    @Test
    fun `a plate is valued once it has been asked about, priced or not`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))

        val unpriced = book(listings = listings(), prices = emptyMap(), readAt = readAll(NOW))

        // Asked, with no catalogue price: the metal still answers, because asking is what made it sayable.
        val silverOnly = showcaseMoney(plate, state(), unpriced)
        assertTrue(silverOnly.entryAsked)
        assertEquals(3, requireNotNull(silverOnly.entry).holes)

        // Asked, and nothing at all to say: no catalogue price and no metal on the ficha.
        val nothing = showcaseMoney(plate, metalless(), unpriced)
        assertTrue(nothing.entryAsked)
        assertNull(nothing.entry)

        // And never asked at all, which is what the two above are told apart from.
        val untouched = showcaseMoney(plate, state(), book(listings = listings()))
        assertFalse(untouched.entryAsked)
        assertNull(untouched.entry)
    }

    /**
     * The shelf mixes both populations in one grid, and the marked plates lead the default order
     * (ADR 0030 §8).
     */
    @Test
    fun `the shelf leads with what the collector is looking for, then the fewest casillas`() {
        val window = listOf(
            showcase(dateRun("panda", 2_000..2_010)),
            showcase(dateRun("libertad", 1_990..1_992)),
        )
        val marked = showcase(dateRun("kooka", 2_020..2_021))

        val tiles = showcaseTiles(
            window = window + marked,
            cards = listOf(card("britannia", owned = 3, issued = 42)),
            wishes = listOf(wish(marked.catalog), wish(britannia)),
            state = state(),
            book = book(),
            nowMillis = NOW,
        )

        assertEquals(
            listOf("britannia", "kooka", "libertad", "panda"),
            showcaseShelf(tiles, ShowcaseSort.ByCasillas, query = "").map { it.catalogId },
        )
        // The collector's own plate says its fraction and what put it here; it is not «0/42».
        val mine = tiles.first { it.catalogId == "britannia" }
        assertEquals("3/42", mine.footnote)
        assertEquals("1 lo busco", mine.marks)
        assertTrue(mine.mine)
        // A plate of the window says how many casillas it is until somebody values it.
        assertEquals("3 casillas", tiles.first { it.catalogId == "libertad" }.footnote)
    }

    /**
     * What each tile's hole holds, which is what the shelf draws it from (#556).
     *
     * The screen used to ask `mine` for this, and the tile is the one place that says a tile is drawn
     * from what it *has* and never from a branch on which species it is. The fact is real in both
     * régimes and not a rename of `mine`: a plate of the collector's covers itself with an `IndexCover`,
     * which is an owned coin by construction, and the window is unowned by ADR 0030 §1, so its cover is
     * a member of the catalog and the hole holds nothing of the collector's.
     */
    @Test
    fun `the collector's tile holds an owned coin and one of the window holds none`() {
        val britannia = dateRun("britannia", 1_987..1_990)
        val tiles = showcaseTiles(
            window = listOf(showcase(dateRun("libertad", 1_990..1_992))),
            cards = listOf(card("britannia", owned = 3, issued = 42)),
            wishes = listOf(wish(britannia)),
            state = state(),
            book = book(),
            nowMillis = NOW,
        )

        assertTrue(tiles.first { it.catalogId == "britannia" }.coverOwned)
        assertFalse(tiles.first { it.catalogId == "libertad" }.coverOwned)
    }

    /**
     * «Por coste de entrar» sorts what has been valued and leaves the rest behind it.
     *
     * Asked for that order the collector is asking about money, and answering with the plates that have
     * none would be answering another question — which is also why it cannot be the default: on the day
     * the shelf is born, nothing has an amount at all.
     */
    @Test
    fun `the cost order puts the valued plates first, dearest first, and the rest behind`() {
        val dear = showcase(dateRun("panda", 2_000..2_010))
        val cheap = showcase(dateRun("libertad", 1_990..1_992))
        // 2020 y 2021, cuyos issues de la ficha son el 100 y el 101: los únicos que este `readAt`
        // deja sin leer, y por tanto la única lámina de las tres que nadie ha tasado.
        val unvalued = showcase(dateRun("kooka", 2_020..2_021))

        val tiles = showcaseTiles(
            window = listOf(cheap, dear, unvalued),
            cards = emptyList(),
            wishes = emptyList(),
            state = state(),
            book = book(
                listings = listings(),
                // Only the two valued plates have a read, which is what tells them from the third:
                // without this the fixture priced all three and the order below passed for the wrong
                // reason.
                readAt = readAll(NOW).filterKeys { (_, issueId) -> issueId < KOOKA_FIRST_ISSUE },
            ),
            nowMillis = NOW,
        )

        // The unvalued one has no amount at all, which is what puts it behind both of them.
        assertNull(tiles.first { it.catalogId == "kooka" }.entryEur)
        val order = showcaseShelf(tiles, ShowcaseSort.ByEntryCost, query = "").map { it.catalogId }
        assertEquals(listOf("panda", "libertad", "kooka"), order)
    }

    /** The search is the name, which is all a tile has: no facet earns a chip here (ADR 0026 §8). */
    @Test
    fun `the search narrows by name and says so when nothing matches`() {
        val tiles = showcaseTiles(
            window = listOf(showcase(dateRun("libertad", 1_990..1_992))),
            cards = emptyList(),
            wishes = emptyList(),
            state = state(),
            book = book(),
            nowMillis = NOW,
        )

        assertEquals(1, showcaseShelf(tiles, ShowcaseSort.ByCasillas, "liber").size)
        assertEquals(1, showcaseShelf(tiles, ShowcaseSort.ByCasillas, "LIBERTAD").size)
        assertTrue(showcaseShelf(tiles, ShowcaseSort.ByCasillas, "panda").isEmpty())
    }

    /**
     * This box matches the way the other two do, because it is the same box (#515).
     *
     * It was a bare `contains`: accent-sensitive, and blind to two words in any order. The album's
     * names are written in Spanish by rule (ADR 0021 §4) and typed on a phone keyboard, so «aguila»
     * has to find «Águila» — which is the whole argument `fold` was written under.
     */
    @Test
    fun `the shelf window folds accents and takes the words in any order`() {
        val tiles = showcaseTiles(
            window = listOf(showcase(dateRun("Águila de plata", 1_990..1_992))),
            cards = emptyList(),
            wishes = emptyList(),
            state = state(),
            book = book(),
            nowMillis = NOW,
        )

        assertEquals(1, showcaseShelf(tiles, ShowcaseSort.ByCasillas, "aguila").size)
        assertEquals(1, showcaseShelf(tiles, ShowcaseSort.ByCasillas, "plata aguila").size)
        assertEquals(1, showcaseShelf(tiles, ShowcaseSort.ByCasillas, "  ").size)
        assertTrue(showcaseShelf(tiles, ShowcaseSort.ByCasillas, "aguila oro").isEmpty())
    }
}

private const val PRICED_TYPE = 2

/** The issue the 2020 of the fixture's listing is, which is where «nobody valued this» starts. */
private const val KOOKA_FIRST_ISSUE = 100

/** Every issue the fixture's listing names is worth 40 € in `unc`; nothing else is priced at all. */
private val PRICED: Map<PriceKey, Double> =
    listings().issueIdByTypeAndYear.values.associate { PriceKey(PRICED_TYPE, it, "unc") to 40.0 }

private val SPOT = SilverSpot(eurPerTroyOunce = 30.0, readAtMillis = NOW)

/** The book as a test wants it: the fixture's prices and spot, plus what was asked about and when. */
private fun book(
    listings: IssueListings = IssueListings.EMPTY,
    prices: Map<PriceKey, Double> = PRICED,
    readAt: Map<Pair<Int, Int>, Long> = emptyMap(),
) = PriceBook(prices = prices, spot = SPOT, listings = listings, readAt = readAt)

/** Every issue the fixture's listing names, read at [at]: a plate the gesture valued that day. */
private fun readAll(at: Long): Map<Pair<Int, Int>, Long> =
    listings().issueIdByTypeAndYear.values.associate { (PRICED_TYPE to it) to at }

/** Each year of the fixture's type addressed to an issue, which is what a stored listing answers. */
private fun listings(): IssueListings = IssueListings(
    listedTypeIds = setOf(PRICED_TYPE),
    issueIdByTypeAndYear = (1_990..2_030).associate { year -> (PRICED_TYPE to year) to 70 + year - 1_990 },
)

private fun showcase(catalog: CollectionCatalog): ShowcasePlate = requireNotNull(
    showcasePlate(
        catalog,
        requireNotNull(CatalogAlbums.over(listOf(catalog), emptyList())[catalog]),
        emptySet(),
    ),
)

private fun dateRunMembers(id: String, years: IntRange): List<CollectionCatalogMember> =
    years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = PRICED_TYPE,
        )
    }

private fun dateRun(id: String, years: IntRange): CollectionCatalog =
    catalog(id, dateRunMembers(id, years))

private fun catalog(id: String, members: List<CollectionCatalogMember>): CollectionCatalog =
    CollectionCatalog(
        schemaVersion = 2,
        id = id,
        name = id,
        shortName = id,
        family = id,
        issuerCode = "mexique",
        seriesStatus = SeriesStatus.Closed,
        source = "https://en.numista.com/catalogue/pieces1.html",
        updatedAt = "2026-08-14",
        members = members,
    )

/** The collector's own plate, as the index hands it over: a card with a plate behind it. */
private val britannia = catalog("britannia", dateRunMembers("britannia", 1_990..1_991))

private fun card(id: String, owned: Int, issued: Int): IndexCard.Derived = IndexCard.Derived(
    name = id,
    coverage = CoverageRatio(owned = owned, issued = issued),
    issuer = "Reino Unido",
    collection = DerivedCollection(
        family = id,
        weightMillioz = 1_000,
        finish = null,
        metal = null,
        distinctTypes = owned,
        quantity = owned,
    ),
    plateCatalogId = id,
    cover = IndexCover(typeId = PRICED_TYPE, printedSide = PrintedSide.Reverse),
)

/** One mark over the first casilla of a catalog, resolved the way the annex resolves it. */
private fun wish(catalog: CollectionCatalog): WishedSlot {
    val member = catalog.members.first()
    return WishedSlot(
        wish = Wish(key = requireNotNull(member.wishKey()), markedAt = NOW),
        catalog = catalog,
        member = member,
    )
}

/** The same collection with no metal on the ficha: nothing left for a hole to be valued by. */
private fun metalless(): CollectionState = CollectionState(
    collection = AssembledCollection(
        typeMeta = mapOf(PRICED_TYPE to TypeMeta(id = PRICED_TYPE, issuerCode = "mexique")),
    ),
)

private fun state(items: List<CollectedItem> = emptyList()): CollectionState = CollectionState(
    collection = AssembledCollection(
        items = items,
        typeMeta = mapOf(
            // One troy ounce of fine silver, so the metal alone could answer for every casilla of
            // these plates — which is the whole point of the first test in this file.
            PRICED_TYPE to TypeMeta(
                id = PRICED_TYPE,
                issuerCode = "mexique",
                issuerName = "Mexique",
                weightOz = 1.0,
                fineness = 0.999,
                metal = Metal.Silver,
            ),
        ),
    ),
)
