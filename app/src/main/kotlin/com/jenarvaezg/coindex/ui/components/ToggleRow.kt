package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * One switch of a configuration: what it is called, why it is greyed, and its state.
 *
 * The whole line carries the tap, because a thumb aiming at a 22dp mark on a phone held in one hand
 * is the same measured miss the filter shelf fixed by taking the whole line (`FilterShelf`). It is
 * one `toggleable` node and not a label beside a control, so the reader hears the name, the reason
 * and the state as a single answer.
 *
 * [note] is where a disabled row says why, and it is what keeps grey from reading as broken: a
 * control the collector cannot move owes them the reason on the spot, not in a help screen.
 */
@Composable
fun ToggleRow(
    label: String,
    note: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The 48dp the Material switch used to bring with it, kept now that the row draws its
            // own mark: what a line of this card is worth is a finger, not a line of type.
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Paper.ink else Paper.muted,
            )
            note?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = Paper.muted)
            }
        }
        TickBox(checked = checked, enabled = enabled)
    }
}

/**
 * The mark this row makes: a ruled square, ticked by hand (#512).
 *
 * A Material `Switch` was the last piece of raw Material in the app — a rounded track, a shadowed
 * thumb and a ripple, in a page made of rectangles, hairlines and glyphs drawn with two strokes.
 * What replaces it is the rule of a [FieldCard] bent into a square, with the tick drawn in the hand
 * of [ShareGlyph] and [ForwardGlyph]: nothing imported, nothing rounded. It stays private to this
 * file while this row is its only caller — the guide's shared marks live in `FieldGuide`, and one
 * of them is what this becomes the day a second card asks for a tick.
 *
 * Grey and still readable as ticked or empty, because a disabled row is reporting the configuration
 * as well as refusing to change it: the fill drops to the hairline instead of losing the tick.
 */
@Composable
private fun TickBox(checked: Boolean, enabled: Boolean) {
    val fill = when {
        !checked -> if (enabled) Color.Transparent else Paper.paperDeep
        enabled -> Paper.moss
        else -> Paper.hairline
    }
    val edge = if (enabled) Paper.line else Paper.hairline
    Canvas(modifier = Modifier.size(TICK_BOX)) {
        drawRect(fill)
        // Inset by half the stroke so the rule lands inside the square instead of straddling it,
        // which is what the dashed cards of the guide do with theirs.
        val rule = EDGE_WIDTH.toPx()
        drawRect(
            color = edge,
            topLeft = Offset(rule / 2, rule / 2),
            size = Size(size.width - rule, size.height - rule),
            style = Stroke(width = rule),
        )
        if (checked) {
            val stroke = size.minDimension * 0.13f
            val elbow = Offset(size.width * 0.42f, size.height * 0.72f)
            drawLine(
                Paper.paper,
                Offset(size.width * 0.22f, size.height * 0.50f),
                elbow,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                Paper.paper,
                elbow,
                Offset(size.width * 0.78f, size.height * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val TICK_BOX = 22.dp
private val EDGE_WIDTH = 1.dp
