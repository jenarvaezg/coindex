package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.components.YearTagMetrics

/**
 * The vertical blanks of the plate, gathered where they can be compared (#411).
 *
 * A casilla is read downwards and each of those blanks was decided on its own: six dp of breathing
 * room here, the ten the year's 48 dp target leaves around its ink there, the gutter between two
 * rows over in the grid. Nobody had ever added them up, and the sum said something the drawing did
 * not intend: the year of a row sat **further from its own coin** than from the coins of the row
 * underneath. With a row cut off at the top of the screen — the only thing you see is a strip of
 * years and then coins — «2018 · 2018 · 2019» labelled the wrong row.
 *
 * So the numbers live here and the rule lives in `PlateSpacingTest`: **more air between two members
 * than inside one**. That is the whole of proximity, and it is arithmetic and not a screenshot.
 *
 * **Since #473 the casilla is read coin → tag → name**, and that is what makes the three distances
 * below constants instead of functions of what each row reserved. The tag hangs off the hole by
 * [underTheHole] whatever the casilla is; the name hangs off the tag by [insideMember] whatever the
 * name is; and what a name did not use falls at the **foot** of the casilla, where it is added to
 * the gap between two rows and cannot be read as anything but separation. None of the three is
 * measured in `sp`, so none of them moves when the collector enlarges the type — which is the half
 * of #473 that no amount of [rowGap] could have bought.
 */
internal object PlateSpacing {
    /** The breathing room the name keeps between the tag above it and the next row below. */
    val namePadding: Dp = 6.dp

    /**
     * From the cardboard under a hole down to the ink of its year, and the same for every casilla.
     *
     * A date run had this since always; what #473 gave the rest of the plate is that a titled
     * casilla has it too, because the name no longer sits in between.
     */
    val underTheHole: Dp = YearTagMetrics.slack

    /** From the ink of a year down to the first line of the name that shares its casilla. */
    val insideMember: Dp = YearTagMetrics.slack + namePadding

    /**
     * What the grid spaces two rows of casillas by, and twice `PlateMetrics.gutter` on purpose.
     *
     * The gutter separates two members side by side, where nothing can be confused: a year is under
     * its own coin and under no other. Down the sheet the members queue up, and a year as far from
     * its coin as from the coins below belongs to whichever of the two the eye reaches first.
     * Sixteen dp did that; thirty-two puts the frank separation of an album sheet between one row
     * and the next, at a cost of a sixth of a row per screen. It lives here and not beside the
     * gutter in `PlateMetrics`, which is the dimensions the plate *shares* with the index: no card
     * lines up against this one, and moving proximity should not mean touching three files.
     */
    val rowGap: Dp = 32.dp

    /**
     * From the last thing a casilla prints down to the coins of the next row.
     *
     * The name, where there is one — a casilla with no name ends at its tag and is therefore
     * *further* from the row below, never nearer, which is the whole of #473.
     */
    val betweenMembers: Dp = namePadding + rowGap
}
