package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.PHOTO_NOT_DOWNLOADED
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The mark of a photograph that is not on this phone: an arrow onto a shelf, in muted ink (#510).
 * Drawn rather than written like [FaceNotDownloaded], because it can fill every hole of a grid; the
 * sentence is its `contentDescription`. Still, so it also reaches exported sheets (ADR 0026 §4, as
 * read by ADR 0029 §7). Sized as fractions of the diameter, to work from 34 dp to 104 dp.
 */
@Composable
fun PhotoNotDownloaded(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.semantics { contentDescription = PHOTO_NOT_DOWNLOADED }) {
        val diameter = size.minDimension
        val width = (STROKE * diameter).coerceAtLeast(1.dp.toPx())
        val stem = STEM * diameter
        val head = HEAD * diameter
        val shelf = SHELF * diameter
        val tip = Offset(center.x, center.y + stem / 2f)
        stroke(Offset(center.x, center.y - stem / 2f), tip, width)
        stroke(Offset(tip.x - head, tip.y - head), tip, width)
        stroke(Offset(tip.x + head, tip.y - head), tip, width)
        stroke(
            Offset(center.x - shelf, tip.y + shelf),
            Offset(center.x + shelf, tip.y + shelf),
            width,
        )
    }
}

/** One stroke of the mark; all share ink, weight and round cap. */
private fun DrawScope.stroke(from: Offset, to: Offset, width: Float) =
    drawLine(color = Paper.muted, start = from, end = to, strokeWidth = width, cap = StrokeCap.Round)

/** The line weight of the mark, as a fraction of the hole's diameter. */
private const val STROKE = 0.030f

/** How tall the arrow's stem is, as a fraction of the diameter. */
private const val STEM = 0.20f

/** How far each barb of the head reaches, and how far below the tip it starts. */
private const val HEAD = 0.075f

/** Half the width of the shelf the arrow points at, which is also its distance below the tip. */
private const val SHELF = 0.13f
