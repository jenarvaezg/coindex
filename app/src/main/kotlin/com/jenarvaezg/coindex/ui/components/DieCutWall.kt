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
    /**
     * What the same two alphas become while the coin is resting on its other face (#509).
     *
     * The light does not merely swap sides: it is also turned up, because at rest the wall is
     * competing with nothing and turned it is competing with a photograph that has just changed.
     * The HTML prototype measured the swap alone at 38 % of the ring's pixels moving against the
     * 5.8 % that the same ring moves today, and with these two values at 39.5 %.
     *
     * **On the AVD only half of the swap is visible, and it is the shadow.** The same casilla
     * photographed at rest and turned moves the bottom of its ring down 28 of 255 luminance levels
     * with [turnedShadowAlpha] at 0.42 — and 59 with it at 1, which is how the value was checked to
     * be doing the work — while the top of the ring moves **exactly zero**, whatever
     * [turnedSheenAlpha] says. That is the ceiling [sheenAlpha] already documents: the cardboard is
     * at 243 of 255 and white has nowhere to go. So what a turned casilla actually says is that
     * *the shadow of the cut has changed sides*, and 0.42 is the shadow that says it without
     * reading as dirt on the paper. The sheen stays in the profile because the sweep needs both
     * halves to close, not because a screen can show it.
     */
    val turnedShadowAlpha: Float = 0.42f,
    val turnedSheenAlpha: Float = 1f,
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

    /**
     * The same sweep with the light on the other side, for a hole whose coin is showing its other
     * face (#509).
     *
     * A cut wall is lit by where the light is, so the honest way for a casilla to say it is turned
     * is that its own cardboard is lit from the other side — no ink, no letter, and not a single dp
     * of the cell. The two profiles are cross-faded rather than rotated: rotating the sweep would
     * turn the cardboard, and the cardboard is precisely what ADR 0026 §3 keeps still while the
     * coin comes round.
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
