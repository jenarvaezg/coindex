package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Cardboard between the edge of the hole and the photograph, and the ceiling for the wall's width:
 * a wider wall would land on the photograph (#303).
 */
internal const val HOLE_CARD_PADDING_DP = 5f

/**
 * Hole size the die-cut was designed at (the 104 dp cards, #357). Padding and wall scale with the
 * hole from here, so small axis holes keep the same cardboard/coin ratio.
 */
internal const val DESIGN_HOLE_DP = 104f

/** Cardboard ring width for a hole of [holeDp], never above [HOLE_CARD_PADDING_DP]. */
internal fun holeCardPaddingDp(holeDp: Float): Float {
    val proportional = HOLE_CARD_PADDING_DP * (holeDp / DESIGN_HOLE_DP)
    // At 34 dp pure proportion (~1.6 dp) vanishes on dark metal, and 2.5 dp read as too much
    // border.
    return proportional.coerceIn(2f, HOLE_CARD_PADDING_DP)
}

/**
 * The wall of the die-cut, drawn as one continuous sweep around the hole. Two 180° strokes left a
 * visible seam where they ended (#357); a sweep fading to nothing at both horizontals has no end.
 */
data class DieCutWall(
    /**
     * Flush with the cardboard the padding leaves free, so the wall never reaches the photograph.
     */
    val widthDp: Float = HOLE_CARD_PADDING_DP,
    val shadowAlpha: Float = 0.22f,
    /**
     * Higher than the shadow because the cardboard is already at 243 of 255 luminance: white can
     * only lift it 12 levels, while ink has 196 to go down.
     */
    val sheenAlpha: Float = 0.85f,
    /**
     * The two alphas while the coin rests on its other face (#509), stronger than at rest so the
     * change reads against a photograph that has just changed. Only the shadow is visible on screen
     * (see [sheenAlpha]'s ceiling); 0.42 shows it without reading as dirt. The sheen stays so the
     * sweep closes.
     */
    val turnedShadowAlpha: Float = 0.42f,
    val turnedSheenAlpha: Float = 1f,
) {
    /**
     * The sweep's colour stops, clockwise from 3 o'clock: 0.25 is the bottom, the lit fresh edge;
     * 0.75 the top, in shadow. Each half fades out on its own tint so neither borrows the other's
     * colour. Measurements in `docs/ux/implementacion-357/`.
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

    /**
     * The same sweep lit from the other side, for a hole whose coin shows its other face (#509).
     * The profiles are cross-faded, not rotated, so the cardboard stays still as ADR 0026 §3
     * requires.
     */
    fun turnedStops(): Array<Pair<Float, Color>> = arrayOf(
        0f to TRANSPARENT_SHADOW,
        0.25f to Paper.ink.copy(alpha = turnedShadowAlpha),
        0.49f to TRANSPARENT_SHADOW,
        0.51f to TRANSPARENT_SHEEN,
        0.75f to Color.White.copy(alpha = turnedSheenAlpha),
        0.99f to TRANSPARENT_SHEEN,
        1f to TRANSPARENT_SHADOW,
    )

    private companion object {
        val TRANSPARENT_SHEEN = Color.White.copy(alpha = 0f)
        val TRANSPARENT_SHADOW = Paper.ink.copy(alpha = 0f)
    }
}
