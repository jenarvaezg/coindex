package com.jenarvaezg.coindex.ui.print

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.ui.COMPLETE_STAMP_WORD
import com.jenarvaezg.coindex.ui.theme.BarlowCondensedFamily
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The rubber stamp of a complete plate, drawn for the printed notebook in millimetres of paper.
 *
 * It is **not** [com.jenarvaezg.coindex.ui.components.StampedRatio]: that composable is the screen
 * and the PNG, sized in dp, and it lands on the ratio the header already showed. A page of the
 * cuaderno keeps «Progreso · n / n emisiones» in the specification (ADR 0026 §5), while the
 * caucho celebrates the same already-counted ratio beside «COMPLETA» — double rule and tilt — over
 * each plate's own heading (#371). The screen's blend is **not** part of it: see the layer below.
 *
 * **Per plate, not per folio.** A shared page (#232) can carry two complete plates, and each one
 * stamps its own band; the thin heading gets a smaller frame so the ink still fits the fourteen
 * millimetres the geometry reserved.
 *
 * The notebook is composed at [com.jenarvaezg.coindex.ui.screens.printDensity], where one dp is one
 * millimetre of paper, so the sizes below are millimetres spoken as dp.
 */
@Composable
fun PrintedCompletionStamp(
    heading: PrintHeading,
    ratio: String,
    modifier: Modifier = Modifier,
) {
    val metrics = printedStampMetrics(heading)

    Box(
        contentAlignment = Alignment.Center,
        // The air the turn needs, reserved by the caller's own box: a rotated rectangle is wider and
        // taller than the one it was drawn as, and on paper there is nothing under the ink to bleed
        // into (#476).
        modifier = modifier.size(stampFootprint(metrics.frame)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                // **The turn and nothing else.** This used to carry `BlendMode.Multiply` on an
                // offscreen layer, the way the screen stamp does, and on paper it cost the caucho its
                // contents: Skia writes a blended layer into the PDF as Type 3 glyphs with a broken
                // bounding box, so every viewer printed an empty double frame — the ratio and
                // «COMPLETA» reached no page at all. The offscreen layer clipped the rotation too,
                // cutting the corners off the frame it did draw. The ink is rust over cream either
                // way; what multiply added was invisible next to what it took (#476).
                .graphicsLayer { rotationZ = STAMP_TILT }
                .size(metrics.frame)
                .border(metrics.outer, Paper.rust.copy(alpha = 0.82f), RoundedCornerShape(0.3f.mm))
                .padding(metrics.gap)
                .border(metrics.inner, Paper.rust.copy(alpha = 0.72f), RoundedCornerShape(0.3f.mm))
                .semantics { contentDescription = COMPLETE_STAMP_WORD },
        ) {
            // **Two lines stacked, each as tall as its own type.** They used to be wrapped in boxes of
            // a fixed height — 5,2 mm for the ratio and 3,8 for the word — and both numbers are *below*
            // the line those sizes actually need (6,7 and 5,0 at print density). Compose measured a
            // paragraph that could not fit a single line and drew **none**: the caucho reached paper as
            // an empty frame, while the semantics still said «COMPLETA» and two tests believed it
            // (#476). Nothing has to be fixed for the baselines to stay apart — a column that spaces
            // its children cannot overlap them, which is what the boxes were for — and thirteen
            // millimetres of type still leave six inside the rule.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(metrics.lineGap),
            ) {
                Text(
                    text = printedCompletionRatio(ratio),
                    style = stampLine(
                        size = metrics.ratioSize,
                        tracking = metrics.ratioLetterSpacing,
                        alpha = 0.88f,
                    ),
                    modifier = Modifier.width(metrics.contentWidth),
                )
                Text(
                    text = COMPLETE_STAMP_WORD.uppercase(),
                    style = stampLine(
                        size = metrics.wordSize,
                        tracking = metrics.letterSpacing,
                        alpha = 0.82f,
                    ),
                    modifier = Modifier.width(metrics.contentWidth),
                )
            }
        }
    }
}

/** One line of the caucho: the condensed face, the size asked for and the ink's own weight. */
private fun stampLine(size: TextUnit, tracking: TextUnit, alpha: Float): TextStyle = TextStyle(
    fontFamily = BarlowCondensedFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = size,
    letterSpacing = tracking,
    textAlign = TextAlign.Center,
    color = Paper.rust.copy(alpha = alpha),
)

/** The compact screen figure given a little typographic air for the printed celebration. */
internal fun printedCompletionRatio(ratio: String): String =
    ratio.split('/').joinToString(" / ") { part -> part.trim() }

/**
 * How much of the band the caucho takes: the frame it is drawn as, plus the air its turn needs.
 *
 * It is the **footprint** and not the frame, because what the heading has to hold is what lands on
 * the paper: a 24 × 22 rectangle turned five and a half degrees measures 26,0 × 24,2, and the corners
 * that stick out are exactly what the old offscreen layer was cutting off (#476).
 *
 * The masthead and the plain band take the measured frame; the slim band of a shared folio gets half
 * of it so the stamp still lands inside fourteen millimetres.
 */
fun printedStampSize(heading: PrintHeading): DpSize =
    stampFootprint(printedStampMetrics(heading).frame)

/**
 * The box a [frame] turned by [STAMP_TILT] actually occupies.
 *
 * Trigonometry rather than a measured constant, so the two frames — and any third one — reserve what
 * their own turn costs: a tilt somebody nudges by a degree cannot go back to clipping the ink.
 */
internal fun stampFootprint(frame: DpSize): DpSize {
    val radians = STAMP_TILT * PI.toFloat() / 180f
    val sin = abs(sin(radians))
    val cos = abs(cos(radians))
    return DpSize(
        width = Dp(frame.width.value * cos + frame.height.value * sin),
        height = Dp(frame.width.value * sin + frame.height.value * cos),
    )
}

private fun printedStampMetrics(heading: PrintHeading): StampMetrics = when (heading) {
    PrintHeading.Slim -> StampMetrics(
        frame = DpSize(12.dp, 11.dp),
        wordSize = 1.6f.sp,
        letterSpacing = 0.25f.sp,
        ratioSize = 2.2f.sp,
        contentWidth = 8f.mm,
        lineGap = 1.2f.mm,
        ratioLetterSpacing = 0.1f.sp,
        outer = 0.35f.mm,
        gap = 0.55f.mm,
        inner = 0.2f.mm,
    )
    PrintHeading.Masthead, PrintHeading.Plain -> StampMetrics(
        frame = DpSize(24.dp, 22.dp),
        wordSize = 2.9f.sp,
        letterSpacing = 0.45f.sp,
        ratioSize = 4.2f.sp,
        contentWidth = 18f.mm,
        lineGap = 2.2f.mm,
        ratioLetterSpacing = 0.2f.sp,
        outer = 0.55f.mm,
        gap = 1.15f.mm,
        inner = 0.3f.mm,
    )
}

/** One drawing of the caucho: frame, word and the two rules, sized for one kind of heading. */
private data class StampMetrics(
    val frame: DpSize,
    val wordSize: TextUnit,
    val letterSpacing: TextUnit,
    val ratioSize: TextUnit,
    val contentWidth: Dp,
    val lineGap: Dp,
    val ratioLetterSpacing: TextUnit,
    val outer: Dp,
    val gap: Dp,
    val inner: Dp,
)

/** Same tilt the screen stamp was measured with (#304). */
private const val STAMP_TILT = 5.5f

/** One dp is one millimetre under [com.jenarvaezg.coindex.ui.screens.printDensity]. */
private val Float.mm: Dp get() = Dp(this)
