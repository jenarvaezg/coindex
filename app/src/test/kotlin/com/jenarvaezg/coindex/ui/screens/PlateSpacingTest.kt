package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.ui.components.YearTagMetrics
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Proximity on the plate is a comparison and never a value, so it is tested as one (#411).
 *
 * Each thing a casilla prints has two neighbours down the sheet, and the audit of 11 August found
 * two of them at almost the same distance: a row cut off at the top of the screen made its years
 * read as labels of the row below. Since #473 the casilla is read coin → tag → name, so what has to
 * be compared is every step of that descent against the one gap that is not inside a member.
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

    /**
     * The year belongs to the coin above it before it belongs to the name below it, which is what
     * the order of #473 says out loud: the tag is the label of a hole, and the name is a gloss on
     * the tag.
     */
    @Test
    fun `a year hangs off its coin nearer than off its own name`() {
        assertTrue(PlateSpacing.underTheHole < PlateSpacing.insideMember)
    }

    /**
     * A casilla with no name ends at its tag, so the row below is further from it and never nearer
     * — which is the defect #473 reported, said as the comparison it always was.
     *
     * What the tag of such a casilla has under it is [PlateSpacing.rowGap] **at least**: the row is
     * as tall as its tallest casilla, so a nameless one beside a named one gets that name's lines
     * added to its own foot.
     */
    @Test
    fun `a casilla with no name keeps its year nearer its coin than the row below`() {
        val untilTheNextRow = YearTagMetrics.slack + PlateSpacing.rowGap

        assertTrue(PlateSpacing.underTheHole < untilTheNextRow)
        assertTrue(untilTheNextRow >= PlateSpacing.underTheHole * 2)
    }
}
