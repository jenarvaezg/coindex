package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.fieldTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proximity on the plate is a comparison and never a value, so it is tested as one (#411).
 *
 * The year of a casilla has two neighbours down the sheet — the name it belongs to, above it, and
 * the coins of the next row, below it — and the audit of 11 August found them at almost the same
 * distance: a row cut off at the top of the screen made its years read as labels of the row below.
 * These are the two distances that were confused, and the rule is that one is at least twice the
 * other.
 */
class PlateSpacingTest {
    @Test
    fun `a year is at least twice as far from the next row as from its own name`() {
        assertTrue(
            PlateSpacing.betweenMembers >= PlateSpacing.insideMember * 2,
            "${PlateSpacing.betweenMembers} of air between members is not twice the " +
                "${PlateSpacing.insideMember} inside one",
        )
    }

    /**
     * The name is centred in the box its row reserved since #412, so the shortest name on the
     * tallest row hands its year more air than the factor of two above allows — and that is the one
     * place the rule of this file was re-decided rather than kept.
     *
     * What #411 was defending is what is checked here instead: a year still belongs to the name over
     * it and not to the coins under it. «Onza Troy» beside a three-line neighbour is the worst case
     * there is, and the ceiling of `plateNameLinesCeiling` is what keeps it from ever being worse.
     */
    @Test
    fun `even the shortest name on the tallest row keeps its year on its own side`() {
        val worst = PlateSpacing.insideMemberCentred(
            reserved = 3,
            used = 1,
            lineHeight = PlateSpacing.nameLine.value.dp,
        )

        assertTrue(
            worst < PlateSpacing.betweenMembers,
            "$worst of air under a centred one-line name reaches the " +
                "${PlateSpacing.betweenMembers} that separate two members",
        )
    }

    /**
     * And the ceiling is what makes that true rather than luck: a line of 21 dp clears three lines,
     * and the collector who enlarges the type past a quarter is given two.
     */
    @Test
    fun `the third line is bought only while the air lasts`() {
        assertEquals(3, plateNameLinesCeiling(21.dp))
        assertEquals(3, plateNameLinesCeiling(25.dp))
        assertEquals(2, plateNameLinesCeiling(26.dp))
        assertEquals(2, plateNameLinesCeiling(42.dp))
    }

    /** A date run prints no name, and then the year hangs straight off the cardboard above it. */
    @Test
    fun `a casilla with no name keeps its year nearer still`() {
        assertTrue(PlateSpacing.underTheHole < PlateSpacing.insideMember)
        assertTrue(PlateSpacing.betweenMembers >= PlateSpacing.underTheHole * 2)
    }

    /**
     * The blank the reserved name box leaves under the hole when the name takes one of its two
     * lines — the price of tags that line up (#337), and the only gap inside a member that the
     * separation between members has to beat on its own.
     */
    @Test
    fun `even the reserved line under a hole stays inside its own member`() {
        assertTrue(PlateSpacing.betweenMembers > PlateSpacing.reservedNameLine)
    }

    /** The copy of the theme's line is arithmetic only while it is still the theme's line. */
    @Test
    fun `the mirrored line is the one the name box measures`() {
        assertEquals(fieldTypography.titleMedium.lineHeight, PlateSpacing.nameLine)
    }
}
