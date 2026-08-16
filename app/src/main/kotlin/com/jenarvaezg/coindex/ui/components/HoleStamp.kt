package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * What an empty casilla has to say, on a chip of paper laid inside the hole (#493, ADR 0029).
 *
 * Like the note left in an empty pocket: the price of the coin that is not there, and whether the
 * collector is looking for it, in the place the coin would be. The header three lines above says what
 * closing the whole plate costs and out of what criterion; the chip is what says **which** hole is
 * which — with seven holes of one bullion date run, one figure in the header cannot.
 *
 * **One chip and not two things over the same coin.** #493 drew the price here and left the
 * instruction: a wish is *a marked casilla*, so its mark and the price want the same centimetre of the
 * same 104 dp hole, and whoever drew the second drew the two together. So this says the mark, the
 * amount, or the mark over the amount — two lines at most, and the mark goes on top because it is what
 * the collector put there.
 *
 * **It covers the ghost, and that is accepted.** The 14 % catalog design under it is the only thing an
 * empty casilla has to say which coin is missing, and in 104 dp there is no room for both. It was
 * measured on the prototype of #500 before it was drawn here, and the trade is #493's own: what the
 * collector cannot see in the ghost is one tap away in the year's tag, and the price is not.
 *
 * It cannot collide with the completion stamp — that one falls on the ratio over the title, in the
 * other corner (ADR 0026 §3) — and it never lands on a full casilla, which has no cost and nothing to
 * look for.
 */
@Composable
fun HoleStamp(
    cost: String?,
    wished: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Whether the marking mode is open **on this casilla**, which is what the chip says in advance
     * (#517).
     *
     * With the mode open and the casilla not marked yet, the word is printed as a ghost: the note
     * this pocket is about to get, at the strength the catalog design under it is drawn at. It is
     * what tells a hole that answers the tap from one that has stepped back — pressing it inks the
     * same word in the same place, and pressing it again takes it out.
     *
     * It carries **no semantics**: to a screen reader a casilla that is not marked is not marked,
     * and what the tap does is already announced by the hole's own click label. A ghost is a thing
     * for eyes.
     */
    markable: Boolean = false,
) {
    if (cost == null && !wished && !markable) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Paper.paper.copy(alpha = CHIP_OPACITY))
            .border(CHIP_RULE, Paper.rust.copy(alpha = CHIP_RULE_OPACITY))
            .padding(horizontal = CHIP_PADDING_H, vertical = CHIP_PADDING_V),
    ) {
        if (wished) {
            Text(
                WishLabels.MARK_WORD,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.moss,
                textAlign = TextAlign.Center,
            )
        } else if (markable) {
            Text(
                WishLabels.MARK_WORD,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.moss.copy(alpha = GHOST_MARK_OPACITY),
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        cost?.let { amount ->
            Text(
                amount,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.rust,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Paper over the coin, and not opaque: at 92 % the ghost still shows around the digits, which is what
 * keeps the chip a note laid on the design rather than a hole punched in it.
 */
private const val CHIP_OPACITY = 0.92f

/**
 * The mark before it is made: the same word, at the strength of the ghost it is laid over (#517).
 *
 * Deliberately the same order of magnitude as the 14 % catalog design under the chip — it is a note
 * that has not been written yet, and anything darker would read as a mark somebody already made.
 */
private const val GHOST_MARK_OPACITY = 0.3f

/** The rule of the chip: the same hairline weight the plate rules everything else with. */
private val CHIP_RULE = 1.dp
private const val CHIP_RULE_OPACITY = 0.5f

private val CHIP_PADDING_H = 7.dp
private val CHIP_PADDING_V = 3.dp
