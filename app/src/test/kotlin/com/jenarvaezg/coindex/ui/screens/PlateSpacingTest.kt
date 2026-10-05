package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.ui.components.YearTagMetrics
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Proximity on the plate is tested as a comparison, not a value (#411). A casilla reads coin → tag
 * → name (#473), and each step must stay nearer than the gap to the next member.
 */
class PlateSpacingTest {
    @Test
    fun `a year is at least twice as far from the next row as from its own coin`() {
        assertTrue(
            PlateSpacing.betweenMembers >= PlateSpacing.underTheHole * 2,
            "${PlateSpacing.betweenMembers} of air between members is not twice the " +
                "${PlateSpacing.underTheHole} from a coin to its year",
        )
    }

    @Test
    fun `a name is at least twice as far from the next row as from its own year`() {
        assertTrue(
            PlateSpacing.betweenMembers >= PlateSpacing.insideMember * 2,
            "${PlateSpacing.betweenMembers} of air between members is not twice the " +
                "${PlateSpacing.insideMember} from a year to its name",
        )
    }

    /** The tag labels the hole, and the name glosses the tag (#473). */
    @Test
    fun `a year hangs off its coin nearer than off its own name`() {
        assertTrue(PlateSpacing.underTheHole < PlateSpacing.insideMember)
    }

    /**
     * A nameless casilla ends at its tag (#473). [PlateSpacing.rowGap] is the least it has below:
     * a row is as tall as its tallest casilla.
     */
    @Test
    fun `a casilla with no name keeps its year nearer its coin than the row below`() {
        val untilTheNextRow = YearTagMetrics.slack + PlateSpacing.rowGap

        assertTrue(PlateSpacing.underTheHole < untilTheNextRow)
        assertTrue(untilTheNextRow >= PlateSpacing.underTheHole * 2)
    }
}
