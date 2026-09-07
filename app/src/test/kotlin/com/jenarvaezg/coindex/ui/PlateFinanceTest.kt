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
 * Which of the three régimes a plate's money comes from, and what tasar it would spend (ADR 0030 §3).
 *
 * The selector used to be a lambda in the body of the root composable, with no test of its own but the
 * device measuring the drawing: the three branches — the shelf window, the market still arriving, the
 * collector's own plate — were only ever exercised by hand. What each branch *computes* is covered by
 * `ShowcaseSubjectTest` and `FiguresSubjectTest`; what this file covers is the choice between them, and
 * that the plate takes the same reading twice.
 */
class PlateFinanceTest {
    /**
     * A plate of the shelf window is priced as one, and it is **not** gated on the collection's pass.
     *
     * Its prices arrive by a gesture of their own (ADR 0030 §3), so waiting for the market of a
     * collection it has no coin in would leave the amount off a plate that has just been valued.
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
        // And the stamp inside each hole, which the collector's own plate draws from the same field:
        // the two régimes differ in the header's figure and never in what is laid on a casilla.
        assertEquals(
            mapOf("libertad-1990" to 40.0, "libertad-1991" to 40.0, "libertad-1992" to 40.0),
            money.holeCosts,
        )
        // The collector's two figures are the other régime's and are never beside this one.
        assertNull(money.value)
        assertNull(money.cost)
        assertFalse(money.waiting)
    }

    /** While the market is still arriving, a plate of the collector's says so instead of a figure. */
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

    /**
     * With the market landed, the collector's plate is the album's own walk: value, cost and stamps.
     *
     * And no `entry`: what entering costs is the other régime's figure, and a plate that holds a coin is
     * not one the collector is looking at from outside (ADR 0030 §6).
     */
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
     * The shelf window is looked up by catalog id and not by `mine`, which is the resolution's own bit.
     *
     * The two answer the same question today and they are two readings: a plate the window does not hold
     * falls through to the collector's régime whatever the resolution said about it, which is what keeps
     * the empty `entry` of a plate nobody curated out of the header.
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
     * The collector's marks reach the plate's own régime, which is ADR 0029 §4's clause.
     *
     * Read on a plate **over** the threshold of ADR 0028 §1, because that is the only place the set can
     * be seen at all: within reach every hole is priced anyway, so a dispatch that dropped the marks on
     * the floor would look identical. Of the fifty-one holes, this one.
     */
    @Test
    fun `a marked casilla is priced on a plate whose holes are out of reach`() {
        val plate = catalog("britannia", 1_990..2_010)
        val marked = requireNotNull(plate.members.last().wishKey())

        val unmarked = finance(book = valuedBook()).money(resolved(plate, mine = true))
        val money = finance(book = valuedBook(), wished = setOf(marked)).money(resolved(plate, mine = true))

        assertEquals(emptyMap(), unmarked.holeCosts)
        assertEquals(mapOf("britannia-2010" to 40.0), money.holeCosts)
        // And the header's second figure is still absent: one hole is not the cost of closing
        // twenty-one, and printing it would say «Coste de cerrar» over a number that closes nothing.
        assertNull(money.cost)
    }

    /** What the gesture prints before it is pressed: the pass's own arithmetic over this plate's holes. */
    @Test
    fun `the gesture names what tasar this plate would spend`() {
        val plate = catalog("libertad", 1_990..1_992)
        val finance = finance(showcase = listOf(showcase(plate)))

        // Three casillas of one type: one `/prices` each and no `/issues` call at all, because the
        // curated file names the issue of every one of them.
        assertEquals(3, finance.calls(resolved(plate, mine = false)))
    }

    /** A plate that is not the window's has nothing to tasar: the gesture is not on it at all. */
    @Test
    fun `the collector's own plate spends nothing, because it has no gesture`() {
        val plate = catalog("britannia", 1_990..1_991)

        assertEquals(0, finance().calls(resolved(plate, mine = true)))
    }

    /** A plate whose prices are all fresh has nothing left to ask, and the gesture says zero. */
    @Test
    fun `a plate valued today asks for nothing more`() {
        val plate = catalog("libertad", 1_990..1_992)
        val finance = finance(showcase = listOf(showcase(plate)), book = valuedBook())

        assertEquals(0, finance.calls(resolved(plate, mine = false)))
    }

    /** Pressing it starts the pass over **this** plate, which is the unit the collector chose. */
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

    /**
     * With nothing to ask, the press answers in words and buys nothing (ADR 0028 §5).
     *
     * A gesture that silently did nothing is a button the collector reads as broken, and one that ran a
     * pass over an empty plan would spend a call to be told what the phone already holds.
     */
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

    /**
     * One reading and not a fresh one per call, which is what the screen's `remember` is keyed for.
     *
     * It cannot assert the `remember` itself — that lives in a composition — but it can assert the half
     * the module owes it: one instance, asked twice about one plate, answers the same thing. An object
     * that did not would make a held reading a lie rather than a saving.
     */
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

/** The issue a given year of the fixture's type is, which is what the curated files declare. */
private fun issueOf(year: Int): Int = 70 + year - 1_990

/** Each year of the fixture's type addressed to an issue, as a stored listing would answer. */
private val LISTINGS = IssueListings(
    listedTypeIds = setOf(PRICED_TYPE),
    issueIdByTypeAndYear = (1_990..2_030).associate { year -> (PRICED_TYPE to year) to issueOf(year) },
)

/** Every issue of the fixture is 40 € in `unc`, which is the one grade a hole is priced in. */
private val PRICED: Map<PriceKey, Double> =
    LISTINGS.issueIdByTypeAndYear.values.associate { PriceKey(PRICED_TYPE, it, "unc") to 40.0 }

/** A book nobody has asked anything of: the listings are seeded, the reads are not. */
private fun freshBook() = PriceBook(prices = PRICED, spot = SPOT, listings = LISTINGS)

/** The same book after a tasación today: every issue asked about, so nothing is left to spend on. */
private fun valuedBook() = freshBook().copy(
    readAt = LISTINGS.issueIdByTypeAndYear.values.associate { (PRICED_TYPE to it) to NOW },
)

private fun finance(
    showcase: List<ShowcasePlate> = emptyList(),
    state: CollectionState = state(),
    book: PriceBook = freshBook(),
    pass: ValuationStatus = ValuationStatus(),
    wished: Set<WishKey> = emptySet(),
    onValue: (String) -> Unit = {},
    onMessage: (UiNotice) -> Unit = {},
) = PlateFinance(
    showcase = showcase,
    state = state,
    book = book,
    pass = pass,
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

/** The collector's coin of the 1990 casilla, on the plates the tests hand an inventory to. */
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
            // The file names the issue of every casilla, as an issue run does (ADR 0014): so the
            // gesture's ceiling is one `/prices` per hole and no `/types/{id}/issues` at all.
            numistaIssueIds = listOf(issueOf(year)),
        )
    },
)
