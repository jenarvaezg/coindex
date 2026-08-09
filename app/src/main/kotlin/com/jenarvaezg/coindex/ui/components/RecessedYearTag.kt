package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The year under a hole, as a tag sunk into the cardboard.
 *
 * It is the affordance turned into form the #302 map went looking for: no colour, no underline and
 * no arrow — the year can be pressed because it *looks* like a separate piece pressed into the
 * sheet. The arrow is what the map could not afford: neither Bitter nor Barlow carries that glyph
 * (#298), so twenty-two of them would be twenty-two letters of the system typeface on a sheet of
 * paper.
 *
 * 48,3 × 28 dp is the drawing the map measured, and it is the only one of its four candidates
 * whose ink reaches Android's 48 dp. None of the four reached 48 dp *tall*, so the tag buys the
 * rest of its target area with [minimumInteractiveComponentSize] instead of pretending its ink
 * is bigger than it is.
 *
 * [onOpen] is null for an announced casilla, which has no Numista page: the tag is then a label
 * and not a target, and it says so by not taking the click.
 */
@Composable
fun RecessedYearTag(
    year: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tag = Modifier
        .widthIn(min = TAG_WIDTH)
        .height(TAG_HEIGHT)
        .recessedInBoard()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(
                if (onOpen != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onOpen)
                } else {
                    Modifier
                },
            )
            .then(tag),
    ) {
        Text(
            text = year,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.ink,
        )
    }
}

/** The tag the #302 bench drew and the AVD confirmed: 48,3 × 28 dp. */
private val TAG_WIDTH = 48.3.dp
private val TAG_HEIGHT = 28.dp
