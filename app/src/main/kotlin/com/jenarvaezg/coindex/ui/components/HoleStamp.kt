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
 * A paper chip inside an empty casilla (#493, ADR 0029): the wish mark, the price of the missing
 * coin, or the mark over the price. The header gives the plate's closing cost; the chip tells the
 * holes apart. It covers the 14 % ghost, accepted because the coin's identity is one tap away in
 * the year tag and the price isn't (#500). Never on a full casilla.
 */
@Composable
fun HoleStamp(
    cost: String?,
    wished: Boolean,
    modifier: Modifier = Modifier,
    /**
     * Whether the marking mode is open on this casilla (#517). If it isn't marked yet, the mark
     * word is shown faintly, previewing what a tap inks. Purely visual: no semantics, since the
     * hole's click label already announces the action.
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

/** Not quite opaque, so the ghost still shows around the digits. */
private const val CHIP_OPACITY = 0.92f

/**
 * The mark before it is made (#517), close to the 14 % ghost under it; darker would read as already
 * marked.
 */
private const val GHOST_MARK_OPACITY = 0.3f

/** The plate's hairline weight. */
private val CHIP_RULE = 1.dp
private const val CHIP_RULE_OPACITY = 0.5f

private val CHIP_PADDING_H = 7.dp
private val CHIP_PADDING_V = 3.dp
