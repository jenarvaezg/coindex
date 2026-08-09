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
 * The ink falling on the ratio of a complete sheet, and how long it takes to fall.
 *
 * Null is the export rule of ADR 0026 §4 said in the same place the gloss says it: the stamp is a
 * **state** and travels to the PNG, but the *stamping* is alive and does not, so a sheet composed
 * off screen finds the ink already dry.
 * [com.jenarvaezg.coindex.ui.screens.OffScreenSheet] provides it, and no surface has to remember
 * whether it is being photographed.
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
 * The ink of one **opening of the sheet**, held above whatever draws it.
 *
 * It is hoisted on purpose and it is the whole of what «se estampa al abrir la hoja» costs. The
 * header of a plate is an item of a lazy grid, so it is disposed the moment the collector scrolls
 * past it: a stamp that kept its own progress would fall again on the way back up, which is exactly
 * the ceremony-on-scroll that ADR 0026 §3 refused for the index. Remembered where the sheet lives,
 * it falls once per opening, however far down the collector goes.
 *
 * An [Animatable] and not `animateFloatAsState`, which starts at its own target: the ink would be
 * dry on the frame the sheet opens and the movement would never once be seen. Losing completeness
 * snaps instead of animating — a plate that grew from 19/19 to 19/20 «deja de enseñarlo, sin drama»,
 * and drama is precisely a stamp fading out of a sheet.
 */
@Composable
fun rememberInkFall(complete: Boolean): State<Float> {
    val stamping = LocalStamping.current
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
 * The ratio a plate heads itself with, and the rubber stamp over it while nothing is missing.
 *
 * **One composable and not two**, because the stamp is not an ornament beside the figure: it lands
 * *on* it (ADR 0026 §3), so the two share a frame of 84 × 76 dp and the ceremony adds not one word
 * and not one figure to the sheet — it eats the datum that was already there.
 *
 * The ratio enters pale and the ink fixes it: both are driven by the same progress, so a plate that
 * is complete before it is opened cannot show a fixed figure under an ink that has not fallen.
 *
 * It is drawn at **one size, in dp**, and a sheet that needs it bigger raises the density it is
 * composed at (`PlateSheet`) rather than passing a factor in. Multiplying each dp by hand looks like
 * the same thing and is not: the 1 dp corners and the two rules stop being proportional to the frame,
 * and on an eight-column export the stamp came out with its own outline broken at the corners.
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
            // The multiply of the ink needs something to multiply against, and a layer is what
            // gives it one: without this the blend has no backdrop of its own and the stamp comes
            // out as flat rust over the paper instead of ink soaking into it.
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
            // Pale only while ink is on its way: «el cociente entra pálido y la tinta lo fija».
            // A plate that is missing eight receives no ink, so its figure is not waiting for any
            // and prints at full rust — which is the `4/22` of #304's second capture.
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
 * The rubber stamp itself: 84 × 76 dp of double rule in `multiply`, turned 5.5°.
 *
 * It says one word, «completa», **including for an open series** (ADR 0026 §3): a second word for
 * the open case would be vocabulary that has to be explained, and the case does not exist today —
 * none of the father's six complete plates is an open series.
 *
 * It comes down slightly larger than it lands, which is the whole of the ceremony: a stamp is
 * pressed onto paper, and something that only fades in is a label appearing.
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
            // Read out as one thing with the figure it encloses: «completa · 22/22».
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

/** The measured frame of #304: 84 × 76 dp, which leaves 122 dp of clearance under «Fuertes». */
private val STAMP_WIDTH = 84.dp
private val STAMP_HEIGHT = 76.dp

/** Hand-pressed and never square to the page. */
private const val STAMP_TILT = 5.5f

/** How much bigger the stamp is in the air than on the paper. */
private const val STAMP_OVERSHOOT = 1.16f

/** Where the figure sits inside the frame, so the word above it has its own band. */
private val RATIO_DROP: Dp = 14.dp

private val RATIO_SIZE = 18.sp

/** What the ratio is worth before the ink lands on it. */
private const val RATIO_PALE = 0.45f
