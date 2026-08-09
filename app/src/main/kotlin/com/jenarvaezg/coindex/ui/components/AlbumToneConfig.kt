package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The calibrated tones of the album's paper, plus the geometry of its die-cut.
 *
 * #349 could only calibrate colour and alpha, and that is how it raised the contrast of a drawing
 * nobody had measured. #357 grew the seam with what the die is actually made of: how wide its wall
 * is and how much light each half of it carries.
 */
data class AlbumToneConfig(
    val cartoucheAlpha: Float = 0.90f,
    val cardAlpha: Float = Paper.card.alpha,
    /** The 1 dp rule that separates cardboard from paper — not the wall of the die-cut. */
    val hairlineColor: Color = Paper.hairline,
    val cartoucheTopRuleAlpha: Float = 0.34f,
    val dieWallWidthDp: Float = HOLE_CARD_PADDING_DP,
    val dieWallShadowAlpha: Float = 0.22f,
    /**
     * Higher than its shadow because the cardboard is already at 243 of 255: white can only take
     * the cut edge 12 levels up, while the ink has 196 to spend going down. Measured in the AVD.
     */
    val dieWallSheenAlpha: Float = 0.85f,
) {
    companion object {
        val Default = AlbumToneConfig()
    }
}
