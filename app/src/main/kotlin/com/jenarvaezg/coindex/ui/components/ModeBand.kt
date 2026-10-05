package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The band at the foot of a sheet while a mode («Marcar lo que busco», «Hacer una colección»)
 * changes what a tap means (#517): the mode's sentence (ADR 0029 §5, #282) and its actions. A
 * layout row rather than a floating bar, so it never covers content. [sheetUnderMode] and
 * [outsideTheMode] mark the rest of the sheet.
 */
@Composable
fun ModeBand(
    sentence: String,
    modifier: Modifier = Modifier,
    actions: @Composable FlowRowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Paper.paperDeep)
            .drawBehind {
                val rule = 1.dp.toPx()
                drawRect(
                    color = Paper.hairline,
                    size = size.copy(height = rule),
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            sentence,
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.ink,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = actions,
        )
    }
}

/**
 * The page under an open mode, a shade deeper (#517). Drawn behind the content, so no photograph or
 * text loses contrast. Darker rather than lighter because the paper is already at 238 of 255
 * (#509).
 */
fun Modifier.sheetUnderMode(open: Boolean): Modifier =
    if (!open) this else drawBehind { drawRect(Paper.paperDeep.copy(alpha = MODE_WASH_OPACITY)) }

/**
 * Fades what the open mode can't act on, such as a full casilla, so only the targets look pressable
 * (#517). Always pair it with removing the tap.
 */
fun Modifier.outsideTheMode(outside: Boolean): Modifier =
    if (!outside) this else alpha(MODE_ASIDE_OPACITY)

/**
 * About ten luminance levels under the sheet: visible across the grid, too light to read as a scrim
 * or to darken an empty casilla's ghost.
 */
private const val MODE_WASH_OPACITY = 0.55f

/** Faint enough to recede, solid enough that the coin is still recognisable. */
private const val MODE_ASIDE_OPACITY = 0.45f
