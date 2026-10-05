package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.DiameterExtreme
import com.jenarvaezg.coindex.domain.MarginFigure
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.MetalSplit
import com.jenarvaezg.coindex.domain.a4Sheets
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.placementYear
import com.jenarvaezg.coindex.ui.CountryPortrait
import com.jenarvaezg.coindex.ui.FiguresLabels
import com.jenarvaezg.coindex.ui.FiguresSubject
import com.jenarvaezg.coindex.ui.SewnEdgeCounts
import com.jenarvaezg.coindex.ui.arcLabel
import com.jenarvaezg.coindex.ui.commonestYearSentence
import com.jenarvaezg.coindex.ui.components.AlbumChrome
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.ReferentLadder
import com.jenarvaezg.coindex.ui.coverageLabel
import com.jenarvaezg.coindex.ui.demonetizedSentence
import com.jenarvaezg.coindex.ui.eurosLabel
import com.jenarvaezg.coindex.ui.fineSilverSentence
import com.jenarvaezg.coindex.ui.kilogramsLabel
import com.jenarvaezg.coindex.ui.matterCensusLabel
import com.jenarvaezg.coindex.ui.metalLabel
import com.jenarvaezg.coindex.ui.mintSentence
import com.jenarvaezg.coindex.ui.moneyStampLabel
import com.jenarvaezg.coindex.ui.paidAgainstTodayLabel
import com.jenarvaezg.coindex.ui.percentLabel
import com.jenarvaezg.coindex.ui.portraitSharesLabel
import com.jenarvaezg.coindex.ui.sameHandSentence
import com.jenarvaezg.coindex.ui.screenDiameterLabel
import com.jenarvaezg.coindex.ui.squareMetresLabel
import com.jenarvaezg.coindex.ui.uncirculatedSentence
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * «Las cifras»: the third top-level cell, ordered by magnitude rather than by slots
 * (ADR 0026 §8, §10). It has no filters, sort or search.
 *
 * Every tappable figure leads to the pieces behind it; figures with nothing underneath are not
 * tappable. The money section is absent, not zero, until market prices arrive (ADR 0028 §7), and a
 * single line stands in its place meanwhile (#519). Everything else comes from the APK, so a fresh
 * install without network shows the rest of the page.
 *
 * @param onOpenCountry opens Monedas narrowed to a country, as the year axis does (#386).
 * @param onOpenYear opens Monedas narrowed to a year.
 */
@Composable
fun FiguresScreen(
    subject: FiguresSubject,
    /** Computed once above the three roots so they all show the same counts. */
    sewnEdge: SewnEdgeCounts?,
    nowMillis: Long,
    onOpenCountry: (String) -> Unit,
    onOpenYear: (Int) -> Unit,
    onOpenPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AlbumChrome(
            counts = sewnEdge,
            onOpenPhone = onOpenPhone,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            item("heading") {
                RootHeading(
                    destination = FiguresLabels.DESTINATION,
                    sentence = FiguresLabels.SENTENCE,
                )
            }
            // Money first. #316 rejected a self-updating amount in a permanent bar; here it is on a
            // page opened on purpose, with its source and the date of both its prices (#594).
            subject.money?.let { money ->
                item("money") {
                    Block(FiguresLabels.MONEY_HEADING) {
                        Text(
                            eurosLabel(money.value.eur),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        // Stamp right under the amount, method last: small caps against the divider
                        // read as the heading of the next block (#398).
                        Text(
                            moneyStampLabel(money.spot, money.value.catalogReadAt, nowMillis),
                            style = MaterialTheme.typography.labelMedium,
                            color = Paper.muted,
                        )
                        coverageLabel(money.value.valued, money.value.pieces)?.let { coverage ->
                            Text(
                                coverage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Paper.muted,
                            )
                        }
                        // A figure, hence the ink; the method still closes the block because it
                        // governs both the total and this comparison.
                        money.paid?.let { paid ->
                            Text(
                                paidAgainstTodayLabel(paid),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Paper.ink,
                            )
                        }
                        Text(
                            FiguresLabels.MONEY_ORIGIN,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Paper.muted,
                        )
                    }
                }
            }
            // In the money section's place and only there (#519).
            if (subject.moneyWaiting) {
                item("money-waiting") {
                    Block(FiguresLabels.MONEY_HEADING) {
                        Text(
                            FiguresLabels.MONEY_WAITING,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Paper.muted,
                        )
                    }
                }
            }
            item("matter") {
                Block(FiguresLabels.MATTER_HEADING) {
                    Text(
                        matterCensusLabel(subject.figures.pieces, subject.figures.issuers),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                    subject.ladders.forEach { reading ->
                        ReferentLadder(reading, modifier = Modifier.padding(top = 14.dp))
                    }
                    Text(
                        squareMetresLabel(
                            subject.figures.area.value,
                            subject.figures.area.a4Sheets(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Paper.muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            item("metal") {
                Block(FiguresLabels.METAL_HEADING) {
                    MetalBar(subject.figures.metals)
                    // Fine ounces under the metal bar (#398); nothing at all without silver.
                    if (subject.figures.fineSilver.value > 0.0) {
                        Text(
                            fineSilverSentence(gramsToOunces(subject.figures.fineSilver.value)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Paper.ink,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            subject.portrait?.let { portrait ->
                item("portrait") {
                    Block(FiguresLabels.PORTRAIT_HEADING) {
                        Portrait(portrait, onOpenCountry)
                    }
                }
            }
            subject.figures.arc?.let { arc ->
                item("arc") {
                    Block(FiguresLabels.ARC_HEADING) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Figure(arc.oldest.toString()) { onOpenYear(arc.oldest) }
                            Text(
                                arcLabel(arc.years),
                                style = MaterialTheme.typography.labelMedium,
                                color = Paper.muted,
                            )
                            Figure(arc.newest.toString()) { onOpenYear(arc.newest) }
                        }
                    }
                }
            }
            subject.figures.size?.let { size ->
                item("size") {
                    Block(FiguresLabels.SIZE_HEADING) {
                        // Centred: the two coins are a side-by-side comparison.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 28.dp,
                                alignment = Alignment.CenterHorizontally,
                            ),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            CoinToScale(size.smallest, size.largest.millimetres)
                            CoinToScale(size.largest, size.largest.millimetres)
                        }
                    }
                }
            }
            item("margins") {
                Block(FiguresLabels.MARGIN_HEADING) {
                    val margins = subject.figures.margins
                    MarginLine(demonetizedSentence(margins.demonetized))
                    // With the demonetized line, what the coins are; the three below, where they
                    // came from.
                    margins.uncirculated?.let { MarginLine(uncirculatedSentence(it)) }
                    margins.sameHand?.let { MarginLine(sameHandSentence(it)) }
                    margins.mostMinted?.let { MarginLine(mintSentence(it, margins.distinctMints)) }
                    margins.commonestYear?.let { year ->
                        MarginLine(
                            commonestYearSentence(year),
                            onClick = year.subject?.toIntOrNull()?.let { { onOpenYear(it) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Block(heading: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(heading)
        content()
        HorizontalDivider(color = Paper.hairline, modifier = Modifier.padding(top = 14.dp))
    }
}

/** A tappable number; only for figures that lead to pieces. */
@Composable
private fun Figure(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        color = Paper.moss,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/**
 * The metal split by mass, never by coin count: by count a mostly-silver collection is one colour,
 * while by mass the copper in a .835 alloy shows (`docs/ux/cifras-326.md`).
 */
@Composable
private fun MetalBar(split: MetalSplit) {
    if (split.measuredGrams <= 0.0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
    ) {
        split.masses.forEach { mass ->
            Box(
                modifier = Modifier
                    .weight(split.shareOf(mass).toFloat().coerceAtLeast(MIN_BAR_WEIGHT))
                    .fillMaxSize()
                    .drawBehind { drawRect(metalColour(mass.metal)) },
            )
        }
    }
    Text(
        split.masses.joinToString(" · ") { mass ->
            "${metalLabel(mass.metal)} ${kilogramsLabel(mass.grams)} " +
                "(${percentLabel(split.shareOf(mass))})"
        },
        style = MaterialTheme.typography.labelMedium,
        color = Paper.muted,
    )
}

/** Minimum weight so every metal in the label also shows in the bar. */
private const val MIN_BAR_WEIGHT = 0.004f

private fun metalColour(metal: Metal): Color = when (metal) {
    Metal.Silver -> Paper.line
    Metal.Gold -> Paper.rust
    Metal.Copper, Metal.Bronze, Metal.Brass, Metal.Cupronickel -> Paper.rust
    Metal.Platinum, Metal.Palladium -> Paper.hairline
    else -> Paper.muted
}

/**
 * The leading country and its shares; tapping it opens that country's pieces. The value share is
 * absent whenever the money section is.
 */
@Composable
private fun Portrait(portrait: CountryPortrait, onOpenCountry: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onOpenCountry(portrait.country) },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(portrait.country, style = MaterialTheme.typography.headlineMedium, color = Paper.moss)
        Text(
            portraitSharesLabel(portrait),
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
        )
    }
}

/** One coin drawn at its diameter relative to the largest («a la misma escala»). */
@Composable
private fun CoinToScale(extreme: DiameterExtreme, largestMillimetres: Double) {
    val fraction = (extreme.millimetres / largestMillimetres).coerceIn(0.2, 1.0)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size((MAX_COIN_DIAMETER * fraction).dp)
                .drawBehind {
                    drawCircle(Paper.line)
                    drawCircle(
                        color = Paper.paper,
                        radius = size.minDimension / 2f - RIM_WIDTH,
                    )
                },
        )
        Text(
            screenDiameterLabel(extreme.millimetres),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            placementYear(extreme.item, extreme.meta)?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = Paper.muted,
            textAlign = TextAlign.Center,
        )
    }
}

private const val MAX_COIN_DIAMETER = 84.0
private const val RIM_WIDTH = 3f

@Composable
private fun MarginLine(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (onClick == null) Paper.ink else Paper.moss,
        modifier = if (onClick == null) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
        }.padding(vertical = 2.dp),
    )
}
