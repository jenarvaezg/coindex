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
 * Everything the screens read, and the one rule about when it is read again.
 *
 * These derivations used to be written in the body of the root composable — the shelf window, the
 * living marks and their month, «Las cifras», the sewn edge, the value of a coin, the address of its
 * ficha — where an emulator was the only thing that could answer any of them. The last class of this
 * file's tests is the other half of the ticket: what a screen keys its `remember` on is this value,
 * so what has to be true is that the value stops changing when nothing behind it has.
 */
class ScreenReadingTest {
    @Test
    fun `the shelf window is the plates this collector holds nothing of`() {
        val reading = reading()

        assertEquals(listOf("libertad"), reading.showcase.map { it.catalog.id })
        // And the plate the collector does own resolves as theirs, from the same reading.
        val mine = reading.plate("britannia")
        assertTrue(mine is PlateResult.Available && mine.mine)
        assertNull(reading.showcasePlateOf("britannia"))
    }

    @Test
    fun `a mark whose casilla is full is not a living wish, and is still drawn on the plate`() {
        // 1990 is the year the collector's coin fills; 1991 is the hole they are hunting.
        val reading = reading(marks = listOf(mark(1_990), mark(1_991)))

        assertEquals(listOf(1_991), reading.livingWishes.map { it.member.year })
        // The keys are the table's own, dead marks included: a plate paints one only on an empty
        // casilla, so the album is what decides whether it shows (ADR 0029 §2).
        assertEquals(setOf(key(1_990), key(1_991)), reading.wishedKeys)
    }

    @Test
    fun `the door draws the marks without prices and the annex draws them with`() {
        val reading = reading(marks = listOf(mark(1_991)), pass = settled())

        assertEquals(listOf("britannia/britannia-1991"), reading.wishedRows.map { it.id })
        assertNull(reading.wishedRows.single().cost)
        assertEquals("40 €", reading.wishAnnex.rows.single().cost)
        // What the mark costs a month, which is the figure «Este teléfono» prints: one `/prices` for a
        // casilla whose curated file already names its issue, and the second call of the gesture's
        // «+2 consultas al mes» only where the issue has to be looked up too (ADR 0029 §5).
        assertEquals(1, reading.wishCalls)
    }

    /** The same gate as every other amount in the app: no market, no price anywhere (ADR 0028 §7). */
    @Test
    fun `while the market is arriving the annex prices nothing and no coin is worth anything`() {
        val reading = reading(
            marks = listOf(mark(1_991)),
            pass = ValuationStatus(wanted = 2, missing = 2, held = ValuationRefusal.NoApiKey),
        )

        assertNull(reading.wishAnnex.rows.single().cost)
        assertNull(reading.coinValue(TYPE))
        // And the absence is worth a line, which is the pass's own reading and not a second guess.
        assertTrue(reading.figures.moneyWaiting)
    }

    @Test
    fun `a coin is worth what its pieces are worth, once the market has landed`() {
        val value = reading(pass = settled()).coinValue(TYPE)

        assertEquals(40.0, value?.eur)
        assertEquals(1, value?.pieces)
    }

    /**
     * The URL Numista handed over, in the language the ficha was asked in, and the type's own
     * address where this phone holds no ficha at all (#508).
     */
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
        // The same three counts «La materia» draws, and never a second walk of the inventory.
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
     * With the curated files unreadable there is no shelf, and the reading says so.
     *
     * What is on screen at that moment is [UiState.fatalError] itself, and the masthead and the sewn
     * edge above it still have to draw: touching `repository.curation` again would raise the parse
     * error a second time, this time with nothing to catch it.
     */
    @Test
    fun `without curated files there is no catalog, no name and no plate`() {
        val reading = UiState(loading = false).reading(of(NO_CURATION))

        assertEquals(emptyList(), reading.catalogs)
        assertEquals(emptySet(), reading.curatedNames)
        assertEquals(emptyList(), reading.showcase)
        assertNull(reading.catalogName("britannia"))
        assertTrue(reading.plate("britannia") is PlateResult.Unavailable)
        // And the sewn edge still counts what the phone holds, because that is the snapshot's.
        assertEquals(1, reading.sewnEdge?.pieces)
    }

    /**
     * Two readings of the same state are the same value, which is what a screen's `remember` needs.
     *
     * The fields are `by lazy`, so a walk belongs to an instance: what makes it happen once is
     * `equals` over the slices in the constructor, and the ViewModel handing back the instance it
     * already had.
     */
    @Test
    fun `two readings of one state are equal`() {
        val state = UiState(loading = false, wishes = listOf(mark(1_991)))

        assertEquals(state.reading(of()), state.reading(of()))
    }

    /**
     * **What is in flight is not part of a reading**, which is the whole of why it is a value.
     *
     * A ficha being refreshed, a tasación in flight, a chooser opening, a snackbar: none of them
     * moves an amount, an album or a mark, and a reading that changed with them would rebuild the
     * plate's subject — and re-walk its album — twice per press. The pass's running count is the same
     * clause read once more: it moves every twenty-five issues and nothing here reads it.
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
        // And when the market stops having landed, which is not the same event as a price arriving.
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

/** The collection's own half of a reading, which is the half a price cannot move. */
private fun of(curation: Curation = CURATION) = CollectionReading(curation, state())

/**
 * The collector's phone: one 1990 Britannia, its ficha, and a box they typed a name into.
 *
 * Assembled through [Curation.assemble] rather than filled in by hand, because that is the whole
 * point of the door (#217): what a reading crosses has to be the assembly the app reads, index and
 * albums included, or the counts under test are counts of something else.
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

/** A pass with nothing left to ask, which is the state every amount on screen is gated on. */
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
