package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val SPOT = SilverSpot(eurPerTroyOunce = 55.23, readAtMillis = 1_754_600_000_000)

/**
 * The page assembled from what the phone holds, and above all **when the money is not there**.
 *
 * A total at 60 % is not incomplete, it is false: without the market, `max(silver, paid)` gives 10.500 €
 * of the real 16.800, which is literally the «only the silver floor» #316 rejected (ADR 0028 §7).
 */
class FiguresSubjectTest {
    /** While the pass has not finished, there is no money section at all. Not a zero, not a strikethrough. */
    @Test
    fun `the money is absent while the market is still arriving`() {
        val subject = figuresSubject(state(), SPOT, priced, settled = false)

        assertNull(subject.money)
        // And everything else is there: the page opens whole out of the APK.
        assertEquals(3, subject.figures.pieces)
        assertEquals(3, subject.ladders.size)
    }

    /** With no spot there is no money either: the silver floor is what the spot buys. */
    @Test
    fun `with no spot on the phone there is no money section`() {
        assertNull(figuresSubject(state(), spot = null, prices = priced, settled = true).money)
    }

    /** Settled, the amount arrives with the day its silver was read. */
    @Test
    fun `settled, the money says its total and the day of its silver`() {
        val subject = figuresSubject(state(), SPOT, priced, settled = true)

        assertEquals(SPOT, subject.money?.spot)
        assertEquals(3, subject.money?.value?.pieces)
        assertTrue(subject.money?.value?.covered == true)
    }

    /**
     * The export's money switch withdraws the amount **and every figure derived from one**.
     *
     * This is the trap the prototype fell into: «Venezuela · 30 % del valor» is money as much as a total is,
     * and it went out with the money switched off. With it off the portrait keeps its pieces, its mass and
     * its silver, and drops its share of the value.
     */
    @Test
    fun `money off takes the amount and every figure derived from one`() {
        val off = figuresSubject(state(), SPOT, priced, settled = true, moneyAllowed = false)

        assertNull(off.money)
        assertNull(off.portrait?.valueShare, "una cifra derivada del dinero se ha colado")
        assertTrue((off.portrait?.pieceShare ?: 0.0) > 0.0)
        assertTrue((off.portrait?.silverShare ?: 0.0) > 0.0)

        val on = figuresSubject(state(), SPOT, priced, settled = true)
        assertTrue((on.portrait?.valueShare ?: 0.0) > 0.0)
    }

    /**
     * The portrait is the country with the most pieces, with its share of the four things.
     *
     * «Venezuela es el 62 % de sus piezas, el 33 % de su peso y el 33 % de su plata»: the three together say
     * what none of them says alone — that they are a lot of small coins.
     */
    @Test
    fun `the portrait is the country with the most pieces`() {
        val portrait = figuresSubject(state(), SPOT, priced, settled = true).portrait

        assertEquals("Venezuela", portrait?.country)
        assertEquals(2, portrait?.pieces)
        assertEquals(2.0 / 3.0, portrait?.pieceShare)
        // Two coins of 5 g against one of 25 g: a third of the mass, two thirds of the pieces.
        assertEquals(10.0 / 35.0, portrait?.massShare)
    }

    /** An empty collection has no portrait rather than a country called nothing. */
    @Test
    fun `an empty collection has no portrait`() {
        val subject = figuresSubject(CollectionState(), SPOT, priced, settled = true)

        assertNull(subject.portrait)
        assertNull(subject.figures.arc)
        assertNull(subject.figures.size)
    }

    /**
     * The three ladders, in their own units, with the stack extrapolated and declared.
     *
     * The weight is read in kilos and accumulated in grams, and the stack is the only one that carries
     * «unos» — `thickness` is missing in a third of the types.
     */
    @Test
    fun `the ladders read in their own units and only the stack is approximate`() {
        val subject = figuresSubject(state(), SPOT, priced, settled = true)

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

    /**
     * The value of one coin, with the origin said, and the pieces it covers.
     *
     * A number with no provenance in an app with two users is a number nobody can check (#316).
     */
    @Test
    fun `a coin is worth what its pieces are worth, with the origin said`() {
        val value = coinValue(2, state(), SPOT, priced)

        assertEquals(2, value?.pieces)
        assertEquals(80.0, value?.eur)
        assertEquals(ValueSource.Market, value?.source)
        assertEquals("unc", value?.grade)
    }

    /** Two pieces of one type that disagree about their origin are given no origin at all. */
    @Test
    fun `two pieces with different origins leave the origin unsaid`() {
        val disagreeing = state(
            items = listOf(
                item(id = 1, typeId = 2, grade = "unc"),
                item(id = 2, typeId = 2, grade = null, price = 500.0),
            ),
        )

        val value = coinValue(2, disagreeing, SPOT, priced)

        assertEquals(540.0, value?.eur)
        assertNull(value?.source)
    }

    /** A coin no source covers is worth nothing that can be said, and prints nothing. */
    @Test
    fun `a coin no source covers has no value`() {
        assertNull(coinValue(3, state(), spot = null, prices = { _, _, _ -> null }))
    }

    /**
     * A plate is worth what is **in its casillas**, and not what every coin of those types is worth.
     *
     * A type that fills one casilla and sits loose in three more rows is one casilla here: the plate's
     * value is the plate's, which is what keeps it a shopping companion rather than a portfolio.
     */
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

        val value = plateValue(album, state(), SPOT, priced)

        assertEquals(2, value?.pieces)
        assertEquals(80.0, value?.eur)
    }

    /** A plate with nothing in it has no value, rather than a value of zero. */
    @Test
    fun `an empty plate is worth nothing that can be printed`() {
        assertNull(plateValue(CollectionCatalogAlbum(emptyList()), state(), SPOT, priced))
    }
}

/** Issue 7 of type 2 is priced in `unc`; nothing else is. */
private val priced: (Int, Int, String) -> Double? = { typeId, issueId, grade ->
    if (typeId == 2 && issueId == 7 && grade == "unc") 40.0 else null
}

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
 * One Spanish coin of 25 g and two Venezuelan ones of 5 g, which is the smallest inventory that has a
 * dominant country, a priced issue and a type with no thickness.
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
