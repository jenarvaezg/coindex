package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.PriceKey
import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.showcasePlate
import com.jenarvaezg.coindex.domain.wishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_786_400_000_000L

/** The type every casilla of these fixtures is, and the only one with a price on the phone. */
private const val PRICED_TYPE = 2

/**
 * Which of the three régimes a plate's money comes from, and what tasar would spend (ADR 0030 §3).
 * Each régime's own arithmetic is in `ShowcaseSubjectTest` and `FiguresSubjectTest`.
 */
class PlateFinanceTest {
    /**
     * Its prices come from its own gesture (ADR 0030 §3), so the collection's pass doesn't gate it.
     */
    @Test
    fun `a plate of the shelf window says what entering costs, pass or no pass`() {
        val plate = catalog("libertad", 1_990..1_992)
        val finance = finance(
            showcase = listOf(showcase(plate)),
            book = valuedBook(),
            pass = ValuationStatus(wanted = 4, missing = 4),
        )

        val money = finance.money(resolved(plate, mine = false))

        assertEquals(120.0, money.entry?.eur)
        assertEquals(3, money.entry?.holes)
        assertTrue(money.entryAsked)
        // Hole stamps use the same field in both régimes; only the header's figure differs.
        assertEquals(
            mapOf("libertad-1990" to 40.0, "libertad-1991" to 40.0, "libertad-1992" to 40.0),
            money.holeCosts,
        )
        // The collector's two figures belong to the other régime.
        assertNull(money.value)
        assertNull(money.cost)
        assertFalse(money.waiting)
    }

    @Test
    fun `the collector's plate withdraws all three readings while the market is arriving`() {
        val plate = catalog("britannia", 1_990..1_991)
        val finance = finance(
            book = valuedBook(),
            pass = ValuationStatus(wanted = 4, missing = 2, held = ValuationRefusal.NoApiKey),
        )

        val money = finance.money(resolved(plate, mine = true))

        assertNull(money.value)
        assertNull(money.cost)
        assertNull(money.entry)
        assertEquals(emptyMap(), money.holeCosts)
        assertTrue(money.waiting)
    }

    /** No `entry`: a plate that holds a coin is not seen from outside (ADR 0030 §6). */
    @Test
    fun `with the market landed the collector's plate values what it holds and prices its holes`() {
        val plate = catalog("britannia", 1_990..1_991)
        val finance = finance(
            state = state(listOf(item(id = 1, issueId = 70))),
            book = valuedBook(),
        )

        val money = finance.money(resolved(plate, mine = true))

        assertEquals(1, money.value?.pieces)
        assertEquals(40.0, money.value?.eur)
        assertEquals(1, money.cost?.holes)
        assertEquals(40.0, money.cost?.eur)
        assertEquals(1, money.holeCosts.size)
        assertNull(money.entry)
        assertFalse(money.waiting)
    }

    /**
     * Looked up by catalog id, not by `mine`, so an uncurated plate never shows an empty `entry`.
     */
    @Test
    fun `a plate the window does not hold is the collector's, whatever the resolution says`() {
        val plate = catalog("britannia", 1_990..1_991)
        val finance = finance(showcase = emptyList(), book = valuedBook())

        val money = finance.money(resolved(plate, mine = false))

        assertNull(money.entry)
        assertEquals(1, money.cost?.holes)
    }

    /**
     * Marks are priced on the collector's plate (ADR 0029 §4). Only beyond the ADR 0028 §1
     * threshold does that show, since within it every hole is priced anyway.
     */
    @Test
    fun `a marked casilla is priced on a plate whose holes are out of reach`() {
        val plate = catalog("britannia", 1_990..2_010)
        val marked = requireNotNull(plate.members.last().wishKey())

        val unmarked = finance(book = valuedBook()).money(resolved(plate, mine = true))
        val money = finance(book = valuedBook(), wished = setOf(marked)).money(resolved(plate, mine = true))

        assertEquals(emptyMap(), unmarked.holeCosts)
        assertEquals(mapOf("britannia-2010" to 40.0), money.holeCosts)
        // Still no cost of closing: one priced hole doesn't close the plate.
        assertNull(money.cost)
    }

    /** The pass's own arithmetic over this plate's holes. */
    @Test
    fun `the gesture names what tasar this plate would spend`() {
        val plate = catalog("libertad", 1_990..1_992)
        val finance = finance(showcase = listOf(showcase(plate)))

        // One `/prices` per casilla and no `/issues`: the curated file names every issue.
        assertEquals(3, finance.calls(resolved(plate, mine = false)))
    }

    @Test
    fun `the collector's own plate spends nothing, because it has no gesture`() {
        val plate = catalog("britannia", 1_990..1_991)

        assertEquals(0, finance().calls(resolved(plate, mine = true)))
    }

    @Test
    fun `a plate valued today asks for nothing more`() {
        val plate = catalog("libertad", 1_990..1_992)
        val finance = finance(showcase = listOf(showcase(plate)), book = valuedBook())

        assertEquals(0, finance.calls(resolved(plate, mine = false)))
    }

    @Test
    fun `pressing the gesture values this plate and nothing else`() {
        val plate = catalog("libertad", 1_990..1_992)
        val valued = mutableListOf<String>()
        val notices = mutableListOf<UiNotice>()
        val finance = finance(
            showcase = listOf(showcase(plate)),
            onValue = { valued += it },
            onMessage = { notices += it },
        )

        finance.press(resolved(plate, mine = false))

        assertEquals(listOf("libertad"), valued)
        assertEquals(emptyList(), notices)
    }

    /** A silent press would read as a broken button (ADR 0028 §5). */
    @Test
    fun `pressing a plate that is already fresh says so and spends nothing`() {
        val plate = catalog("libertad", 1_990..1_992)
        val valued = mutableListOf<String>()
        val notices = mutableListOf<UiNotice>()
        val finance = finance(
            showcase = listOf(showcase(plate)),
            book = valuedBook(),
            onValue = { valued += it },
            onMessage = { notices += it },
        )

        finance.press(resolved(plate, mine = false))

        assertEquals(emptyList(), valued)
        assertEquals(listOf(UiNotice(ShowcaseLabels.ALREADY_FRESH)), notices)
    }

    /** The screen's `remember` holds one reading, so it must answer the same thing twice. */
    @Test
    fun `one reading asked twice answers the same thing`() {
        val plate = catalog("libertad", 1_990..1_992)
        val finance = finance(showcase = listOf(showcase(plate)), book = valuedBook())
        val resolved = resolved(plate, mine = false)

        assertEquals(finance.money(resolved), finance.money(resolved))
        assertEquals(finance.calls(resolved), finance.calls(resolved))
        assertNotNull(finance.money(resolved).entry)
    }
}

private val SPOT = SilverSpot(eurPerTroyOunce = 30.0, readAtMillis = NOW)

/** The issue of a given year of the fixture's type, as the curated files declare it. */
private fun issueOf(year: Int): Int = 70 + year - 1_990

/** Each year of the fixture's type addressed to an issue, as a stored listing would answer. */
private val LISTINGS = IssueListings(
    listedTypeIds = setOf(PRICED_TYPE),
    issueIdByTypeAndYear = (1_990..2_030).associate { year -> (PRICED_TYPE to year) to issueOf(year) },
)

/** Every issue is 40 € in `unc`, the grade holes are priced in. */
private val PRICED: Map<PriceKey, Double> =
    LISTINGS.issueIdByTypeAndYear.values.associate { PriceKey(PRICED_TYPE, it, "unc") to 40.0 }

/** A book nobody has asked anything of: the listings are seeded, the reads are not. */
private fun freshBook() = PriceBook(prices = PRICED, spot = SPOT, listings = LISTINGS)

/** The same book after a tasación today: nothing is left to spend on. */
private fun valuedBook() = freshBook().copy(
    readAt = LISTINGS.issueIdByTypeAndYear.values.associate { (PRICED_TYPE to it) to NOW },
)

private fun finance(
    showcase: List<ShowcasePlate> = emptyList(),
    state: CollectionState = state(),
    book: PriceBook = freshBook(),
    // Taken apart as `ScreenReading` does: holding only its two answers keeps a running count from
    // rebuilding the object.
    pass: ValuationStatus = ValuationStatus(),
    wished: Set<WishKey> = emptySet(),
    onValue: (String) -> Unit = {},
    onMessage: (UiNotice) -> Unit = {},
) = PlateFinance(
    showcase = showcase,
    state = state,
    book = book,
    settled = pass.settled,
    waiting = pass.waiting,
    wished = wished,
    nowMillis = NOW,
    onValue = onValue,
    onMessage = onMessage,
)

private fun resolved(catalog: CollectionCatalog, mine: Boolean) = PlateResult.Available(
    catalog = catalog,
    album = requireNotNull(CatalogAlbums.over(listOf(catalog), itemsOf(catalog))[catalog]),
    mine = mine,
)

private fun showcase(catalog: CollectionCatalog): ShowcasePlate = requireNotNull(
    showcasePlate(
        catalog,
        requireNotNull(CatalogAlbums.over(listOf(catalog), emptyList())[catalog]),
        emptySet(),
    ),
)

/** The collector's 1990 coin, on the «britannia» plates. */
private fun itemsOf(catalog: CollectionCatalog): List<CollectedItem> =
    if (catalog.id == "britannia") listOf(item(id = 1, issueId = 70)) else emptyList()

private fun item(id: Long, issueId: Int) = CollectedItem(
    id = id,
    quantity = 1,
    typeId = PRICED_TYPE,
    grade = "unc",
    price = null,
    issueId = issueId,
    gregorianYear = 1_990,
)

private fun state(items: List<CollectedItem> = emptyList()): CollectionState = CollectionState(
    collection = AssembledCollection(
        items = items,
        typeMeta = mapOf(
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

private fun catalog(id: String, years: IntRange): CollectionCatalog = CollectionCatalog(
    schemaVersion = 2,
    id = id,
    name = id,
    shortName = id,
    family = id,
    issuerCode = "mexique",
    seriesStatus = SeriesStatus.Closed,
    source = "https://en.numista.com/catalogue/pieces1.html",
    updatedAt = "2026-08-14",
    members = years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = PRICED_TYPE,
            // Every casilla names its issue (ADR 0014): one `/prices` per hole, no `/issues`.
            numistaIssueIds = listOf(issueOf(year)),
        )
    },
)
