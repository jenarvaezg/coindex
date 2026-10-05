package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.PriceKey
import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_786_400_000_000L

/** The one type every casilla of these fixtures is, and the only one with a price on the phone. */
private const val TYPE = 2

/**
 * [ScreenReading]: what the screens derive (shelf window, living marks, «Las cifras», sewn edge,
 * coin value, ficha address), and that it stays equal while nothing behind it changes.
 */
class ScreenReadingTest {
    @Test
    fun `the shelf window is the plates this collector holds nothing of`() {
        val reading = reading()

        assertEquals(listOf("libertad"), reading.showcase.map { it.catalog.id })
        // The plate the collector owns resolves as theirs.
        val mine = reading.plate("britannia")
        assertTrue(mine is PlateResult.Available && mine.mine)
        assertNull(reading.showcasePlateOf("britannia"))
    }

    @Test
    fun `a mark whose casilla is full is not a living wish, and is still drawn on the plate`() {
        // 1990 is the year the collector's coin fills; 1991 is the hole they are hunting.
        val reading = reading(marks = listOf(mark(1_990), mark(1_991)))

        assertEquals(listOf(1_991), reading.livingWishes.map { it.member.year })
        // Dead marks stay in the keys: a plate paints one only on an empty casilla (ADR 0029 §2).
        assertEquals(setOf(key(1_990), key(1_991)), reading.wishedKeys)
    }

    @Test
    fun `the door draws the marks without prices and the annex draws them with`() {
        val reading = reading(marks = listOf(mark(1_991)), pass = settled())

        assertEquals(listOf("britannia/britannia-1991"), reading.wishedRows.map { it.id })
        assertNull(reading.wishedRows.single().cost)
        assertEquals("40 €", reading.wishAnnex.rows.single().cost)
        // The mark's monthly cost: one `/prices`, since the curated file names the issue. The
        // second call of «+2 consultas al mes» is for an issue to look up (ADR 0029 §5).
        assertEquals(1, reading.wishCalls)
    }

    /** The same gate as every amount in the app: no market, no price (ADR 0028 §7). */
    @Test
    fun `while the market is arriving the annex prices nothing and no coin is worth anything`() {
        val reading = reading(
            marks = listOf(mark(1_991)),
            pass = ValuationStatus(wanted = 2, missing = 2, held = ValuationRefusal.NoApiKey),
        )

        assertNull(reading.wishAnnex.rows.single().cost)
        assertNull(reading.coinValue(TYPE))
        // The absence gets a line, read from the pass itself.
        assertTrue(reading.figures.moneyWaiting)
    }

    @Test
    fun `a coin is worth what its pieces are worth, once the market has landed`() {
        val value = reading(pass = settled()).coinValue(TYPE)

        assertEquals(40.0, value?.eur)
        assertEquals(1, value?.pieces)
    }

    /** Numista's URL keeps the language the ficha was asked in (#508). */
    @Test
    fun `the ficha of a coin is the address Numista gave, or the type's own`() {
        val reading = reading()

        assertEquals("https://es.numista.com/1885", reading.numistaUrl(TYPE))
        assertEquals("https://en.numista.com/catalogue/pieces99.html", reading.numistaUrl(99))
    }

    /** Zeros here would claim the collection is empty before the snapshot lands (#418). */
    @Test
    fun `the sewn edge is absent while the first snapshot is still being read`() {
        assertNull(reading(loading = true).sewnEdge)

        val edge = assertNotNull(reading().sewnEdge)
        assertEquals(1, edge.pieces)
        assertEquals(1, edge.types)
        // The counts «La materia» draws, not a second walk of the inventory.
        assertEquals(reading().figures.figures.pieces, edge.pieces)
    }

    @Test
    fun `a card names itself the way the screen that opened it does`() {
        val reading = reading()

        assertEquals("britannia", reading.catalogName("britannia"))
        assertNull(reading.catalogName("nobody"))
        assertEquals("La caja", reading.boxName(7))
        assertNull(reading.boxName(8))
    }

    /**
     * The screen then shows [UiState.fatalError] under a masthead and sewn edge that still draw;
     * reading `repository.curation` again would rethrow the parse error with nothing to catch it.
     */
    @Test
    fun `without curated files there is no catalog, no name and no plate`() {
        val reading = UiState(loading = false).reading(of(NO_CURATION))

        assertEquals(emptyList(), reading.catalogs)
        assertEquals(emptySet(), reading.curatedNames)
        assertEquals(emptyList(), reading.showcase)
        assertNull(reading.catalogName("britannia"))
        assertTrue(reading.plate("britannia") is PlateResult.Unavailable)
        // The sewn edge comes from the snapshot, so it still counts.
        assertEquals(1, reading.sewnEdge?.pieces)
    }

    /**
     * A screen's `remember` keys on this. The fields are `by lazy`, so walking once also relies on
     * the ViewModel handing back the instance it already had.
     */
    @Test
    fun `two readings of one state are equal`() {
        val state = UiState(loading = false, wishes = listOf(mark(1_991)))

        assertEquals(state.reading(of()), state.reading(of()))
    }

    /**
     * Otherwise each press would rebuild the plate's subject and re-walk its album. The pass's
     * running count moves every twenty-five issues and nothing in a reading uses it.
     */
    @Test
    fun `a reading does not move for what is in flight, nor for the count of a running pass`() {
        val running = ValuationStatus(wanted = 40, missing = 15)
        val state = UiState(loading = false, valuation = running)

        assertEquals(
            state.reading(of()),
            state.copy(
                refreshingFichas = setOf(TYPE),
                valuingPlate = "libertad",
                exportingData = true,
                message = UiNotice("lo que sea"),
                // Twenty-five issues further on, which is how often the pass reports itself.
                valuation = running.copy(missing = 5),
            ).reading(of()),
        )
    }

    @Test
    fun `a reading moves when the collection, the market or the marks do`() {
        val state = UiState(loading = false)
        val reading = state.reading(of())

        assertTrue(reading != state.reading(CollectionReading(CURATION, state(items = emptyList()))))
        assertTrue(reading != state.copy(prices = valuedBook()).reading(of()))
        assertTrue(reading != state.copy(wishes = listOf(mark(1_991))).reading(of()))
        // Also when the market stops having landed, a separate event from a price arriving.
        assertTrue(
            reading != state.copy(
                valuation = ValuationStatus(wanted = 2, missing = 2, held = ValuationRefusal.NoApiKey),
            ).reading(of()),
        )
    }
}

/** The two catalogs of the fixture: one the collector has a coin of, one they have nothing of. */
private val BRITANNIA = catalog("britannia", 1_990..1_991)
private val LIBERTAD = catalog("libertad", 1_992..1_993)

private val CURATION = Curation(catalogs = listOf(BRITANNIA, LIBERTAD))

/** The collection half of a reading, which prices don't move. */
private fun of(curation: Curation = CURATION) = CollectionReading(curation, state())

/**
 * The collector's phone: one 1990 Britannia, its ficha, and a named box. Built with
 * [Curation.assemble] so the reading crosses the same assembly the app reads (#217).
 */
private fun state(items: List<CollectedItem> = listOf(item())): CollectionState = CollectionState(
    collection = CURATION.assemble(
        CollectionSnapshot(
            items = items,
            typeMeta = mapOf(
                TYPE to TypeMeta(
                    id = TYPE,
                    issuerCode = "royaume-uni",
                    issuerName = "Royaume-Uni",
                    weightOz = 1.0,
                    fineness = 0.999,
                    metal = Metal.Silver,
                    numistaUrl = "https://es.numista.com/1885",
                ),
            ),
            ownGroupings = listOf(OwnGrouping(id = 7, name = "La caja", typeIds = listOf(TYPE))),
        ),
    ),
)

private fun reading(
    marks: List<Wish> = emptyList(),
    pass: ValuationStatus = ValuationStatus(),
    loading: Boolean = false,
): ScreenReading = UiState(
    loading = loading,
    prices = valuedBook(),
    pricesArrivedAt = NOW,
    wishes = marks,
    valuation = pass,
).reading(of())

/** A pass with nothing left to ask, which gates every amount on screen. */
private fun settled() = ValuationStatus(wanted = 2, missing = 0)

private fun key(year: Int) = WishKey(TYPE, year, issueOf(year))

private fun mark(year: Int) = Wish(key(year), NOW)

private fun item() = CollectedItem(
    id = 1,
    quantity = 1,
    typeId = TYPE,
    grade = "unc",
    price = null,
    issueId = issueOf(1_990),
    gregorianYear = 1_990,
)

/** The issue a given year of the fixture's type is, which is what the curated files declare. */
private fun issueOf(year: Int): Int = 70 + year - 1_990

private val LISTINGS = IssueListings(
    listedTypeIds = setOf(TYPE),
    issueIdByTypeAndYear = (1_990..1_993).associate { year -> (TYPE to year) to issueOf(year) },
)

/** Every issue of the fixture is 40 € in `unc`, asked about today. */
private fun valuedBook() = PriceBook(
    prices = LISTINGS.issueIdByTypeAndYear.values.associate { PriceKey(TYPE, it, "unc") to 40.0 },
    listings = LISTINGS,
    readAt = LISTINGS.issueIdByTypeAndYear.values.associate { (TYPE to it) to NOW },
)

private fun catalog(id: String, years: IntRange): CollectionCatalog = CollectionCatalog(
    schemaVersion = 2,
    id = id,
    name = id,
    shortName = id,
    family = id,
    issuerCode = "royaume-uni",
    seriesStatus = SeriesStatus.Closed,
    source = "https://en.numista.com/catalogue/pieces1.html",
    updatedAt = "2026-09-07",
    members = years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = TYPE,
            numistaIssueIds = listOf(issueOf(year)),
        )
    },
)
