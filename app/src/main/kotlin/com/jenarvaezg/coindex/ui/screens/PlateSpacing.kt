package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.ui.components.YearTagMetrics

/**
 * The vertical blanks of the plate, gathered where they can be compared (#411).
 *
 * A casilla is read downwards — coin, name, sunken tag — and each of those blanks was decided on
 * its own: six dp of breathing room here, the ten the year's 48 dp target leaves around its ink
 * there, the gutter between two rows over in the grid. Nobody had ever added them up, and the sum
 * said something the drawing did not intend: the year of a row sat **further from its own name**
 * than from the coins of the row underneath. With a row cut off at the top of the screen — the only
 * thing you see is a strip of years and then coins — «2018 · 2018 · 2019» labelled the wrong row,
 * and the way out was to recognise the coins.
 *
 * So the numbers live here and the rule lives in `PlateSpacingTest`: **more air between two members
 * than inside one**, twice as much. That is the whole of proximity, and it is arithmetic and not a
 * screenshot.
 */
internal object PlateSpacing {
    /** The breathing room the name keeps between the hole above it and the tag below. */
    val namePadding: Dp = 6.dp

    /**
     * `titleMedium`'s line, mirrored from the theme so the arithmetic below can be read here.
     *
     * It stays a `TextUnit` and not a `Dp` on purpose: the name box measures the real one against
     * the density, because it has to grow with the collector's font scale, and a test pins this
     * copy to the typography — unit included — so the two cannot drift apart.
     */
    val nameLine: TextUnit = 21.sp

    /** From the cardboard of a nameless casilla down to the ink of its year: a date run. */
    val underTheHole: Dp = YearTagMetrics.slack

    /** From the last line of a name down to the ink of the year that belongs to it. */
    val insideMember: Dp = namePadding + YearTagMetrics.slack

    /**
     * The line the reserved name box leaves empty under a hole when its name takes only one of the
     * two — the price of the tags of a row sharing one baseline (#337), and the widest blank there
     * is inside a member since the name sank to the bottom of its box.
     *
     * At font scale 1, which is where it can be compared with the rest. Two things it does not
     * cover, both of them the same shape and both older than this file: a casilla with **no** name
     * inside a row that has one reserves the whole box and not a line of it, and a collector who
     * enlarges the type grows the box while the gap between rows stays put. See #473.
     */
    val reservedNameLine: Dp = nameLine.value.dp + namePadding

    /**
     * What the grid spaces two rows of casillas by, and twice `PlateMetrics.gutter` on purpose.
     *
     * The gutter separates two members side by side, where nothing can be confused: a year is under
     * its own coin and under no other. Down the sheet the members queue up, and a year as far from
     * its name as from the coins below belongs to whichever of the two the eye reaches first.
     * Sixteen dp did that; thirty-two puts the frank separation of an album sheet between one row
     * and the next, at a cost of a sixth of a row per screen. It lives here and not beside the
     * gutter in `PlateMetrics`, which is the dimensions the plate *shares* with the index: no card
     * lines up against this one, and moving proximity should not mean touching three files.
     */
    val rowGap: Dp = 32.dp

    /** From the ink of a year down to the coins of the next row. */
    val betweenMembers: Dp = YearTagMetrics.slack + rowGap
}
