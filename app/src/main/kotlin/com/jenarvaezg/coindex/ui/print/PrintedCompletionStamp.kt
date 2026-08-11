package com.jenarvaezg.coindex.ui.print

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.ui.COMPLETE_STAMP_WORD
import com.jenarvaezg.coindex.ui.theme.BarlowCondensedFamily
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The rubber stamp of a complete plate, drawn for the printed notebook in millimetres of paper.
 *
 * It is **not** [com.jenarvaezg.coindex.ui.components.StampedRatio]: that composable is the screen
 * and the PNG, sized in dp, and it lands on the ratio the header already showed. A page of the
 * cuaderno keeps «Progreso · n / n emisiones» in the specification (ADR 0026 §5), while the
 * caucho celebrates the same already-counted ratio beside «COMPLETA» — double rule, tilt,
 * `multiply` — over each plate's own heading (#371).
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
        modifier = modifier
            // Multiply needs a backdrop of its own, exactly as the screen stamp does: without an
            // offscreen layer the blend has nothing to multiply against and the ink prints flat.
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                rotationZ = STAMP_TILT
                blendMode = BlendMode.Multiply
            }
            .size(metrics.frame)
            .border(metrics.outer, Paper.rust.copy(alpha = 0.82f), RoundedCornerShape(0.3f.mm))
            .padding(metrics.gap)
            .border(metrics.inner, Paper.rust.copy(alpha = 0.72f), RoundedCornerShape(0.3f.mm))
            .semantics { contentDescription = COMPLETE_STAMP_WORD },
    ) {
        // Fixed line boxes in one explicitly spaced column keep the two baselines independent.
        // Independently aligned or padded Text nodes can each inherit the stamp's minimum height
        // at print density, making their glyphs overlap even though their alignments differ.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.lineGap),
        ) {
            Box(
                modifier = Modifier.size(width = metrics.contentWidth, height = metrics.ratioSlot),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = printedCompletionRatio(ratio),
                    fontFamily = BarlowCondensedFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = metrics.ratioSize,
                    letterSpacing = metrics.ratioLetterSpacing,
                    textAlign = TextAlign.Center,
                    color = Paper.rust.copy(alpha = 0.88f),
                )
            }
            Box(
                modifier = Modifier.size(width = metrics.contentWidth, height = metrics.wordSlot),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = COMPLETE_STAMP_WORD.uppercase(),
                    fontFamily = BarlowCondensedFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = metrics.wordSize,
                    letterSpacing = metrics.letterSpacing,
                    textAlign = TextAlign.Center,
                    color = Paper.rust.copy(alpha = 0.82f),
                )
            }
        }
    }
}

/** The compact screen figure given a little typographic air for the printed celebration. */
internal fun printedCompletionRatio(ratio: String): String =
    ratio.split('/').joinToString(" / ") { part -> part.trim() }

/**
 * How big the caucho is on each kind of heading: the masthead and the plain band take the measured
 * frame; the slim band of a shared folio gets half of it so the stamp still lands inside fourteen
 * millimetres.
 */
fun printedStampSize(heading: PrintHeading): DpSize = printedStampMetrics(heading).frame

private fun printedStampMetrics(heading: PrintHeading): StampMetrics = when (heading) {
    PrintHeading.Slim -> StampMetrics(
        frame = DpSize(12.dp, 11.dp),
        wordSize = 1.6f.sp,
        letterSpacing = 0.25f.sp,
        ratioSize = 2.2f.sp,
        contentWidth = 8f.mm,
        ratioSlot = 2.8f.mm,
        wordSlot = 2.2f.mm,
        lineGap = 0.8f.mm,
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
        ratioSlot = 5.2f.mm,
        wordSlot = 3.8f.mm,
        lineGap = 1.5f.mm,
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
    val ratioSlot: Dp,
    val wordSlot: Dp,
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
