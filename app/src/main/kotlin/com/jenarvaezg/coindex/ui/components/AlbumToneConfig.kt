package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * What the bench calibrates on the album's paper: four tones, and the wall of the die-cut.
 *
 * #349 could only reach colour and alpha, and that is how it raised the contrast of a drawing nobody
 * had measured. #357 gave the seam the die itself — see [DieCutWall].
 */
data class AlbumToneConfig(
    val cartoucheAlpha: Float = 0.90f,
    val cardAlpha: Float = Paper.card.alpha,
    /** The 1 dp rule that separates cardboard from paper — not the wall of the die-cut. */
    val hairlineColor: Color = Paper.hairline,
    val cartoucheTopRuleAlpha: Float = 0.34f,
    val dieWall: DieCutWall = DieCutWall(),
) {
    companion object {
        val Default = AlbumToneConfig()
    }
}
