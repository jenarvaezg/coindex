package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * What one empty casilla costs, on a chip of paper laid inside the hole (#493).
 *
 * Like the note left in an empty pocket: it is the price of the coin that is not there, in the place
 * the coin would be. The header three lines above says what closing the whole plate costs and out of
 * what criterion; the stamp is what says **which** hole is which — with seven holes of one bullion
 * date run, one figure in the header cannot.
 *
 * **It covers the ghost, and that is accepted.** The 14 % catalog design under it is the only thing an
 * empty casilla has to say which coin is missing, and in 104 dp there is no room for both. It was
 * measured on the prototype before it was drawn here, and the trade is the ticket's own: what the
 * collector cannot see in the ghost is one tap away in the year's tag, and the price is not.
 *
 * It cannot collide with the completion stamp — that one falls on the ratio over the title, in the
 * other corner (ADR 0026 §3) — and it never lands on a full casilla, which has no cost at all.
 *
 * **The one thing still to be drawn next to it is the wish of #497**, which had decided nothing when
 * this shipped: a wish is *a marked casilla*, so its mark and this chip want the same centimetre of the
 * same 104 dp hole. Whoever draws that one draws the two together, here, rather than adding a second
 * thing over the same coin.
 */
@Composable
fun HoleCostStamp(cost: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Paper.paper.copy(alpha = CHIP_OPACITY))
            .border(CHIP_RULE, Paper.rust.copy(alpha = CHIP_RULE_OPACITY))
            .padding(horizontal = CHIP_PADDING_H, vertical = CHIP_PADDING_V),
    ) {
        Text(
            cost,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.rust,
        )
    }
}

/**
 * Paper over the coin, and not opaque: at 92 % the ghost still shows around the digits, which is what
 * keeps the chip a note laid on the design rather than a hole punched in it.
 */
private const val CHIP_OPACITY = 0.92f

/** The rule of the chip: the same hairline weight the plate rules everything else with. */
private val CHIP_RULE = 1.dp
private const val CHIP_RULE_OPACITY = 0.5f

private val CHIP_PADDING_H = 7.dp
private val CHIP_PADDING_V = 3.dp
