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
 *
 * **Drawn and not written**, unlike [FaceNotDownloaded], because this one falls on every hole of a
 * grid at once. A plate off wifi is thirty casillas: thirty times four words is a wall of prose
 * where the collector wanted his album, and the same wall at the 34 dp of the notebook's axes
 * would not even fit. The sentence stays where it was — the far face of a coin the collector has
 * just turned over, which is one hole and an answer to a gesture — and it reaches whoever is not
 * looking as this drawing's own `contentDescription`.
 *
 * **Still**, like everything else the album draws. A pulse here would be the very shimmer the
 * ticket refused: what this state means is precisely that nothing is happening. Being still is
 * also what sends it to paper by ADR 0026 §4, as ADR 0029 §7 reads it — «alive» is what follows
 * the finger, the sensor or the navigation — and that is right: a plate exported with no pictures
 * in it says why it is empty instead of showing eleven mute discs.
 *
 * Everything is a fraction of the diameter and never a dp, the rule [CoinGloss] already keeps: the
 * same mark is read at the 104 dp of a casilla and at the 34 dp of an axis cell.
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

/** One stroke of the mark: they are all the same ink, the same weight and the same round end. */
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
