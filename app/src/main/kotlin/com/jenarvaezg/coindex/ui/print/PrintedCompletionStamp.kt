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
 * The rubber stamp of a complete plate on the printed notebook (#371): the ratio over «COMPLETA»,
 * double rule and tilt, on each complete plate's heading. The screen's equivalent is
 * [com.jenarvaezg.coindex.ui.components.StampedRatio]; the page keeps «Progreso» in its
 * specification as well (ADR 0026 §5). The slim band of a shared folio (#232) gets a smaller frame.
 *
 * Composed at [com.jenarvaezg.coindex.ui.screens.printDensity], where one dp is one millimetre.
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
        // Room for the rotated frame, which is larger than the unrotated one (#476).
        modifier = modifier.size(stampFootprint(metrics.frame)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                // Rotation only, no `BlendMode.Multiply` as on screen: Skia writes a blended
                // offscreen layer into the PDF as Type 3 glyphs with a broken bounding box, so the
                // text vanished, and the layer clipped the frame's corners (#476).
                .graphicsLayer { rotationZ = STAMP_TILT }
                .size(metrics.frame)
                .border(metrics.outer, Paper.rust.copy(alpha = 0.82f), RoundedCornerShape(0.3f.mm))
                .padding(metrics.gap)
                .border(metrics.inner, Paper.rust.copy(alpha = 0.72f), RoundedCornerShape(0.3f.mm))
                .semantics { contentDescription = COMPLETE_STAMP_WORD },
        ) {
            // No fixed-height boxes around the lines: boxes shorter than the line height made
            // Compose draw no text at all, while the semantics still said «COMPLETA» (#476).
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
 * The space the stamp takes in a heading: its rotated footprint, not its frame (#476). The slim
 * band gets a half-size frame so the stamp fits in 14 mm.
 */
fun printedStampSize(heading: PrintHeading): DpSize =
    stampFootprint(printedStampMetrics(heading).frame)

/** The box a [frame] turned by [STAMP_TILT] occupies, computed so any frame or tilt fits. */
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
