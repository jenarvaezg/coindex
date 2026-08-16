package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.ui.PHOTO_NOT_DOWNLOADED
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Why a hole is not showing a coin, when it is not showing one (#510).
 *
 * Three things can be behind an empty hole and the album used to draw two of them the same: the
 * stand-in disc stood both for «the photograph is on its way» and for «the photograph is not on
 * this phone and nothing is bringing it right now». On a phone off wifi the second is the normal
 * state of a whole plate — ADR 0024 only prefetches on an unmetered network — and it read as a
 * grid of broken images, which is what the audit of 14 August 2026 wrote down.
 *
 * The **dashed ring** of a casilla the collector is missing is not one of these: that is a fact
 * about the collection and it is drawn over whatever the photograph turned out to be.
 */
enum class HoleSilence {
    /** Asked for and not answered yet. Seconds, and it ends in a coin or in [NotOnThisPhone]. */
    Loading,

    /**
     * Asked for and answered without a picture: no network, or a URL that is not coming back.
     *
     * Always temporary and always the collector's to fix — walk into a wifi — which is why it is
     * the one of the three that says something out loud.
     */
    NotOnThisPhone,

    /** Nothing to ask for: the catalogue holds no candidate for this face. */
    NoPhotograph,
}

/**
 * Which silence this is, out of what the hole already knows.
 *
 * [settled] is the load having reported back — Coil calls it once per set of candidates, after the
 * last fallback has failed — and it is the whole of the difference between waiting and loading.
 * Nothing here consults the network, the prefetch or its refusal: a photograph that did not arrive
 * did not arrive, whatever the reason, and the reason has a screen of its own in Ajustes.
 *
 * @param candidates how many URLs this face offers, which is zero for a face with no picture.
 * @return null when the coin is on the hole and there is no silence to explain.
 */
fun holeSilence(candidates: Int, settled: Boolean, painted: Boolean): HoleSilence? = when {
    painted -> null
    candidates == 0 -> HoleSilence.NoPhotograph
    settled -> HoleSilence.NotOnThisPhone
    else -> HoleSilence.Loading
}

/**
 * Whether the holes of this tree may say that a photograph has not arrived.
 *
 * False on paper, which is the export rule of ADR 0026 §4 read for a notice rather than for a
 * movement: a PNG the father sends to somebody else cannot carry an arrow asking **his** phone to
 * find a wifi. What the exported sheet keeps is the stand-in disc it has always had, because the
 * hole is empty either way and the sheet is a picture of the album and not of the download.
 */
val LocalPhotoNotices = staticCompositionLocalOf { true }

/**
 * The mark of a photograph that is not on this phone: an arrow onto a shelf, in muted ink.
 *
 * **Drawn and not written**, unlike [FaceNotDownloaded], because this one falls on every hole of a
 * grid at once. A plate off wifi is thirty casillas: thirty times four words is a wall of prose
 * where the collector wanted his album, and the same wall at the 34 dp of the notebook's axes
 * would not even fit. The sentence stays where it was — the far face of a coin the collector has
 * just turned over, which is one hole and an answer to a gesture.
 *
 * **Still**, like everything else the album draws. A pulse here would be the very shimmer the
 * ticket refused: what this state means is precisely that nothing is happening.
 *
 * Everything is a fraction of the diameter and never a dp, the rule [CoinGloss] already keeps: the
 * same mark is read at the 104 dp of a casilla and at the 34 dp of an axis cell.
 */
@Composable
fun PhotoNotDownloaded(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.semantics { contentDescription = PHOTO_NOT_DOWNLOADED }) {
        val diameter = size.minDimension
        val stroke = (STROKE * diameter).coerceAtLeast(1.dp.toPx())
        val stem = STEM * diameter
        val head = HEAD * diameter
        val shelf = SHELF * diameter
        val tip = center.y + stem / 2f
        val line = Stroke(width = stroke, cap = StrokeCap.Round)
        drawLine(
            color = Paper.muted,
            start = Offset(center.x, center.y - stem / 2f),
            end = Offset(center.x, tip),
            strokeWidth = line.width,
            cap = line.cap,
        )
        drawLine(
            color = Paper.muted,
            start = Offset(center.x - head, tip - head),
            end = Offset(center.x, tip),
            strokeWidth = line.width,
            cap = line.cap,
        )
        drawLine(
            color = Paper.muted,
            start = Offset(center.x + head, tip - head),
            end = Offset(center.x, tip),
            strokeWidth = line.width,
            cap = line.cap,
        )
        drawLine(
            color = Paper.muted,
            start = Offset(center.x - shelf, tip + shelf),
            end = Offset(center.x + shelf, tip + shelf),
            strokeWidth = line.width,
            cap = line.cap,
        )
    }
}

/** The line weight of the mark, as a fraction of the hole's diameter. */
private const val STROKE = 0.030f

/** How tall the arrow's stem is, as a fraction of the diameter. */
private const val STEM = 0.20f

/** How far each barb of the head reaches, and how far below the tip it starts. */
private const val HEAD = 0.075f

/** Half the width of the shelf the arrow points at, which is also its distance below the tip. */
private const val SHELF = 0.13f
