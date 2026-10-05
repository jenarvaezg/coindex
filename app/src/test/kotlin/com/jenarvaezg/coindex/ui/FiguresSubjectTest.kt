package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.data.prices.HOLE_THRESHOLD_SLOTS
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.PriceKey
import com.jenarvaezg.coindex.data.prices.holesAreWithinReach
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.ItemRef
import com.jenarvaezg.coindex.domain.LadderKind
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.Referent
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.gramsToOunces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val SPOT = SilverSpot(eurPerTroyOunce = 55.23, readAtMillis = 1_754_600_000_000)

/**
 * «Las cifras» assembled from what the phone holds, above all while the money is missing: a total
 * without the market is the silver floor #316 rejected (ADR 0028 §7).
 */
class FiguresSubjectTest {
    @Test
    fun `the money is absent while the market is still arriving`() {
        val subject = figuresSubject(state(), BOOK, settled = false)

        assertNull(subject.money)
        // Everything else is there: the page opens from the APK alone.
        assertEquals(3, subject.figures.pieces)
        assertEquals(3, subject.ladders.size)
    }

    /** Said instead of the section, never beside it: that would be a half-done total (#519). */
    @Test
    fun `waiting, the money's slot says the market has not landed`() {
        val subject = figuresSubject(state(), BOOK, settled = false, waiting = true)

        assertNull(subject.money)
        assertTrue(subject.moneyWaiting)
    }

    @Test
    fun `a pass that is about to finish on its own says nothing`() {
        assertFalse(figuresSubject(state(), BOOK, settled = false).moneyWaiting)
    }

    @Test
    fun `with the market landed nothing is said about waiting`() {
        val subject = figuresSubject(state(), BOOK, settled = true, waiting = true)

        assertNotNull(subject.money)
        assertFalse(subject.moneyWaiting)
    }

    /** An export without money waits for nothing (#228, ADR 0021 §13). */
    @Test
    fun `the export with the money off does not say the market is missing`() {
        val off = figuresSubject(
            state(), BOOK, settled = false, moneyAllowed = false, waiting = true,
        )

        assertFalse(off.moneyWaiting)
    }

    /** The silver floor needs the spot. */
    @Test
    fun `with no spot on the phone there is no money section`() {
        assertNull(figuresSubject(state(), BOOK.copy(spot = null), settled = true).money)
    }

    @Test
    fun `settled, the money says its total and the day of its silver`() {
        val subject = figuresSubject(state(), BOOK, settled = true)

        assertEquals(SPOT, subject.money?.spot)
        assertEquals(3, subject.money?.value?.pieces)
        assertTrue(subject.money?.value?.covered == true)
    }

    /** The portrait's value share is money too; its other shares stay. */
    @Test
    fun `money off takes the amount and every figure derived from one`() {
        val off = figuresSubject(state(), BOOK, settled = true, moneyAllowed = false)

        assertNull(off.money)
        assertNull(off.portrait?.valueShare, "una cifra derivada del dinero se ha colado")
        assertTrue((off.portrait?.pieceShare ?: 0.0) > 0.0)
        assertTrue((off.portrait?.silverShare ?: 0.0) > 0.0)

        val on = figuresSubject(state(), BOOK, settled = true)
        assertTrue((on.portrait?.valueShare ?: 0.0) > 0.0)
    }

    /** Hung off `MoneyReading`, so no branch can leak it with the money switched off. */
    @Test
    fun `what was paid arrives with the money and cannot leave without it`() {
        val bought = state(items = listOf(item(id = 1, typeId = 2, grade = "unc", price = 30.0)))

        val paid = figuresSubject(bought, BOOK, settled = true).money?.paid

        assertEquals(30.0, paid?.paid)
        assertEquals(40.0, paid?.today)
        assertEquals(1, paid?.pieces)
        assertNull(figuresSubject(bought, BOOK, settled = true, moneyAllowed = false).money)
    }

    @Test
    fun `nothing declared leaves the comparison unsaid`() {
        assertNull(figuresSubject(state(), BOOK, settled = true).money?.paid)
    }

    @Test
    fun `the portrait is the country with the most pieces`() {
        val portrait = figuresSubject(state(), BOOK, settled = true).portrait

        assertEquals("Venezuela", portrait?.country)
        assertEquals(2, portrait?.pieces)
        assertEquals(2.0 / 3.0, portrait?.pieceShare)
        // Two coins of 5 g against one of 25 g: two sevenths of the mass, two thirds of the pieces.
        assertEquals(10.0 / 35.0, portrait?.massShare)
    }

    @Test
    fun `an empty collection has no portrait`() {
        val subject = figuresSubject(CollectionState(), BOOK, settled = true)

        assertNull(subject.portrait)
        assertNull(subject.figures.arc)
        assertNull(subject.figures.size)
    }

    /** Weight is read in kilos; the stack is extrapolated because many types lack `thickness`. */
    @Test
    fun `the ladders read in their own units and only the stack is approximate`() {
        val subject = figuresSubject(state(), BOOK, settled = true)

        assertEquals(
            listOf(LadderKind.Weight, LadderKind.Row, LadderKind.Stack),
            subject.ladders.map { it.ladder.kind },
        )
        assertEquals(0.035, subject.ladders.first().amount)
        assertEquals(listOf(false, false, true), subject.ladders.map { it.approximate })
        assertTrue(subject.ladders.last().isStack())
        // Under the first rung of every ladder, with this toy collection.
        assertEquals(Referent.Brick, subject.ladders.first().placement.nextUp?.referent)
    }

    /** A number with no provenance can't be checked (#316). */
    @Test
    fun `a coin is worth what its pieces are worth, with the origin said`() {
        val value = coinValue(2, state(), BOOK)

        assertEquals(2, value?.pieces)
        assertEquals(80.0, value?.eur)
        assertEquals(ValueSource.Market, value?.source)
        assertEquals("unc", value?.grade)
    }

    @Test
    fun `two pieces with different origins leave the origin unsaid`() {
        val disagreeing = state(
            items = listOf(
                item(id = 1, typeId = 2, grade = "unc"),
                item(id = 2, typeId = 2, grade = null, price = 500.0),
            ),
        )

        val value = coinValue(2, disagreeing, BOOK)

        assertEquals(540.0, value?.eur)
        assertNull(value?.source)
    }

    @Test
    fun `a coin no source covers has no value`() {
        assertNull(coinValue(3, state(), PriceBook()))
    }

    /** The plate's value, not a portfolio's: a type loose elsewhere counts only in its casilla. */
    @Test
    fun `a plate is worth what fills its casillas and nothing else`() {
        val album = CollectionCatalogAlbum(
            members = listOf(
                CollectionCatalogAlbumMember(
                    member = CollectionCatalogMember(id = "a", label = "1960", year = 1_960),
                    status = CollectionCatalogMemberStatus.Owned(1, listOf(ItemRef(2, 2, 2))),
                ),
                CollectionCatalogAlbumMember(
                    member = CollectionCatalogMember(id = "b", label = "1961", year = 1_961),
                    status = CollectionCatalogMemberStatus.Missing,
                ),
            ),
        )

        val value = plateValue(album, state(), BOOK)

        assertEquals(2, value?.pieces)
        assertEquals(80.0, value?.eur)
    }

    @Test
    fun `an empty plate is worth nothing that can be printed`() {
        assertNull(plateValue(CollectionCatalogAlbum(emptyList()), state(), BOOK))
    }

    /**
     * A hole with neither a declared issue nor a stored listing can't be priced; it is left out
     * rather than silencing the whole figure (#493).
     */
    @Test
    fun `the cost of closing prices the holes it can and leaves the rest out`() {
        val money = plateMoney(
            albumWith(
                listOf(
                    hole(id = "b", year = 1_961),
                    // Type 3 has neither price nor weight: no catalogue price, no silver floor.
                    hole(id = "c", year = 1_962, typeId = 3, issueIds = emptyList()),
                ),
            ),
            state(),
            BOOK,
        )

        assertEquals(80.0, money.value?.eur)
        assertEquals(40.0, money.cost?.eur)
        assertEquals(1, money.cost?.holes)
        // Only the priced hole carries a stamp, and the other gets no «—».
        assertEquals(mapOf("b" to 40.0), money.holeCosts)
    }

    /** Few curated files name their issues; the stored listing addresses the rest (#452). */
    @Test
    fun `a hole the listing addresses is priced as well as one the file declares`() {
        val listings = IssueListings.held(
            reads = listOf(TypeIssueReadEntity(typeId = 2, readAt = 0)),
            issues = listOf(
                TypeIssueEntity(typeId = 2, issueId = 7, position = 0, year = 1_961, gregorianYear = null),
            ),
        )

        val money = plateMoney(
            albumWith(listOf(hole(id = "b", year = 1_961, issueIds = emptyList()))),
            state(),
            BOOK.copy(listings = listings),
        )

        assertEquals(40.0, money.cost?.eur)
        assertEquals(mapOf("b" to 40.0), money.holeCosts)
    }

    @Test
    fun `a complete plate has no cost of closing and no stamps`() {
        val money = plateMoney(albumWith(emptyList()), state(), BOOK)

        assertEquals(80.0, money.value?.eur)
        assertNull(money.cost)
        assertEquals(emptyMap(), money.holeCosts)
    }

    /**
     * The two figures are made of different reads, and a marked casilla is repriced when marked
     * (ADR 0029 §4), so one date could not cover both (#494, #594).
     */
    @Test
    fun `each figure of a plate is dated by the oldest read behind it`() {
        val book = BOOK.copy(
            prices = BOOK.prices + (PriceKey(typeId = 3, issueId = 9, grade = "unc") to 25.0),
            readAt = mapOf((2 to 7) to JUNE, (3 to 9) to AUGUST),
        )

        val money = plateMoney(
            albumWith(listOf(hole(id = "b", year = 1_961, typeId = 3, issueIds = listOf(9)))),
            state(),
            book,
        )

        // The filled casilla is type 2 issue 7, read in June; the hole, type 3 issue 9, in August.
        assertEquals(JUNE, money.value?.catalogReadAt)
        assertEquals(AUGUST, money.cost?.catalogReadAt)
    }

    /** A date is a promise about the whole figure. */
    @Test
    fun `a cost of closing made of two reads says the older one`() {
        val book = BOOK.copy(
            prices = BOOK.prices + (PriceKey(typeId = 3, issueId = 9, grade = "unc") to 25.0),
            readAt = mapOf((2 to 7) to JUNE, (3 to 9) to AUGUST),
        )

        val money = plateMoney(
            albumWith(
                listOf(
                    hole(id = "b", year = 1_961),
                    hole(id = "c", year = 1_962, typeId = 3, issueIds = listOf(9)),
                ),
            ),
            state(),
            book,
        )

        assertEquals(2, money.cost?.holes)
        assertEquals(JUNE, money.cost?.catalogReadAt)
    }

    /**
     * Metal and paid prices have no Numista read; the spot's date would always say today (#594).
     */
    @Test
    fun `a plate no catalogue price feeds says no date at all`() {
        val money = plateMoney(albumWith(listOf(hole(id = "b", year = 1_961))), state(), BOOK)

        assertNotNull(money.value)
        assertNotNull(money.cost)
        assertNull(money.value?.catalogReadAt)
        assertNull(money.cost?.catalogReadAt)
    }

    /** The plate's rule, which #561 relied on when it lengthened a catalogue price's life. */
    @Test
    fun `the total of the page carries the oldest read of its catalogue prices`() {
        val subject = figuresSubject(state(), BOOK.copy(readAt = mapOf((2 to 7) to JUNE)), settled = true)

        assertEquals(JUNE, subject.money?.value?.catalogReadAt)
        assertNull(figuresSubject(state(), BOOK, settled = true).money?.value?.catalogReadAt)
    }

    /**
     * The pass never prices holes beyond the ADR 0028 §1 threshold. The bound is asked of
     * `holesAreWithinReach` rather than repeated, so changing it cannot leave a stale copy here.
     */
    @Test
    fun `over the threshold a plate has no second figure, and the bound is not duplicated`() {
        val withinReach = (1..HOLE_THRESHOLD_SLOTS).map { hole(id = "h$it", year = 1_960 + it) }
        val reproach = withinReach + hole(id = "over", year = 2_000)

        val counted = plateMoney(albumWith(withinReach), state(), BOOK)
        val over = plateMoney(albumWith(reproach), state(), BOOK)

        assertEquals(HOLE_THRESHOLD_SLOTS, counted.cost?.holes)
        assertEquals(HOLE_THRESHOLD_SLOTS, counted.holeCosts.size)
        assertNull(over.cost)
        assertEquals(emptyMap(), over.holeCosts)
        assertTrue(holesAreWithinReach(HOLE_THRESHOLD_SLOTS))
        assertFalse(holesAreWithinReach(HOLE_THRESHOLD_SLOTS + 1))
        assertFalse(holesAreWithinReach(0))
    }
}

/** One filled casilla plus the holes a test wants. */
private fun albumWith(holes: List<CollectionCatalogMember>) = CollectionCatalogAlbum(
    members = listOf(
        CollectionCatalogAlbumMember(
            member = CollectionCatalogMember(
                id = "a",
                label = "1960",
                year = 1_960,
                numistaTypeId = 2,
            ),
            status = CollectionCatalogMemberStatus.Owned(1, listOf(ItemRef(2, 2, 2))),
        ),
    ) + holes.map { CollectionCatalogAlbumMember(it, CollectionCatalogMemberStatus.Missing) },
)

/** One empty casilla, of the priced type unless a test asks for another. */
private fun hole(
    id: String,
    year: Int,
    typeId: Int = 2,
    issueIds: List<Int> = listOf(7),
) = CollectionCatalogMember(
    id = id,
    label = year.toString(),
    year = year,
    numistaTypeId = typeId,
    numistaIssueIds = issueIds,
)

/** Two reads within one catalogue price's ninety-day life (ADR 0028 §5, #561). */
private const val JUNE = 1_780_000_000_000
private const val AUGUST = 1_786_000_000_000

/** Issue 7 of type 2 is priced in `unc`; nothing else is. */
private val BOOK = PriceBook(
    prices = mapOf(PriceKey(typeId = 2, issueId = 7, grade = "unc") to 40.0),
    spot = SPOT,
)

private fun item(
    id: Long,
    typeId: Int,
    quantity: Int = 1,
    grade: String? = null,
    price: Double? = null,
) = CollectedItem(
    id = id,
    quantity = quantity,
    typeId = typeId,
    grade = grade,
    price = price,
    issueId = 7,
    gregorianYear = 1_960,
)

/**
 * One Spanish coin of 25 g and two Venezuelan ones of 5 g: the smallest inventory with a dominant
 * country, a priced issue and a type with no thickness.
 */
private fun state(
    items: List<CollectedItem> = listOf(
        item(id = 1, typeId = 1, grade = "unc"),
        item(id = 2, typeId = 2, quantity = 2, grade = "unc"),
    ),
): CollectionState = CollectionState(
    collection = AssembledCollection(
        items = items,
        typeMeta = mapOf(
            1 to meta(1, "espagne", "España", weightGrams = 25.0, thickness = 2.5),
            2 to meta(2, "venezuela", "Venezuela", weightGrams = 5.0),
            3 to meta(3, "france", "France", weightGrams = null),
        ),
    ),
)

private fun meta(
    id: Int,
    issuerCode: String,
    issuerName: String,
    weightGrams: Double?,
    thickness: Double? = null,
) = TypeMeta(
    id = id,
    issuerCode = issuerCode,
    issuerName = issuerName,
    weightOz = weightGrams?.let(::gramsToOunces),
    metal = Metal.Silver,
    fineness = 0.9,
    sizeMillimetres = 30.0,
    thicknessMillimetres = thickness,
)
