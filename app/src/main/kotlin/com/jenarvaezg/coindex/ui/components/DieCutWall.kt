package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Cardboard the die-cut leaves between the edge of the hole and the photograph.
 *
 * It is also the ceiling for the wall's width: painted inwards from the edge, a wider wall would
 * land on the photograph, and the photograph already brings its own light baked in (#303).
 */
const val HOLE_CARD_PADDING_DP = 5f

/**
 * The wall of the die-cut, as one continuous sweep around the hole.
 *
 * It used to be two strokes of `sweepAngle = 180f` facing each other, and a 180° stroke ends at
 * once: #357 measured 29 of the 255 luminance levels inside 2° of arc at 3 o'clock, which reads as
 * a seam between two semicircles instead of as a hole. A sweep that fades to nothing at both
 * horizontals has no termination to measure, so the seam goes by construction and not by tuning a
 * value.
 *
 * Fractions run clockwise from 3 o'clock: 0.25 is the bottom, where the freshly cut edge catches
 * the light and is paler than the cardboard, and 0.75 is the top, where the wall is in its own
 * shadow. Each half fades out on its own tint — one stop of transparent white just before 9
 * o'clock and one of transparent ink just after — so neither half ever borrows the other's colour
 * on the way to nothing.
 */
internal fun dieCutWallStops(tone: AlbumToneConfig): Array<Pair<Float, Color>> = arrayOf(
    0f to TRANSPARENT_SHEEN,
    0.25f to Color.White.copy(alpha = tone.dieWallSheenAlpha),
    0.49f to TRANSPARENT_SHEEN,
    0.51f to TRANSPARENT_SHADOW,
    0.75f to Paper.ink.copy(alpha = tone.dieWallShadowAlpha),
    0.99f to TRANSPARENT_SHADOW,
    1f to TRANSPARENT_SHEEN,
)

private val TRANSPARENT_SHEEN = Color.White.copy(alpha = 0f)
private val TRANSPARENT_SHADOW = Paper.ink.copy(alpha = 0f)
