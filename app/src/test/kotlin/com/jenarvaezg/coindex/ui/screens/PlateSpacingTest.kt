package com.jenarvaezg.coindex.ui.screens

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
