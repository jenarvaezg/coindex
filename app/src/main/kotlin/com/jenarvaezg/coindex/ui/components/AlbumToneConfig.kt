package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper

/** The four calibrated tones shared by album cartouches and die-cut holes. */
data class AlbumToneConfig(
    val cartoucheAlpha: Float = 0.90f,
    val cardAlpha: Float = Paper.card.alpha,
    val hairlineColor: Color = Paper.hairline,
    val cartoucheTopRuleAlpha: Float = 0.34f,
) {
    companion object {
        val Default = AlbumToneConfig()
    }
}
