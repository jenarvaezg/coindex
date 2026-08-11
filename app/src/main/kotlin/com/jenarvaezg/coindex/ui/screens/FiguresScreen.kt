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
import com.jenarvaezg.coindex.ui.percentLabel
import com.jenarvaezg.coindex.ui.portraitSharesLabel
import com.jenarvaezg.coindex.ui.sameHandSentence
import com.jenarvaezg.coindex.ui.screenDiameterLabel
import com.jenarvaezg.coindex.ui.spotStampLabel
import com.jenarvaezg.coindex.ui.squareMetresLabel
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * «Las cifras»: the third cell of the top level, and the one page that is not made of slots
 * (ADR 0026 §8, §10).
 *
 * **It carries no shelf.** No filters, no sorting, no search box: its order is chosen by the figure you
 * touch. Collections orders the collection by plate, Coins by type, and this one by magnitude.
 *
 * **It goes down.** What earns this a cell of its own is a grain of its own — grams — and what makes it a
 * hierarchy rather than a dashboard is that every figure that can be touched leads to the pieces that
 * compose it. The ones that cannot be touched are drawings and totals with nothing underneath them, and
 * they are silent rather than tappable-and-inert.
 *
 * **The money is absent, not zero, until the market has landed** (ADR 0028 §7). Everything else on the
 * page comes out of the APK, so a freshly installed phone with no network opens it whole.
 *
 * @param onOpenCountry the pieces of a country, which is the shelf of Coins narrowed the way the year
 *   axis already narrows it (#386).
 * @param onOpenYear the pieces of one year, the same way.
 */
@Composable
fun FiguresScreen(
    subject: FiguresSubject,
    /** The sewn-edge census, assembled once above the three roots so this screen cannot invent its own. */
    sewnEdge: SewnEdgeCounts,
    nowMillis: Long,
    onOpenCountry: (String) -> Unit,
    onOpenYear: (Int) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AlbumChrome(
            counts = sewnEdge,
            onSettings = onSettings,
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
            // The money opens the page, and that does not contradict #316: what was rejected there is an
            // amount that changes on its own in a permanent bar. Here it is on a page opened on purpose,
            // with its origin stated and the spot's date under it.
            subject.money?.let { money ->
                item("money") {
                    Block(FiguresLabels.MONEY_HEADING) {
                        Text(
                            eurosLabel(money.value.eur),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        // The stamp rides **against the amount** and the method closes the block, which
                        // is the order that stops the small caps reading as a heading: in rust and
                        // pressed against the divider they were the eyebrow of the block below (#398).
                        Text(
                            spotStampLabel(money.spot, nowMillis),
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
                        Text(
                            FiguresLabels.MONEY_ORIGIN,
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
                    // The fine ounces, where the prototype had them: right under the bar that has just
                    // split the mass, and not next to a weight «la materia» already says (#398). A
                    // collection with no silver in it does not say it has none — it says nothing.
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
                        // Centred, because what the block is is a **comparison**: the two coins side
                        // by side with the sheet's margin on both of them, and not a pair pushed
                        // against the left edge with half a screen of paper to their right.
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

/** A number with something underneath it. Never drawn for a figure that leads nowhere. */
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
 * The metal bar, **by mass and never by coin**.
 *
 * By coin it is one colour — 565 of his 574 pieces are silver — and by mass it says that almost a kilo of
 * the collection is not silver, because a .835 coin is 16,5 % copper. It grows on its own the day another
 * metal arrives, which is what the bar was asked for (`docs/ux/cifras-326.md`).
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

/** A metal with a share too small to draw still gets a sliver: the bar lists what the label lists. */
private const val MIN_BAR_WEIGHT = 0.004f

private fun metalColour(metal: Metal): Color = when (metal) {
    Metal.Silver -> Paper.line
    Metal.Gold -> Paper.rust
    Metal.Copper, Metal.Bronze, Metal.Brass, Metal.Cupronickel -> Paper.rust
    Metal.Platinum, Metal.Palladium -> Paper.hairline
    else -> Paper.muted
}

/**
 * The collection's portrait: one country and the three or four things it is a share of.
 *
 * Touchable, and it is the drill-down the whole page is built around: «Venezuela · 62 %» leads to those
 * pieces. The share of the **value** is money, so it is absent exactly when the money section is.
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

/**
 * One coin drawn at its real diameter relative to the largest, which is what «a la misma escala» means.
 *
 * A drawing and not a figure, and it is what makes this a field guide: `size` is in 100 % of the types, so
 * the smallest coin against the largest costs nothing.
 */
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
