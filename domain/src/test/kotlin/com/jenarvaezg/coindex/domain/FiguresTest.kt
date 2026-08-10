package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Everything «Las cifras» draws out of the APK, with not a single call (ADR 0028 §7).
 *
 * The numbers pinned here are the ones measured over the father's real collection on 10 August 2026 —
 * 572 pieces, 34 issuers, `docs/ux/cifras-316.md` and `docs/ux/cifras-326.md` — reduced to the smallest
 * inventory that exercises each rule. What the suite cannot hold is his collection itself: that is
 * `FieldReportTest`, and it stays inert without a private snapshot.
 */
class FiguresTest {
    /**
     * The metal splits by **mass and never by coin**, because by coin it is a bar of one colour: 565 of
     * his 574 pieces are silver, and by mass almost a kilo of the collection is not.
     */
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

    /**
     * A copper alloy counts as copper, which is what it mostly is, and every other base metal counts as
     * itself — so the bar grows on its own the day a steel coin arrives, which is what it was asked for.
     */
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

        // Copper before steel on a tie of mass, which is `metalOrder`: the order of the inventory must
        // not be able to swap two bands between launches.
        assertEquals(
            listOf(MetalMass(Metal.Copper, 30.0), MetalMass(Metal.Steel, 30.0)),
            split.masses,
        )
    }

    /**
     * A piece with no dominant metal is left **out** of the split rather than guessed at.
     *
     * The bimetallic 500 bolívares describes a core and a ring, and «Cobre recubierto de cuproníquel» is
     * a copper core clad in something else. Between them they are 0,1 % of his collection, and the
     * denominator says so instead of absorbing them.
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
     * The arc reads **Gregorian** years, and the undated inherit their type's minimum.
     *
     * Both halves are measured defects the prototype found: two of his pieces are dated in Hijri years and
     * read literally they stretch the axis to 711 years, and 23 rows carry no year at all — without the
     * inheritance the arc is 246 years instead of 1.756 (ADR 0026 §9, #326).
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

    /** No year anywhere is no arc, rather than an arc from zero. */
    @Test
    fun `a collection with no year at all has no arc`() {
        assertNull(yearArc(listOf(item(id = 1, typeId = 1)), mapOf(1 to meta())))
    }

    /**
     * The smallest against the largest, and **on a tie the older piece**.
     *
     * The tie is not hypothetical: four of his pieces are 42 mm, and what is worth drawing is the Maria
     * Theresa thaler of 1780 rather than whichever of them the inventory listed last.
     */
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

    /** One coin drawn against itself is not a comparison. */
    @Test
    fun `one measured coin is no size comparison`() {
        assertNull(
            sizeComparison(listOf(item(id = 1, typeId = 1)), mapOf(1 to meta(sizeMillimetres = 30.0))),
        )
    }

    /**
     * The four figures nobody asked for, out of the ficha that was already in the APK.
     *
     * The denominator of the demonetized share is the **whole collection** and not the types Numista
     * answered for: a percentage over a moving denominator is a figure nobody can check.
     */
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

    /**
     * The stack is the one figure the app gives extrapolated, and it says so with «unos».
     *
     * `thickness` is missing in a third of the types, so it is measured over the 449 of his 572 pieces
     * that carry one and scaled to all of them: 73,8 cm measured becomes «unos 94 cm»
     * (`docs/ux/cifras-316.md`).
     */
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

    /** Nothing measured extrapolates to nothing, and never to zero. */
    @Test
    fun `a magnitude nobody measured extrapolates to nothing`() {
        assertNull(Magnitude(value = 0.0, measuredPieces = 0, pieces = 12).extrapolated)
    }

    /**
     * The row and the area come off the diameter, and the spread is read in A4 sheets.
     *
     * 15,22 m in a row and 0,35 m² — 5,6 folios — over his collection, which is the figure that makes
     * «extendidas» mean anything at all.
     */
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

    /** The fine silver is the figure the spot multiplies, and it is not the weight. */
    @Test
    fun `the fine silver is counted apart from the weight`() {
        val figures = collectionFigures(
            listOf(item(id = 1, typeId = 1)),
            mapOf(1 to meta(weightGrams = 100.0, metal = Metal.Silver, fineness = 0.9)),
        )

        assertEquals(100.0, figures.weight.value)
        assertEquals(90.0, figures.fineSilver.value)
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
