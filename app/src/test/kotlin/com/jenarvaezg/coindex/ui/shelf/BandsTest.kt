package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.gramsToOunces
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where the chips of the two shelves put a weight and a year.
 *
 * The property that matters across all four is that **every band is total**: a row with no value lands
 * on a chip of its own rather than on none, because a row no chip of a facet can reach is a row that
 * disappears without anything on screen saying so.
 */
class BandsTest {
    @Test
    fun `an ounce of silver is the ounce band, and 25 grams is not`() {
        assertEquals(GramBand.Ounce, GramBand.of(gramsToOunces(31.103)))
        // The module of the 22 Venezuelan fuertes: 25 g of .900, which its label calls 10 – 25 g.
        assertEquals(GramBand.TenToTwentyFive, GramBand.of(gramsToOunces(25.0)))
        assertEquals(GramBand.UnderTen, GramBand.of(gramsToOunces(2.5)))
        assertEquals(GramBand.OverThirtyFour, GramBand.of(gramsToOunces(62.42)))
    }

    @Test
    fun `a weight nobody recorded is Sin peso, not the smallest band`() {
        assertEquals(GramBand.Unweighed, GramBand.of(null))
        assertEquals(GramBand.Unweighed, GramBand.of(0.0))
        assertEquals(GramBand.Unweighed, GramBand.of(Double.NaN))
    }

    @Test
    fun `the year bands split where the two collections do`() {
        assertEquals(YearBand.BeforeNineteenHundred, YearBand.of(1874))
        assertEquals(YearBand.ThroughTheSeventies, YearBand.of(1966))
        assertEquals(YearBand.ThroughTheSeventies, YearBand.of(1979))
        assertEquals(YearBand.EightiesAndNineties, YearBand.of(1980))
        assertEquals(YearBand.SinceTwoThousand, YearBand.of(2026))
        // The two unpublished submissions of #186 have no year, and «Sin año» is their chip.
        assertEquals(YearBand.Undated, YearBand.of(null))
    }

    @Test
    fun `a collection with no single weight is a set or a box, not zero ounces`() {
        assertEquals(OunceBand.Spanning, OunceBand.of(null))
        assertEquals(OunceBand.UnderHalf, OunceBand.of(250))
        assertEquals(OunceBand.HalfToOne, OunceBand.of(500))
        assertEquals(OunceBand.HalfToOne, OunceBand.of(1_000))
        assertEquals(OunceBand.OverOne, OunceBand.of(2_000))
    }

    @Test
    fun `where a collection starts is measured off its earliest coin`() {
        assertEquals(StartBand.BeforeFifty, StartBand.of(1874))
        assertEquals(StartBand.FiftyToNinetyNine, StartBand.of(1966))
        assertEquals(StartBand.SinceTwoThousand, StartBand.of(2020))
        // A collection whose types no ficha has arrived for yet is one sync from a date.
        assertEquals(StartBand.Unknown, StartBand.of(null))
    }
}
