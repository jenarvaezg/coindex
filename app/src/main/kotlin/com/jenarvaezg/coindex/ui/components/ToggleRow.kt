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
 * One switch of a configuration: its name, why it is greyed, and its state. The whole line is one
 * `toggleable` node, so it is easy to hit and screen readers announce name, note and state
 * together. [note] tells the collector why a disabled row can't be changed.
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
            // A full touch target, as the Material switch used to provide.
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
 * The row's mark: a ruled square with a hand-drawn tick (#512), in place of a Material `Switch`.
 * Private while this row is its only caller; move it to `FieldGuide` if another needs it. Disabled,
 * it stays readable as ticked or empty.
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
        // Inset by half the stroke so the rule lands inside the square.
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
