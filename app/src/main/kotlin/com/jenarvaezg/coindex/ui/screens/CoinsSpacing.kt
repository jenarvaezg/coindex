package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The vertical blanks of the Coins grid, gathered so they can be compared (#511).
 *
 * `PlateSpacing` did this arithmetic for the plate — **more air between two members than inside one**
 * — and the grid of Coins was never held to it. Measured on the AVD at 420 dpi, a card's year sat
 * **8,8 dp under its own cartouche and 14,5 dp over the coin of the row below**: a ratio of 1,6, which
 * on paper passes and on screen does not. What the arithmetic misses is that the cartouche is a filled
 * recess and the year is not in it — the card *closes* at the cartouche's bottom rule, so the year is
 * already outside its own card, floating on the board with a 104 dp coin under it. «2008 · 2009 · 2010»
 * read as the heading of the row beneath, which is the same defect #411 found and the reason it wrote
 * its rule down instead of nudging a number.
 *
 * So the seam between two rows grows and the blank inside the card does not. The cost is real and it is
 * the one the plate already paid: about a sixth of a row per screen.
 */
internal object CoinsSpacing {
    /** From the cartouche's bottom rule down to the line box of the year that belongs to it. */
    val underTheCartouche: Dp = 3.dp

    /** What a card keeps under its last line before the seam starts. */
    val cardFoot: Dp = 4.dp

    /**
     * What the grid puts between two rows of cards, over and above [cardFoot].
     *
     * Eighteen and not the album's six: six was inherited from a grid whose cards ended **at** the
     * cartouche, and the year has hung under it since #337. Measured again on the AVD after the change:
     * 8,8 dp to its own card and 26,3 dp to the next one, which is the three-to-one the plate reads at.
     */
    val rowSeam: Dp = 18.dp

    /** From the last thing a card prints down to the coins of the next row. */
    val betweenCards: Dp = cardFoot + rowSeam
}
