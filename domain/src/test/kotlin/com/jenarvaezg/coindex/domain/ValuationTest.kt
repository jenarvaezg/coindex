package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The spot measured on 8 August 2026 (`docs/ux/cifras-316.md`), which every figure here divides by. */
private val SPOT = SilverSpot(eurPerTroyOunce = 55.23, readAtMillis = 1_754_600_000_000)

/**
 * What a piece is worth: the maximum of three numbers, piece by piece and never by family
 * (ADR 0026 §10, #316).
 */
class ValuationTest {
    /**
     * The fineness is a rule over Numista's prose, like the metal and the finish before it.
     *
     * Every shape in the 916 seeded fichas is pinned, because the silver floor is the whole of the value
     * of the pieces Numista has no price for: read wrong, a duro of .835 is either 16 % over or nothing
     * at all.
     */
    @Test
    fun `the fineness is read off the composition, in every shape Numista writes it`() {
        assertEquals(0.925, silverFineness("Plata 925"))
        assertEquals(0.999, silverFineness("Plata 999"))
        assertEquals(0.835, silverFineness("Plata 835 (Copper .165)"))
        assertEquals(0.8, silverFineness("Plata 800 (.800 silver .200 copper)"))
        // The decimal comma is Spanish, and the ficha is fetched in Spanish.
        assertEquals(0.9999, silverFineness("Plata 999,9"))
        // The first number wins, or the mark on the coin would overwrite the alloy.
        assertEquals(0.9999, silverFineness("""Plata 999,9 (Marked "PLATA 1000")"""))
        // Billon names its alloy in the head and its fineness inside the bracket: the only shape in
        // the seeded cache where the two are apart.
        assertEquals(0.4, silverFineness("Vellón (plata 400) (Copper .500, Nickel .050, Zinc .050)"))
    }

    /** No number is no floor, and not a floor of one: two of his fichas say «Plata» and stop. */
    @Test
    fun `a silver with no declared fineness has no floor at all`() {
        assertNull(silverFineness("Plata"))
        assertNull(silverFineness("Cuproníquel"))
        assertNull(silverFineness(null))
        // «Nickel silver» has no silver in it, so the metal rule has already spent the word.
        assertNull(silverFineness("Nickel silver 800"))
    }

    /** Fine and not gross, which is the difference between a floor and a lie. */
    @Test
    fun `the silver floor is the fine silver and not the coin`() {
        val duro = meta(weightGrams = 25.0, fineness = 0.835)

        assertEquals(20.875, fineSilverGrams(duro))
        assertNull(fineSilverGrams(meta(weightGrams = 25.0, fineness = null)))
        assertNull(fineSilverGrams(meta(weightGrams = null, fineness = 0.925)))
        assertNull(fineSilverGrams(null))
    }

    /**
     * The market wins today and the metal wins tomorrow, which is what obliges the maximum.
     *
     * Not an occasional tie-break: at 55,23 €/oz the market wins on 517 of his 572 pieces and silver on
     * 14, and at 74 €/oz silver wins on 338. An app that had picked one source would say a silver duro is
     * worth less than its own silver (#316).
     */
    @Test
    fun `the order of the three sources inverts as the spot rises`() {
        // A .835 duro of 25 g: 20,875 g of fine silver, which is 0,671 troy ounces.
        val duro = meta(weightGrams = 25.0, fineness = 0.835)
        val piece = item(grade = "unc", price = 30.0)
        val prices = priceOf(mapOf("unc" to 45.0))

        val atToday = pieceValue(piece, duro, SPOT, prices)
        assertEquals(ValueSource.Market, atToday?.source)
        assertEquals(45.0, atToday?.eur)

        val atSeventyFour = pieceValue(piece, duro, SPOT.copy(eurPerTroyOunce = 74.0), prices)
        assertEquals(ValueSource.Silver, atSeventyFour?.source)
        assertEquals(49.66, atSeventyFour?.eur?.let { Math.round(it * 100) / 100.0 })
    }

    /**
     * What was paid does not go spare for covering only 16 %: it wins 41 times, and the 2 Bolívares of
     * 1879 makes the case alone — Numista gives it no price, its silver is a few euros, and without this
     * source its real value would leave the screen (#316).
     */
    @Test
    fun `what was paid wins where neither the catalogue nor the metal reaches`() {
        val value = pieceValue(
            item(grade = "vf", price = 400.0),
            meta(weightGrams = 10.0, fineness = 0.835),
            SPOT,
            priceOf(emptyMap()),
        )

        assertEquals(ValueSource.Paid, value?.source)
        assertEquals(400.0, value?.eur)
    }

    /**
     * `price` is what was paid for the **row**, so a lot of 102 bolívares is not 102 × the lot.
     *
     * The six Venezuelan lots are where five of every six of his pieces are (#316), so getting this
     * backwards would multiply the collection's total by a hundred.
     */
    @Test
    fun `what was paid is divided by the pieces of its row`() {
        val lot = item(grade = "vf", price = 204.0, quantity = 102)

        assertEquals(2.0, pieceValue(lot, null, null, priceOf(emptyMap()))?.eur)
    }

    /** The grade is the pricing key: a piece is valued in its own, when Numista publishes one. */
    @Test
    fun `a piece is valued in its own grade`() {
        val value = pieceValue(
            item(grade = "vf"),
            null,
            null,
            priceOf(mapOf("vf" to 25.1, "unc" to 39.6)),
        )

        assertEquals(ValueSource.Market, value?.source)
        assertEquals("vf", value?.grade)
        assertEquals(25.1, value?.eur)
    }

    /**
     * The nearest grade when its own has none — 22 of his 229 rows — and **on a tie the worse one**.
     *
     * Guessing upwards is guessing in the collector's favour, which is the direction a valuation must
     * never round in.
     */
    @Test
    fun `a grade with no price falls to its nearest neighbour, and downwards on a tie`() {
        val neighbours = pieceValue(
            item(grade = "f"),
            null,
            null,
            priceOf(mapOf("vg" to 24.3, "vf" to 25.1)),
        )

        assertEquals(ValueSource.NeighbouringGrade, neighbours?.source)
        assertEquals("vg", neighbours?.grade)
        assertEquals(24.3, neighbours?.eur)
    }

    /** A piece the collector never graded is valued in `unc`, which is what a hole is valued in. */
    @Test
    fun `an ungraded piece is valued uncirculated`() {
        val value = pieceValue(item(grade = null), null, null, priceOf(mapOf(UNCIRCULATED to 39.6)))

        assertEquals("unc", value?.grade)
        assertEquals(39.6, value?.eur)
    }

    /** A grade Numista does not publish is not a grade: it falls back rather than looking itself up. */
    @Test
    fun `a grade outside Numista's own list is read as uncirculated`() {
        val value = pieceValue(
            item(grade = "excelente"),
            null,
            null,
            priceOf(mapOf(UNCIRCULATED to 12.0)),
        )

        assertEquals("unc", value?.grade)
    }

    /** A piece with no issue recorded has no market price to be addressed by. */
    @Test
    fun `a piece with no issue has no catalogue price`() {
        val value = pieceValue(
            item(grade = "unc", issueId = null),
            meta(weightGrams = 20.0, fineness = 0.9),
            SPOT,
            priceOf(mapOf("unc" to 999.0)),
        )

        assertEquals(ValueSource.Silver, value?.source)
    }

    /** No source at all is null, which is the case the coverage sentence of ADR 0028 §7 exists for. */
    @Test
    fun `a piece no source covers is worth nothing that can be said`() {
        assertNull(pieceValue(item(grade = null, price = null), null, null, priceOf(emptyMap())))
    }

    /**
     * The total is over **pieces** and not over rows, and it says its coverage.
     *
     * «El valor de N de tus 574 piezas» is a coverage and is said; «llevo 140 de 223» is a progress and
     * is not (ADR 0028 §7).
     */
    @Test
    fun `the total multiplies each row by its pieces and counts what it covered`() {
        val items = listOf(
            item(id = 1, grade = "unc", quantity = 3),
            item(id = 2, typeId = 2, grade = null, price = null),
        )

        val total = collectionValue(items, emptyMap(), null, priceOf(mapOf("unc" to 10.0)))

        assertEquals(30.0, total.eur)
        assertEquals(3, total.valued)
        assertEquals(4, total.pieces)
        assertTrue(!total.covered)
    }

    /** With no spot there is no silver floor: the source is absent rather than zero. */
    @Test
    fun `with no spot on the phone the metal buys nothing`() {
        assertNull(
            pieceValue(
                item(grade = null, price = null),
                meta(weightGrams = 31.1, fineness = 0.999),
                spot = null,
                prices = priceOf(emptyMap()),
            ),
        )
    }

    /**
     * What was paid against what those same pieces are worth today, over the rows that declare a price.
     *
     * `price` is what was paid **for the row** — the six Venezuelan bulks carry one figure for 102
     * pieces — so it is totalled as it comes, while the value today is per piece and multiplies back up.
     */
    @Test
    fun `the paid comparison totals the row's price against what its pieces are worth today`() {
        val comparison = paidComparison(
            listOf(
                item(id = 1, grade = "unc", price = 30.0),
                item(id = 2, grade = "unc", price = 12.0, quantity = 3),
                // No price declared: outside the comparison entirely, on both sides of it.
                item(id = 3, grade = "unc", price = null),
            ),
            mapOf(1 to meta(weightGrams = null, fineness = null)),
            SPOT,
            priceOf(mapOf("unc" to 40.0)),
        )

        assertEquals(42.0, comparison?.paid)
        assertEquals(160.0, comparison?.today)
        assertEquals(4, comparison?.pieces)
    }

    /**
     * A collection that declares no price has no comparison, rather than «pagaste 0 €».
     *
     * What has no price is not a gap in the data: it is what the collector did not buy — gifts and
     * inheritance — and a zero would make a sentence out of that (#491).
     */
    @Test
    fun `nothing declared is no comparison at all`() {
        assertNull(
            paidComparison(
                listOf(item(id = 1, grade = "unc"), item(id = 2, price = 0.0)),
                mapOf(1 to meta(weightGrams = 25.0, fineness = 0.835)),
                SPOT,
                priceOf(mapOf("unc" to 40.0)),
            ),
        )
    }

    /**
     * The comparison can never show a loss, and that is the maximum's doing and not a bug.
     *
     * What a piece is worth is the greatest of the three, and what was paid is one of the three, so
     * today is at worst what it cost. The page states that criterion right above the sentence
     * («el mayor de tres precios»), and valuing this one line by a second rule would put two values
     * for the same coins on one screen.
     */
    @Test
    fun `a piece nobody prices is worth what it cost, and never less`() {
        val comparison = paidComparison(
            listOf(item(id = 1, price = 500.0)),
            mapOf(1 to meta(weightGrams = 25.0, fineness = 0.835)),
            SPOT,
            priceOf(emptyMap()),
        )

        assertEquals(500.0, comparison?.paid)
        assertEquals(500.0, comparison?.today)
    }
}

private fun meta(weightGrams: Double?, fineness: Double?) = TypeMeta(
    id = 1,
    weightOz = weightGrams?.let(::gramsToOunces),
    fineness = fineness,
)

private fun item(
    id: Long = 1,
    typeId: Int = 1,
    grade: String? = null,
    price: Double? = null,
    quantity: Int = 1,
    issueId: Int? = 7,
) = CollectedItem(
    id = id,
    quantity = quantity,
    typeId = typeId,
    grade = grade,
    price = price,
    issueId = issueId,
)

/** Prices for issue 7 of type 1, by grade, which is the shape the app's price book hands over. */
private fun priceOf(grades: Map<String, Double>): (Int, Int, String) -> Double? =
    { typeId, issueId, grade ->
        if (typeId == 1 && issueId == 7) grades[grade] else null
    }
