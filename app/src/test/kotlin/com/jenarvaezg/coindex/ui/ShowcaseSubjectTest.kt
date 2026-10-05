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
 * What a plate of the shelf window says about money, and what its tile says (ADR 0030 §6, §8). A
 * plate nobody has valued says nothing, though its silver floor costs no call; a total read on
 * different days is dated by its oldest part (#494).
 */
class ShowcaseSubjectTest {
    /** A floor-only figure would read as the price, so the gate is whether this phone asked. */
    @Test
    fun `a plate nobody has valued has no figure, even where its metal could be priced`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))

        val money = showcaseMoney(plate, state(), book())

        assertNull(money.entry)
        assertEquals(emptyMap(), money.holeCosts)
        // The plate's own two figures stay absent: it holds nothing.
        assertNull(money.value)
        assertNull(money.cost)
    }

    @Test
    fun `once valued the plate says what entering costs and how many casillas that covers`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))

        val money = showcaseMoney(plate, state(), book(listings = listings(), readAt = readAll(NOW)))

        val entry = requireNotNull(money.entry)
        // Three casillas at 40 in `unc`; the fixture's silver floor is lower.
        assertEquals(120.0, entry.eur)
        assertEquals(3, entry.holes)
        assertEquals(3, entry.slots)
        assertEquals(3, money.holeCosts.size)
    }

    /**
     * A marked casilla is refreshed by the monthly pass (ADR 0029 §4) while the rest of the plate
     * keeps the gesture's date (#494).
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
        // A floor: the plate has three casillas and the amount covers two, as in `PlateCost`.
        assertEquals(3, money.entry?.slots)
    }

    /**
     * Valued means asked, not priced (ADR 0028 §4): no catalogue price is a datum, and once asked
     * the silver floor can be said. Only a type with no metal leaves the plate without a figure.
     */
    @Test
    fun `a plate is valued once it has been asked about, priced or not`() {
        val plate = showcase(dateRun("libertad", 1_990..1_992))

        val unpriced = book(listings = listings(), prices = emptyMap(), readAt = readAll(NOW))

        // Asked, with no catalogue price: the metal answers.
        val silverOnly = showcaseMoney(plate, state(), unpriced)
        assertTrue(silverOnly.entryAsked)
        assertEquals(3, requireNotNull(silverOnly.entry).holes)

        // Asked, and nothing at all to say: no catalogue price and no metal on the ficha.
        val nothing = showcaseMoney(plate, metalless(), unpriced)
        assertTrue(nothing.entryAsked)
        assertNull(nothing.entry)

        // Never asked.
        val untouched = showcaseMoney(plate, state(), book(listings = listings()))
        assertFalse(untouched.entryAsked)
        assertNull(untouched.entry)
    }

    /** Both populations share one grid (ADR 0030 §8). */
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
        // The collector's plate says its fraction and why it is on the shelf.
        val mine = tiles.first { it.catalogId == "britannia" }
        assertEquals("3/42", mine.footnote)
        assertEquals("1 lo busco", mine.marks)
        assertTrue(mine.mine)
        // A plate of the window says how many casillas it is until somebody values it.
        assertEquals("3 casillas", tiles.first { it.catalogId == "libertad" }.footnote)
    }

    /**
     * A tile is drawn from what its hole holds, not from `mine` (#556): an `IndexCover` is an owned
     * coin, and the window is unowned (ADR 0030 §1).
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

    /** Not the default order: on a new shelf nothing has an amount yet. */
    @Test
    fun `the cost order puts the valued plates first, dearest first, and the rest behind`() {
        val dear = showcase(dateRun("panda", 2_000..2_010))
        val cheap = showcase(dateRun("libertad", 1_990..1_992))
        // Issues 100 and 101, the only ones `readAt` leaves unread: the plate nobody valued.
        val unvalued = showcase(dateRun("kooka", 2_020..2_021))

        val tiles = showcaseTiles(
            window = listOf(cheap, dear, unvalued),
            cards = emptyList(),
            wishes = emptyList(),
            state = state(),
            book = book(
                listings = listings(),
                // Only the valued plates have a read, or the order below would pass by accident.
                readAt = readAll(NOW).filterKeys { (_, issueId) -> issueId < KOOKA_FIRST_ISSUE },
            ),
            nowMillis = NOW,
        )

        // No amount at all, so it goes last.
        assertNull(tiles.first { it.catalogId == "kooka" }.entryEur)
        val order = showcaseShelf(tiles, ShowcaseSort.ByEntryCost, query = "").map { it.catalogId }
        assertEquals(listOf("panda", "libertad", "kooka"), order)
    }

    /** A tile has only its name to search; no facet earns a chip (ADR 0026 §8). */
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
     * The same `fold` as the other two boxes (#515): names are Spanish (ADR 0021 §4) and typed on a
     * phone keyboard, so «aguila» has to find «Águila».
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

/** The 2020 issue in the fixture's listing, where the unvalued plate starts. */
private const val KOOKA_FIRST_ISSUE = 100

/** Every listed issue is 40 € in `unc`; nothing else is priced. */
private val PRICED: Map<PriceKey, Double> =
    listings().issueIdByTypeAndYear.values.associate { PriceKey(PRICED_TYPE, it, "unc") to 40.0 }

private val SPOT = SilverSpot(eurPerTroyOunce = 30.0, readAtMillis = NOW)

/** The fixture's prices and spot, plus what was asked about and when. */
private fun book(
    listings: IssueListings = IssueListings.EMPTY,
    prices: Map<PriceKey, Double> = PRICED,
    readAt: Map<Pair<Int, Int>, Long> = emptyMap(),
) = PriceBook(prices = prices, spot = SPOT, listings = listings, readAt = readAt)

/** Every issue the fixture's listing names, read at [at]: a plate the gesture valued that day. */
private fun readAll(at: Long): Map<Pair<Int, Int>, Long> =
    listings().issueIdByTypeAndYear.values.associate { (PRICED_TYPE to it) to at }

/** Each year of the fixture's type addressed to an issue, as a stored listing answers. */
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
            // One troy ounce of fine silver: the metal alone could price every casilla.
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
