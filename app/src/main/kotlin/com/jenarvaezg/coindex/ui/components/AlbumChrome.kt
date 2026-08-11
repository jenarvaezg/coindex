package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.APP_NAME
import com.jenarvaezg.coindex.ui.SETTINGS_LABEL
import com.jenarvaezg.coindex.ui.SewnEdgeCounts
import com.jenarvaezg.coindex.ui.sewnEdgeLabel
import com.jenarvaezg.coindex.ui.theme.Paper

/** The sewn album edge: three counts and the way into Ajustes. */
@Composable
fun AlbumChrome(
    counts: SewnEdgeCounts?,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Paper.ink)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(APP_NAME, style = MaterialTheme.typography.titleMedium, color = Paper.paper)
        Text(
            sewnEdgeLabel(counts),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.paperDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        SettingsGlyph(onClick = onSettings)
    }
}

@Composable
private fun SettingsGlyph(onClick: () -> Unit) {
    Canvas(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = SETTINGS_LABEL }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(13.dp),
    ) {
        val stroke = 1.5.dp.toPx()
        val xs = listOf(size.width * 0.28f, size.width * 0.66f, size.width * 0.43f)
        repeat(3) { row ->
            val y = size.height * (0.22f + row * 0.28f)
            drawLine(Paper.paper, Offset(0f, y), Offset(size.width, y), stroke)
            drawCircle(Paper.ink, radius = 2.8.dp.toPx(), center = Offset(xs[row], y))
            drawCircle(
                Paper.paper,
                radius = 2.8.dp.toPx(),
                center = Offset(xs[row], y),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
            )
        }
    }
}
