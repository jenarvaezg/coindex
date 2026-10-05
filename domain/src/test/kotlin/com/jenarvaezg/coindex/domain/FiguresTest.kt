package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * «Las cifras», computed from the APK without a single call (ADR 0028 §7), over the smallest
 * inventory that exercises each rule (`docs/ux/cifras-316.md`, `docs/ux/cifras-326.md`). The real
 * collection is `FieldReportTest`'s job.
 */
class FiguresTest {
    /** By coin the bar would be almost all silver; by mass the base metals show. */
    @Test
    fun `the metal is split by mass, and the alloy of a silver coin is copper`() {
        val split = metalSplit(
            listOf(item(id = 1, typeId = 1)),
            mapOf(1 to meta(weightGrams = 100.0, metal = Metal.Silver, fineness = 0.835)),
        )

        assertEquals(
            listOf(MetalMass(Metal.Silver, 83.5), MetalMass(Metal.Copper, 16.5)),
            split.masses,
        )
        assertEquals(0.835, split.shareOf(split.masses.first()))
        assertEquals(100.0, split.measuredGrams)
    }

    /** A new base metal gets its own band when its first coin arrives. */
    @Test
    fun `a copper alloy is copper and every other base metal is itself`() {
        val split = metalSplit(
            listOf(item(id = 1, typeId = 1), item(id = 2, typeId = 2), item(id = 3, typeId = 3)),
            mapOf(
                1 to meta(weightGrams = 10.0, metal = Metal.Cupronickel),
                2 to meta(weightGrams = 20.0, metal = Metal.Bronze),
                3 to meta(weightGrams = 30.0, metal = Metal.Steel),
            ),
        )

        // Copper before steel on a tie (`metalOrder`), so inventory order can't swap two bands
        // between launches.
        assertEquals(
            listOf(MetalMass(Metal.Copper, 30.0), MetalMass(Metal.Steel, 30.0)),
            split.masses,
        )
    }

    /**
     * Left out rather than guessed: the bimetallic 500 bolívares, or «Cobre recubierto de
     * cuproníquel». The denominator shows the gap instead of absorbing it.
     */
    @Test
    fun `a piece with no dominant metal is measured by nobody`() {
        val split = metalSplit(
            listOf(item(id = 1, typeId = 1), item(id = 2, typeId = 2)),
            mapOf(
                1 to meta(weightGrams = 90.0, metal = Metal.Silver, fineness = 1.0),
                2 to meta(weightGrams = 10.0, metal = Metal.Other),
            ),
        )

        assertEquals(listOf(MetalMass(Metal.Silver, 90.0)), split.masses)
        assertEquals(90.0, split.measuredGrams)
        assertEquals(100.0, split.grams)
    }

    /**
     * Hijri years read literally would stretch the axis, and dropping the undated would shrink it
     * (ADR 0026 §9, #326).
     */
    @Test
    fun `the arc is Gregorian, and the undated inherit their type's minimum`() {
        val arc = yearArc(
            listOf(
                // The ½ Dirham: engraved 1316 of the Hijri calendar, struck in 1899.
                item(id = 1, typeId = 1).copy(issueYear = 1_316, gregorianYear = 1_899),
                // An undated Roman denarius: no year of its own at all.
                item(id = 2, typeId = 2),
                item(id = 3, typeId = 3).copy(gregorianYear = 2_026),
            ),
            mapOf(
                1 to meta(),
                2 to meta().copy(minYear = 270),
                3 to meta(),
            ),
        )

        assertEquals(YearArc(270, 2_026), arc)
        assertEquals(1_756, arc?.years)
    }

    /** Rather than an arc from year zero. */
    @Test
    fun `a collection with no year at all has no arc`() {
        assertNull(yearArc(listOf(item(id = 1, typeId = 1)), mapOf(1 to meta())))
    }

    /** Ties happen (several 42 mm thalers), and the older piece beats inventory order. */
    @Test
    fun `the size extremes break a tie by the older piece`() {
        val comparison = sizeComparison(
            listOf(
                item(id = 1, typeId = 1).copy(gregorianYear = 1_899),
                item(id = 2, typeId = 2).copy(gregorianYear = 1_975),
                item(id = 3, typeId = 3).copy(gregorianYear = 1_780),
            ),
            mapOf(
                1 to meta(sizeMillimetres = 14.5),
                2 to meta(sizeMillimetres = 42.0),
                3 to meta(sizeMillimetres = 42.0),
            ),
        )

        assertEquals(14.5, comparison?.smallest?.millimetres)
        assertEquals(1_899, comparison?.smallest?.item?.gregorianYear)
        assertEquals(42.0, comparison?.largest?.millimetres)
        assertEquals(1_780, comparison?.largest?.item?.gregorianYear)
    }

    @Test
    fun `one measured coin is no size comparison`() {
        assertNull(
            sizeComparison(listOf(item(id = 1, typeId = 1)), mapOf(1 to meta(sizeMillimetres = 30.0))),
        )
    }

    /** A share over only the types Numista answered for would have a moving denominator. */
    @Test
    fun `the margins count pieces over the whole collection, and silence is not a no`() {
        val margins = marginFigures(
            listOf(
                item(id = 1, typeId = 1, quantity = 3).copy(gregorianYear = 1_960),
                item(id = 2, typeId = 2).copy(gregorianYear = 1_960),
                item(id = 3, typeId = 3),
            ),
            mapOf(
                1 to meta().copy(
                    demonetized = true,
                    hands = listOf("Désiré-Albert Barre", "Désiré-Albert Barre"),
                    mints = listOf("Casa de la Moneda de París"),
                ),
                2 to meta().copy(demonetized = true, mints = listOf("Royal Mint (Tower Hill)")),
                // Numista says nothing about this one, which is not «still legal tender».
                3 to meta(),
            ),
        )

        assertEquals(MarginFigure(4, 5), margins.demonetized)
        // Three and not six: a hand credited on both faces of one type is one hand.
        assertEquals(MarginFigure(3, 5, "Désiré-Albert Barre"), margins.sameHand)
        assertEquals(MarginFigure(3, 5, "Casa de la Moneda de París"), margins.mostMinted)
        assertEquals(2, margins.distinctMints)
        assertEquals(MarginFigure(4, 5, "1960"), margins.commonestYear)
    }

    /** By piece, like the rest of the page: worn bulks invert what the rows would say (#491). */
    @Test
    fun `the uncirculated figure counts pieces and holds both grades`() {
        val margins = marginFigures(
            listOf(
                item(id = 1, typeId = 1).copy(grade = "unc"),
                item(id = 2, typeId = 1).copy(grade = "AU"),
                // A worn bulk counts as 102 pieces, not one row.
                item(id = 3, typeId = 1, quantity = 102).copy(grade = "f"),
                // No grade is not «circulated»: it counts in the denominator and in nothing else.
                item(id = 4, typeId = 1),
            ),
            mapOf(1 to meta()),
        )

        assertEquals(MarginFigure(2, 105), margins.uncirculated)
    }

    /** Like every margin figure except the demonetized share, whose zero is a real reading. */
    @Test
    fun `no uncirculated piece is silence and not a zero`() {
        val margins = marginFigures(
            listOf(item(id = 1, typeId = 1).copy(grade = "f"), item(id = 2, typeId = 1)),
            mapOf(1 to meta()),
        )

        assertNull(margins.uncirculated)
    }

    /** The only extrapolated figure, so it says «unos»: many types lack `thickness` (#316). */
    @Test
    fun `the stack is measured over the pieces that have a thickness and scaled to all of them`() {
        val figures = collectionFigures(
            listOf(item(id = 1, typeId = 1), item(id = 2, typeId = 2)),
            mapOf(
                1 to meta(weightGrams = 25.0, sizeMillimetres = 37.0).copy(
                    thicknessMillimetres = 2.5,
                ),
                2 to meta(weightGrams = 25.0, sizeMillimetres = 37.0),
            ),
        )

        assertEquals(0.25, figures.stack.value)
        assertEquals(1, figures.stack.measuredPieces)
        assertEquals(2, figures.stack.pieces)
        assertTrue(!figures.stack.complete)
        assertEquals(0.5, figures.stack.extrapolated)
        // Everything else is measured over both, so it carries no «unos».
        assertTrue(figures.weight.complete)
        assertEquals(50.0, figures.weight.value)
    }

    @Test
    fun `a magnitude nobody measured extrapolates to nothing`() {
        assertNull(Magnitude(value = 0.0, measuredPieces = 0, pieces = 12).extrapolated)
    }

    /** The spread is read in A4 sheets. */
    @Test
    fun `the row is diameters end to end and the area is circles`() {
        val figures = collectionFigures(
            listOf(item(id = 1, typeId = 1, quantity = 4)),
            mapOf(1 to meta(weightGrams = 25.0, sizeMillimetres = 40.0)),
        )

        assertEquals(0.16, figures.row.value)
        assertEquals(0.005, round(figures.area.value, 3))
        assertEquals(0.08, round(figures.area.a4Sheets(), 2))
        assertEquals(4, figures.pieces)
        assertEquals(1, figures.types)
    }

    /** The spot multiplies the fine silver, not the weight. */
    @Test
    fun `the fine silver is counted apart from the weight`() {
        val figures = collectionFigures(
            listOf(item(id = 1, typeId = 1)),
            mapOf(1 to meta(weightGrams = 100.0, metal = Metal.Silver, fineness = 0.9)),
        )

        assertEquals(100.0, figures.weight.value)
        assertEquals(90.0, figures.fineSilver.value)
    }

    /** The sewn edge and «La materia» share one census (#400). */
    @Test
    fun `a coerced zero still counts as one piece in the figures census`() {
        val items = listOf(
            item(id = 1, typeId = 1, quantity = 3),
            item(id = 2, typeId = 2, quantity = 0),
        )
        assertEquals(4, collectionFigures(items, emptyMap()).pieces)
    }
}

private fun round(value: Double, decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return Math.round(value * factor) / factor
}

private fun meta(
    weightGrams: Double? = null,
    metal: Metal? = null,
    fineness: Double? = null,
    sizeMillimetres: Double? = null,
) = TypeMeta(
    id = 1,
    weightOz = weightGrams?.let(::gramsToOunces),
    metal = metal,
    fineness = fineness,
    sizeMillimetres = sizeMillimetres,
)

private fun item(id: Long, typeId: Int, quantity: Int = 1) =
    CollectedItem(id = id, quantity = quantity, typeId = typeId)
