package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Cardboard the die-cut leaves between the edge of the hole and the photograph.
 *
 * It is also the ceiling for the wall's width: painted inwards from the edge, a wider wall would
 * land on the photograph, and the photograph already brings its own light baked in (#303).
 */
internal const val HOLE_CARD_PADDING_DP = 5f

/**
 * Hole size the die-cut was measured against (Collections / Monedas cards, #357).
 *
 * Axis holes are 34 dp; keeping a flat 5 dp ring there eats a third of the metal. Padding and wall
 * scale with the hole so the cardboard/coin ratio stays the one entrada-default already shows.
 */
internal const val DESIGN_HOLE_DP = 104f

/** Cardboard ring width for a hole of [holeDp], never above the measured 5 dp. */
internal fun holeCardPaddingDp(holeDp: Float): Float {
    val proportional = HOLE_CARD_PADDING_DP * (holeDp / DESIGN_HOLE_DP)
    // Pure proportion on 34 dp is ~1.6 dp and vanishes on dark metal; 2.5 dp read as «mucho borde»
    // on the country axis. 2 dp keeps the hairline company without eating the photograph.
    return proportional.coerceIn(2f, HOLE_CARD_PADDING_DP)
}

/**
 * The wall of the die-cut, drawn as one continuous sweep around the hole.
 *
 * It used to be two strokes of `sweepAngle = 180f` facing each other, and a 180° stroke ends at
 * once: #357 measured 76 of the 255 luminance levels inside 2° of arc at 3 o'clock, which reads as a
 * seam between two semicircles instead of as a hole. A sweep that fades to nothing at both
 * horizontals has no termination to measure, so the seam goes by construction and not by tuning a
 * value.
 */
data class DieCutWall(
    /** Flush with the cardboard the padding leaves free, so the wall never reaches the photograph. */
    val widthDp: Float = HOLE_CARD_PADDING_DP,
    val shadowAlpha: Float = 0.22f,
    /**
     * Higher than the shadow because the cardboard is already at 243 of 255: white can only take the
     * cut edge 12 levels up, while the ink has 196 to spend going down. Measured in the AVD.
     */
    val sheenAlpha: Float = 0.85f,
) {
    /**
     * The sweep's colour stops, clockwise from 3 o'clock.
     *
     * 0.25 is the bottom, where the freshly cut edge catches the light and is paler than the
     * cardboard, and 0.75 is the top, where the wall is in its own shadow. Each half fades out on
     * its own tint — one stop of transparent white just before 9 o'clock and one of transparent ink
     * just after — so neither half ever borrows the other's colour on the way to nothing.
     *
     * What the shape guarantees is that the profile handed to the brush is continuous; what it
     * measures on a screen is in `docs/ux/implementacion-357/`.
     */
    fun stops(): Array<Pair<Float, Color>> = arrayOf(
        0f to TRANSPARENT_SHEEN,
        0.25f to Color.White.copy(alpha = sheenAlpha),
        0.49f to TRANSPARENT_SHEEN,
        0.51f to TRANSPARENT_SHADOW,
        0.75f to Paper.ink.copy(alpha = shadowAlpha),
        0.99f to TRANSPARENT_SHADOW,
        1f to TRANSPARENT_SHEEN,
    )

    private companion object {
        val TRANSPARENT_SHEEN = Color.White.copy(alpha = 0f)
        val TRANSPARENT_SHADOW = Paper.ink.copy(alpha = 0f)
    }
}
