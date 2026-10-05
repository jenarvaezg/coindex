package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The vertical gaps of the Monedas grid, kept together so they can be compared (#511).
 *
 * Same rule as [PlateSpacing]: more space between two cards than inside one. The year hangs below
 * the cartouche, outside the card's filled recess, so with a small row gap it read as the heading
 * of the row below. The cost is about a sixth of a row per screen.
 */
internal object CoinsSpacing {
    /** From the cartouche's bottom rule to the year's line box. */
    val underTheCartouche: Dp = 3.dp

    /** Space a card keeps under its last line. */
    val cardFoot: Dp = 4.dp

    /**
     * Gap between two rows of cards, on top of [cardFoot]. 18 dp puts the year about three times
     * closer to its own card than to the next row, the ratio the plate uses.
     */
    val rowSeam: Dp = 18.dp

    /** From a card's last line to the coins of the next row. */
    val betweenCards: Dp = cardFoot + rowSeam
}
