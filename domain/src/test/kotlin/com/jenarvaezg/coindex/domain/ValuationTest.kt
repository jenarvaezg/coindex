package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Spot read on 8 August 2026 (`docs/ux/cifras-316.md`); the figures below derive from it. */
private val SPOT = SilverSpot(eurPerTroyOunce = 55.23, readAtMillis = 1_754_600_000_000)

/** A piece is worth the maximum of three sources, per piece and never per family (ADR 0026 §10). */
class ValuationTest {
    /** Every shape in the seeded cache: the silver floor is all the value an unpriced piece has. */
    @Test
    fun `the fineness is read off the composition, in every shape Numista writes it`() {
        assertEquals(0.925, silverFineness("Plata 925"))
        assertEquals(0.999, silverFineness("Plata 999"))
        assertEquals(0.835, silverFineness("Plata 835 (Copper .165)"))
        assertEquals(0.8, silverFineness("Plata 800 (.800 silver .200 copper)"))
        // Decimal comma: the ficha is fetched in Spanish.
        assertEquals(0.9999, silverFineness("Plata 999,9"))
        // The first number wins, or the mark on the coin would overwrite the alloy.
        assertEquals(0.9999, silverFineness("""Plata 999,9 (Marked "PLATA 1000")"""))
        // Billon puts its fineness inside the bracket, apart from the alloy's name.
        assertEquals(0.4, silverFineness("Vellón (plata 400) (Copper .500, Nickel .050, Zinc .050)"))
    }

    /** A bare «Plata» gets no floor rather than a fineness of one. */
    @Test
    fun `a silver with no declared fineness has no floor at all`() {
        assertNull(silverFineness("Plata"))
        assertNull(silverFineness("Cuproníquel"))
        assertNull(silverFineness(null))
        // «Nickel silver» contains no silver.
        assertNull(silverFineness("Nickel silver 800"))
    }

    @Test
    fun `the silver floor is the fine silver and not the coin`() {
        val duro = meta(weightGrams = 25.0, fineness = 0.835)

        assertEquals(20.875, fineSilverGrams(duro))
        assertNull(fineSilverGrams(meta(weightGrams = 25.0, fineness = null)))
        assertNull(fineSilverGrams(meta(weightGrams = null, fineness = 0.925)))
        assertNull(fineSilverGrams(null))
    }

    /** Why it is a maximum: one source alone would value a duro below its own silver (#316). */
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

    /** E.g. the 2 Bolívares of 1879: Numista has no price for it and its silver is worth little. */
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

    /** `price` is paid per row: a lot of 102 bolívares does not cost 102 times its price (#316). */
    @Test
    fun `what was paid is divided by the pieces of its row`() {
        val lot = item(grade = "vf", price = 204.0, quantity = 102)

        assertEquals(2.0, pieceValue(lot, null, null, priceOf(emptyMap()))?.eur)
    }

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

    /** On a tie the worse grade wins: a valuation never rounds in the collector's favour. */
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

    /** `unc` is also the grade a hole is valued in. */
    @Test
    fun `an ungraded piece is valued uncirculated`() {
        val value = pieceValue(item(grade = null), null, null, priceOf(mapOf(UNCIRCULATED to 39.6)))

        assertEquals("unc", value?.grade)
        assertEquals(39.6, value?.eur)
    }

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

    /** Null rather than zero: it is what the coverage sentence of ADR 0028 §7 counts. */
    @Test
    fun `a piece no source covers is worth nothing that can be said`() {
        assertNull(pieceValue(item(grade = null, price = null), null, null, priceOf(emptyMap())))
    }

    /** Coverage counts pieces, not rows (ADR 0028 §7). */
    @Test
    fun `the total multiplies each row by its pieces and counts what it covered`() {
        val items = listOf(
            item(id = 1, grade = "unc", quantity = 3),
            item(id = 2, typeId = 2, grade = null, price = null),
        )

        val total = collectionValue(items, emptyMap(), null, priceOf(mapOf("unc" to 10.0)), NEVER_READ)

        assertEquals(30.0, total.eur)
        assertEquals(3, total.valued)
        assertEquals(4, total.pieces)
        assertTrue(!total.covered)
        // Nothing was ever read, so there is no catalogue date (#594).
        assertNull(total.catalogReadAt)
    }

    /**
     * Reads of different ages mix in one total: a marked casilla is repriced the day it is marked
     * (ADR 0029 §4) and a catalogue price lives ninety days (#561). The date shown is the oldest
     * (#494, #594).
     */
    @Test
    fun `a total is dated by the oldest read behind it`() {
        val june = 1_780_000_000_000
        val august = 1_786_000_000_000
        val items = listOf(
            item(id = 1, grade = "unc"),
            item(id = 2, typeId = 2, issueId = 9, grade = "unc"),
        )
        val reads = { typeId: Int, _: Int -> if (typeId == 1) august else june }

        // Both pieces are priced, so both reads are behind the total: the older one dates it.
        assertEquals(
            june,
            collectionValue(
                items,
                emptyMap(),
                null,
                { typeId, _, grade -> if (grade == "unc") 10.0 * typeId else null },
                reads,
            ).catalogReadAt,
        )
        // A piece no source covers is not in the total, so its read does not date it.
        assertEquals(
            august,
            collectionValue(items, emptyMap(), null, priceOf(mapOf("unc" to 10.0)), reads).catalogReadAt,
        )
    }

    /** The source is absent rather than zero. */
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

    /** `price` is per row and totalled as is; today's value is per piece and multiplied back up. */
    @Test
    fun `the paid comparison totals the row's price against what its pieces are worth today`() {
        val comparison = paidComparison(
            listOf(
                item(id = 1, grade = "unc", price = 30.0),
                item(id = 2, grade = "unc", price = 12.0, quantity = 3),
                // No price declared: left out of both sides.
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

    /** Rather than «pagaste 0 €»: an undeclared price is a gift or an inheritance (#491). */
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
     * What was paid is one of the three sources, so the comparison never shows a loss. Intended:
     * the page states the criterion («el mayor de tres precios»), and one rule means one value.
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

    /** Only two sources: nobody paid for a coin that is not there (ADR 0028 §8, #493). */
    @Test
    fun `a hole is valued uncirculated, and the metal can beat the catalogue`() {
        val catalogue = holeValue(
            typeId = 1,
            issueId = 7,
            meta = meta(weightGrams = 25.0, fineness = 0.835),
            spot = SPOT,
            prices = priceOf(mapOf(UNCIRCULATED to 80.0)),
        )

        assertEquals(ValueSource.Market, catalogue?.source)
        assertEquals(UNCIRCULATED, catalogue?.grade)
        assertEquals(80.0, catalogue?.eur)

        // A catalogue price below its own silver: the floor wins, as for a piece.
        val floor = holeValue(
            typeId = 1,
            issueId = 7,
            meta = meta(weightGrams = 25.0, fineness = 0.835),
            spot = SPOT,
            prices = priceOf(mapOf(UNCIRCULATED to 5.0)),
        )

        assertEquals(ValueSource.Silver, floor?.source)
    }

    /** The header labels the amount «en sin circular», so it cannot come from another grade. */
    @Test
    fun `a hole takes no neighbouring grade, because its label names the grade`() {
        val value = holeValue(
            typeId = 1,
            issueId = 7,
            meta = null,
            spot = null,
            prices = priceOf(mapOf("xf" to 60.0, "au" to 70.0)),
        )

        assertNull(value)
    }

    @Test
    fun `a hole with no issue and no metal costs nothing that can be said`() {
        assertNull(
            holeValue(
                typeId = 1,
                issueId = null,
                meta = meta(weightGrams = 25.0, fineness = 0.835),
                spot = null,
                prices = priceOf(mapOf(UNCIRCULATED to 80.0)),
            ),
        )

        assertEquals(
            ValueSource.Silver,
            holeValue(
                typeId = 1,
                issueId = null,
                meta = meta(weightGrams = 25.0, fineness = 0.835),
                spot = SPOT,
                prices = priceOf(mapOf(UNCIRCULATED to 80.0)),
            )?.source,
        )
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

/** No price ever read: every test that is not about dates. */
private val NEVER_READ: (Int, Int) -> Long? = { _, _ -> null }

/** Prices for issue 7 of type 1 by grade, shaped like the app's price book. */
private fun priceOf(grades: Map<String, Double>): (Int, Int, String) -> Double? =
    { typeId, issueId, grade ->
        if (typeId == 1 && issueId == 7) grades[grade] else null
    }
