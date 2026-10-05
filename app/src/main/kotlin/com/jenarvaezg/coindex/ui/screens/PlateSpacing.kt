package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.YearTagMetrics

/**
 * The plate's vertical gaps, kept together so they can be compared (#411). `PlateSpacingTest`
 * enforces the rule: more space between two members than inside one, so a year never reads as
 * belonging to the row below.
 *
 * Since #473 a casilla reads coin, tag, name, which makes these constants: the tag hangs
 * [underTheHole] below the hole, the name [insideMember] below the tag, and a short name's unused
 * height falls at the casilla's foot, adding to the row gap. None is in `sp`, so font scaling
 * doesn't change them.
 */
internal object PlateSpacing {
    /** Vertical padding around the name. */
    val namePadding: Dp = 6.dp

    /** From the bottom of a hole to the ink of its year, the same in every casilla. */
    val underTheHole: Dp = YearTagMetrics.slack

    /** From the ink of a year to the first line of its casilla's name. */
    val insideMember: Dp = YearTagMetrics.slack + namePadding

    /**
     * Gap between rows of casillas, twice `PlateMetrics.gutter`: side by side nothing gets
     * confused, but down the sheet a year must be clearly closer to its own coin. Costs about a
     * sixth of a row per screen. Kept here rather than in `PlateMetrics`, which holds dimensions
     * shared with the index.
     */
    val rowGap: Dp = 32.dp

    /**
     * From a casilla's name to the coins of the next row. A casilla without a name ends at its tag,
     * so it is only ever further from the next row.
     */
    val betweenMembers: Dp = namePadding + rowGap
}
