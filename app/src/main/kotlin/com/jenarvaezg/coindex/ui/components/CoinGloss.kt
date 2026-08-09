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
 * The gloss of a coin: light **and** shadow, moved by the tilt of the phone.
 *
 * A linear gradient at 105° — black → transparent → white → transparent → black — laid over the
 * photograph in `BlendMode.Softlight`. It is variant H of #303, and what it wins is not light: it is
 * that the coin stops being a flat cut-out glued into a hole. No `RuntimeShader` is involved, so the
 * effect imposes no version floor: `BlendMode` is API 29, exactly the `minSdk`.
 *
 * **Everything is a fraction of the diameter, never a dp.** #303 prototyped ±55 dp over a hole of
 * 121 dp and wrote down what that was — ±45 % of the diameter. Production's casilla is 104 dp, and
 * 55 dp there would be ±53 %: a band that spends more time off the coin than on it, which reads as a
 * flicker at the extremes of the tilt. The proportion is also what survives the day a hole changes
 * size, and «Las cifras» brings one of its own.
 */
@Immutable
data class CoinGloss(
    /**
     * Half the prototype video's, because the photograph already carries its own light baked in from
     * the upper left: piling white on an already pale picture does not give metal, it gives haze.
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
 * How the coins of this tree gloss, or `null` where they must not.
 *
 * Null is the export rule of ADR 0026 §4 said in one place: what is alive does not travel to paper.
 * [com.jenarvaezg.coindex.ui.screens.OffScreenSheet] provides it for every sheet that composes
 * itself off screen, so no surface has to remember whether it is being photographed.
 */
val LocalCoinGloss = staticCompositionLocalOf<CoinGloss?> { CoinGloss.Default }

/**
 * Marks a photograph as metal: the gloss goes over it and the accelerometer moves it.
 *
 * One modifier and not a list of screens (ADR 0026 §4): **every coin photograph glosses**, die-cut
 * or loose — the plate, the index, `PieceCard` and the side sheets.
 *
 * [isCoin] is where the other half of the rule lives, so that a third surface cannot forget it:
 * empty cardboard never glosses, for the direct reason that there is no coin there, and neither does
 * the desaturated design of an issue the collector is missing — that is the catalog's drawing, not
 * metal.
 *
 * Being composed is also what registers the sensor: while no coin is on screen the accelerometer is
 * not worth its battery, and no surface has to say so out loud.
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
 * The drawing on its own, for the bench, which governs both the parameters and the tilt itself.
 *
 * `drawWithContent` and not a layer of its own: the blend is against what is already under the band
 * — the photograph — and the clip of whoever owns the hole is what keeps it off the cardboard.
 */
fun Modifier.coinGloss(gloss: CoinGloss, tilt: CoinTilt): Modifier = drawWithContent {
    drawContent()
    if (gloss.intensity <= 0f) return@drawWithContent
    val diameter = size.minDimension
    val centre = gloss.bandCentre(diameter, tilt.lateral)
    val half = gloss.bandHalfWidth(diameter)
    val shadow = Color.Black.copy(alpha = gloss.intensity)
    // The rectangle is the square of the hole turned on its own centre, and a circle inscribed in a
    // square stays inscribed however the square is turned: the band covers the whole coin.
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
