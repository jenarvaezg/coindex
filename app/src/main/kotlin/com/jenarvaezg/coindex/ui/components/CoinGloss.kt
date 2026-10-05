package com.jenarvaezg.coindex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * The gloss of a coin, light and shadow moved by the tilt of the phone (variant H of #303): a
 * linear gradient at 105°, black → transparent → white → transparent → black, over the photograph
 * in `BlendMode.Softlight` (API 29, the `minSdk`; no `RuntimeShader`).
 *
 * Every length is a fraction of the diameter, never dp, so the band behaves the same in holes of
 * any size.
 */
@Immutable
data class CoinGloss(
    /**
     * Moderate because the photograph already has light baked in from the upper left; more white
     * gives haze rather than metal.
     */
    val intensity: Float = 0.5f,
    /** How far the band travels from the centre, each way, as a fraction of the diameter. */
    val travel: Float = 0.45f,
    /** Half the width of the band, as a fraction of the diameter — the gradient's own reach. */
    val halfBand: Float = 0.32f,
    val angleDegrees: Float = 105f,
) {
    /** Where the band's white sits right now, in pixels from the centre of the coin. */
    fun bandCentre(diameterPx: Float, lateral: Float): Float =
        lateral.coerceIn(-1f, 1f) * travel * diameterPx

    fun bandHalfWidth(diameterPx: Float): Float = halfBand * diameterPx

    companion object {
        val Default = CoinGloss()
    }
}

/**
 * How the coins of this tree gloss, or null where they must not: what moves doesn't go to paper
 * (ADR 0026 §4). [com.jenarvaezg.coindex.ui.screens.OffScreenSheet] provides null.
 */
val LocalCoinGloss = staticCompositionLocalOf<CoinGloss?> { CoinGloss.Default }

/**
 * Marks a photograph as metal: the gloss goes over it and the accelerometer moves it. Every coin
 * photograph glosses, die-cut or loose (ADR 0026 §4). Pass [isCoin] false for empty cardboard or
 * the design of a missing issue. Being composed also keeps the sensor registered.
 */
@Composable
fun Modifier.coinGloss(isCoin: Boolean = true): Modifier {
    val gloss = LocalCoinGloss.current?.takeIf { isCoin } ?: return this
    val tilt = LocalCoinTilt.current
    DisposableEffect(tilt) {
        tilt.coinAppeared()
        onDispose { tilt.coinLeft() }
    }
    return coinGloss(gloss, tilt)
}

/**
 * The drawing alone, for the bench, which supplies both the parameters and the tilt. No layer of
 * its own: it blends against the photograph, and the hole's clip keeps it off the cardboard.
 */
fun Modifier.coinGloss(gloss: CoinGloss, tilt: CoinTilt): Modifier = drawWithContent {
    drawContent()
    if (gloss.intensity <= 0f) return@drawWithContent
    val diameter = size.minDimension
    val centre = gloss.bandCentre(diameter, tilt.lateral)
    val half = gloss.bandHalfWidth(diameter)
    val shadow = Color.Black.copy(alpha = gloss.intensity)
    // The hole's square turned on its centre still contains the inscribed coin.
    rotate(gloss.angleDegrees) {
        drawRect(
            brush = Brush.horizontalGradient(
                0f to shadow,
                0.24f to Color.Transparent,
                0.5f to Color.White.copy(alpha = gloss.intensity),
                0.76f to Color.Transparent,
                1f to shadow,
                startX = center.x + centre - half,
                endX = center.x + centre + half,
            ),
            blendMode = BlendMode.Softlight,
        )
    }
}
