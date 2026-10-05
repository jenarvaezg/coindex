package com.jenarvaezg.coindex.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.jenarvaezg.coindex.ui.COMPLETE_STAMP_WORD
import com.jenarvaezg.coindex.ui.theme.BarlowCondensedFamily
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The ink falling on the ratio of a complete sheet. Null means no animation (ADR 0026 §4): the
 * stamp is a state and reaches the PNG, the stamping is not, so
 * [com.jenarvaezg.coindex.ui.screens.OffScreenSheet] provides null and finds the ink dry.
 */
@Immutable
data class Stamping(
    /** Approved at 300 ms on an HTML prototype (#304) and confirmed on the bench (ADR 0026 §15). */
    val durationMillis: Int = 300,
) {
    companion object {
        val Default = Stamping()
    }
}

val LocalStamping = staticCompositionLocalOf<Stamping?> { Stamping.Default }

/**
 * The ink of one opening of the sheet, hoisted to where the sheet lives: the plate header is a lazy
 * grid item, and a stamp holding its own progress would fall again after scrolling back up
 * (ADR 0026 §3). An [Animatable], because `animateFloatAsState` starts at its target and the fall
 * would never show. Losing completeness snaps rather than fading the stamp out.
 */
@Composable
fun rememberInkFall(complete: Boolean): State<Float> {
    // With system animations off the ink is dry, as on an export (#514).
    val stamping = LocalStamping.current.takeIf { LocalMotion.current }
    val landed = if (complete) 1f else 0f
    val ink = remember(stamping) { Animatable(if (stamping == null) landed else 0f) }
    LaunchedEffect(stamping, landed) {
        when {
            stamping == null || landed == 0f -> ink.snapTo(landed)
            else -> ink.animateTo(landed, tween(stamping.durationMillis))
        }
    }
    return ink.asState()
}

/**
 * The ratio a plate heads itself with, and the rubber stamp landing on it while nothing is missing
 * (ADR 0026 §3); they share one frame. The ratio starts pale and the ink fixes it, both driven by
 * [fall].
 *
 * Drawn at one size in dp. To make it bigger, compose at a higher density (as the shared PNG does)
 * rather than scaling dp values, which broke the corners of the double rule.
 */
@Composable
fun StampedRatio(
    ratio: String,
    complete: Boolean,
    /** How far the ink has fallen, from [rememberInkFall] — held by whoever owns the sheet. */
    fall: State<Float>,
    modifier: Modifier = Modifier,
) {
    val ink = fall.value

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // Gives the stamp's multiply blend a backdrop; without it the ink is flat rust.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .size(width = STAMP_WIDTH, height = STAMP_HEIGHT),
    ) {
        Text(
            text = ratio,
            modifier = Modifier.padding(top = RATIO_DROP),
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = RATIO_SIZE,
            textAlign = TextAlign.Center,
            // Pale only while ink is on its way; an incomplete plate's ratio is full rust (#304).
            color = Paper.rust.copy(
                alpha = if (complete) RATIO_PALE + (1f - RATIO_PALE) * ink else 1f,
            ),
        )
        if (ink > 0f) {
            CompletionStamp(ink = ink)
        }
    }
}

/**
 * The rubber stamp itself: a double rule in `multiply`, turned [STAMP_TILT]. It says «completa»
 * even for an open series (ADR 0026 §3). It comes down slightly larger than it lands, so it reads
 * as pressed rather than faded in.
 */
@Composable
private fun CompletionStamp(ink: Float) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(width = STAMP_WIDTH, height = STAMP_HEIGHT)
            .graphicsLayer {
                val press = STAMP_OVERSHOOT - ink * (STAMP_OVERSHOOT - 1f)
                scaleX = press
                scaleY = press
                rotationZ = STAMP_TILT
                alpha = ink
                blendMode = BlendMode.Multiply
            }
            .border(2.dp, Paper.rust.copy(alpha = 0.82f), RoundedCornerShape(1.dp))
            .padding(4.dp)
            .border(1.dp, Paper.rust.copy(alpha = 0.72f), RoundedCornerShape(1.dp))
            // Read out together with the figure it encloses: «completa · 22/22».
            .semantics { contentDescription = COMPLETE_STAMP_WORD },
    ) {
        Text(
            text = COMPLETE_STAMP_WORD.uppercase(),
            modifier = Modifier.padding(top = 7.dp),
            fontFamily = BarlowCondensedFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = Paper.rust.copy(alpha = 0.82f),
        )
    }
}

/** The stamp's frame (#304). */
private val STAMP_WIDTH = 84.dp
private val STAMP_HEIGHT = 76.dp

/** Hand-pressed and never square to the page. */
private const val STAMP_TILT = 5.5f

/** How much bigger the stamp is in the air than on the paper. */
private const val STAMP_OVERSHOOT = 1.16f

/** Where the figure sits inside the frame, so the word above it has its own band. */
private val RATIO_DROP: Dp = 14.dp

private val RATIO_SIZE = 18.sp

/** The ratio's alpha before the ink lands on it. */
private const val RATIO_PALE = 0.45f
