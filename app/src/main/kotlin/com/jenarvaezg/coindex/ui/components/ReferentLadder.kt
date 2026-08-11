package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.LadderUnit
import com.jenarvaezg.coindex.domain.Rung as DomainRung
import com.jenarvaezg.coindex.ui.FiguresLabels
import com.jenarvaezg.coindex.ui.LadderReading
import com.jenarvaezg.coindex.ui.ladderAmountLabel
import com.jenarvaezg.coindex.ui.ladderComparison
import com.jenarvaezg.coindex.ui.theme.Paper

/** How far the mark hangs below the rule, and how tall it is. */
private val MARK_DROP = 22.dp

/** The rule's own weight, and the mark's. */
private const val RULE_WIDTH = 2f
private const val MARK_WIDTH = 3f

/**
 * One ladder of five referents with the collection interpolated between two of them.
 *
 * The decision that orders the whole page, and it did not come out of the drawing but out of the
 * collector: **the comparison does not decorate the figure, it is the figure**. «6,95 kg» says nothing;
 * «more than a cat and 310 g short of a bowling ball» does.
 *
 * Three things the shape obliges:
 *
 * - **The scale is ordinal, not metric.** The five figures are equally spaced and the collection is
 *   interpolated between its two neighbours. Logarithmic was tried first and piled three labels on top of
 *   the fourth — the bowling ball and the tyre overlapped and the lorry vanished under the mark. For the
 *   same reason it **carries no zoom**: zooming an ordinal scale means nothing, and making it metric
 *   brings the overlaps back.
 * - **The rule has to say what it measures.** Without «una al lado de otra llegan a» the two lower
 *   ladders are two lines with animals on them. Those five words are not furniture, they are the figure's
 *   own sentence (`docs/ux/cifras-326.md`).
 * - **The mark hangs below the rule.** Above it, it covered the label of the nearest referent exactly
 *   whenever the collection landed near one — which is precisely when the ladder is saying something.
 */
@Composable
fun ReferentLadder(reading: LadderReading, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            FiguresLabels.ladderStatement(reading.ladder.kind),
            style = MaterialTheme.typography.labelMedium,
            color = Paper.muted,
        )
        Text(
            ladderAmountLabel(reading.ladder.unit, reading.amount, reading.approximate),
            style = MaterialTheme.typography.headlineMedium,
        )
        // The sentence the ladder exists for: what has just been passed and what is within reach.
        ladderComparison(reading).takeIf(String::isNotEmpty)?.let { comparison ->
            Text(comparison, style = MaterialTheme.typography.bodyMedium, color = Paper.ink)
        }
        Rungs(reading)
    }
}

@Composable
private fun Rungs(reading: LadderReading) {
    val rungs = reading.ladder.rungs
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        // The figures stand on the rule, so they are laid out first and share its horizontal spacing.
        RungRow(
            modifier = Modifier.fillMaxWidth().height(SILHOUETTE_HEIGHT),
            count = rungs.size,
        ) { index ->
            ReferentSilhouette(
                referent = rungs[index].referent,
                height = SILHOUETTE_HEIGHT,
                color = Paper.line,
                paper = Paper.paper,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MARK_DROP)
                .drawBehind {
                    drawLine(
                        color = Paper.ink,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = RULE_WIDTH,
                    )
                    // Ticks where the referents stand, so the equal spacing is visible and the mark
                    // can be read against it rather than against the labels.
                    val step = size.width / (rungs.size - 1)
                    rungs.indices.forEach { index ->
                        val x = (index * step).coerceIn(0f, size.width)
                        drawLine(
                            color = Paper.line,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height * 0.35f),
                            strokeWidth = RULE_WIDTH,
                        )
                    }
                    val markX = (reading.placement.fraction.toFloat() * size.width)
                        .coerceIn(0f, size.width)
                    drawLine(
                        color = Paper.rust,
                        start = Offset(markX, 0f),
                        end = Offset(markX, size.height),
                        strokeWidth = MARK_WIDTH,
                    )
                },
        )
        RungRow(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), count = rungs.size) { index ->
            Rung(rungs[index], reading.ladder.unit)
        }
    }
}

/**
 * One referent under the rule: what it is, and **how much of it there is**.
 *
 * The magnitude is what the implementation of #326 dropped and what the prototype had — `ladrillo
 * 2,00 kg`. Without it the five names are an order the collector has to take on trust: nothing on
 * screen says why the brick comes before the cat, and the mark cannot be checked against anything but
 * the equal spacing of the ticks, which is ordinal and deliberately says nothing about distance (#398).
 *
 * It is the referent's own magnitude and never «unos»: the extrapolation belongs to the collection's
 * figure — a third of the types have no `thickness` — and a bowling ball weighs 7,26 kg exactly.
 */
@Composable
private fun Rung(rung: DomainRung, unit: LadderUnit) {
    Column(
        modifier = Modifier.width(LABEL_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            FiguresLabels.referent(rung.referent),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
            textAlign = TextAlign.Center,
        )
        Text(
            ladderAmountLabel(unit, rung.amount, approximate = false),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.line,
            textAlign = TextAlign.Center,
        )
    }
}

private val LABEL_WIDTH: Dp = 58.dp

/**
 * [count] cells across the full width, the first flush left and the last flush right.
 *
 * `SpaceBetween` would do it for the labels, but the figures have different widths and the rule's ticks
 * are drawn at exact fractions: what has to line up is each cell's **centre** with its tick, and only
 * the ends are the exception — a bus centred on the last tick would hang half of itself off the sheet.
 */
@Composable
private fun RungRow(modifier: Modifier, count: Int, cell: @Composable (Int) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(count) { index -> Box(contentAlignment = Alignment.BottomCenter) { cell(index) } }
    }
}
