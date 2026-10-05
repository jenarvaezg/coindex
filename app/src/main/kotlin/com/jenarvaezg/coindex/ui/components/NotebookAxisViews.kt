package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.shelf.CountryAxisBlock
import com.jenarvaezg.coindex.ui.shelf.CountryAxisCell
import com.jenarvaezg.coindex.ui.shelf.CountryAxisModel
import com.jenarvaezg.coindex.ui.shelf.YearAxisCentury
import com.jenarvaezg.coindex.ui.shelf.YearAxisDecade
import com.jenarvaezg.coindex.ui.shelf.YearAxisIsland
import com.jenarvaezg.coindex.ui.shelf.YearAxisModel
import com.jenarvaezg.coindex.ui.shelf.YearCellState
import com.jenarvaezg.coindex.ui.shelf.countryAxisFoldAction
import com.jenarvaezg.coindex.ui.shelf.countryAxisFoldLabel
import com.jenarvaezg.coindex.ui.shelf.fold
import com.jenarvaezg.coindex.ui.shelf.yearAxisQuantityMark
import com.jenarvaezg.coindex.ui.theme.Paper

/** Gap between axis cells (atlas-315, #340): fits ten year cells across a phone's decade row. */
val AXIS_GAP = 5.dp

/** Dense hole of the country and year axes — many casillas at once, not a card. */
val AXIS_HOLE = 34.dp

/** Country-axis label column: name + fraction + fold, left of the wrapping holes. */
private val COUNTRY_LABEL_WIDTH = 88.dp

/** Between that column and the holes, and part of the width the fold counts columns in. */
private val COUNTRY_LABEL_GAP = 8.dp

/** Decade label column of the year axis («1960»): four digits, leaving the rest to ten seats. */
private val YEAR_DECADE_LABEL_WIDTH = 36.dp

/** Pinprick of bare cardboard — the atlas's third state, not an empty Box. */
private val BARE_DOT = 3.dp

/** Vertical padding of a decade row, enough that a quantity mark doesn't touch the next (#406). */
private val YEAR_DECADE_ROW_PAD = 5.dp

/**
 * Rust quantity on a year seat — larger than [labelLarge] so ×N still reads in a dense calendar.
 */
@Composable
private fun yearAxisQuantityStyle() =
    MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)

/**
 * One country block: label and ratio on the left, wrapping holes on the right (atlas-315). Loose
 * pieces drop the cardboard (`backed = false`) so they read as coins, not holes to fill. A tap
 * opens Monedas filtered to that country.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryAxisRow(
    block: CountryAxisBlock,
    images: Map<Int, TypeImages>,
    onCountryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggleFold: (String) -> Unit = {},
) {
    // The fold counts how many holes fit in the width this block actually got, so wider screens
    // show more before folding.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onCountryClick(block.country) }),
    ) {
        val holesWidth = maxWidth - COUNTRY_LABEL_WIDTH - COUNTRY_LABEL_GAP
        val columns = ((holesWidth + AXIS_GAP) / (AXIS_HOLE + AXIS_GAP)).toInt()
        val fold = block.fold(columns = columns, expanded = expanded)
        Row(horizontalArrangement = Arrangement.spacedBy(COUNTRY_LABEL_GAP)) {
            Column(modifier = Modifier.width(COUNTRY_LABEL_WIDTH)) {
                Text(
                    block.country,
                    style = MaterialTheme.typography.titleMedium,
                    color = Paper.ink,
                )
                Text(
                    block.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.rust,
                    modifier = Modifier.padding(top = 2.dp),
                )
                // Under the ratio, which it continues («42/115 … y faltan 66»); after the holes it
                // took a line of its own whenever the row was full.
                if (fold.foldable > 0) {
                    CountryAxisFoldMark(
                        hidden = fold.foldable,
                        expanded = expanded,
                        onClick = { onToggleFold(block.country) },
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AXIS_GAP),
                verticalArrangement = Arrangement.spacedBy(AXIS_GAP),
                modifier = Modifier.weight(1f),
            ) {
                for (cell in fold.cells) {
                    AxisHole(cell = cell, images = images)
                }
            }
        }
    }
}

/**
 * The «… y faltan 66» that unfolds a country's absences in place, and folds them back (#417). Its
 * own click, so it doesn't open Monedas like the rest of the block; underlined so it reads as
 * pressable. [minimumInteractiveComponentSize] gives it a full touch target, as for a casilla's
 * year tag (#473).
 */
@Composable
private fun CountryAxisFoldMark(
    hidden: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                role = Role.Button,
                onClickLabel = countryAxisFoldAction(hidden = hidden, expanded = expanded),
                onClick = onClick,
            )
            .padding(horizontal = 4.dp),
    ) {
        Text(
            countryAxisFoldLabel(hidden = hidden, expanded = expanded),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.rust,
            textDecoration = TextDecoration.Underline,
        )
    }
}

/** Compact tail: one or two loose coins in a running line (atlas-315 / eje-pais-cola). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryAxisTail(
    blocks: List<CountryAxisBlock>,
    images: Map<Int, TypeImages>,
    onCountryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        for (block in blocks) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = { onCountryClick(block.country) },
                ),
            ) {
                Text(
                    block.country,
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.ink,
                )
                for (cell in block.cells) {
                    AxisHole(cell = cell, images = images)
                }
            }
        }
    }
}

@Composable
private fun AxisHole(cell: CountryAxisCell, images: Map<Int, TypeImages>) {
    val typeId = cell.typeId
    val photo = typeId?.let { images[it]?.printedPhoto(PrintedSide.Reverse) }
    when (cell) {
        // A casilla of the card's plate, so «te falta». [AXIS_HOLE] is under [GHOST_MIN_DP], so it
        // draws the whole coin under the dotted rule (#556).
        is CountryAxisCell.Slot -> AlbumHole(
            photo = photo,
            absence = if (cell.owned) HoleAbsence.Filled else HoleAbsence.Missing,
            modifier = Modifier.size(AXIS_HOLE),
        )
        is CountryAxisCell.Loose -> AlbumHole(
            photo = photo,
            backed = false,
            modifier = Modifier.size(AXIS_HOLE),
        )
    }
}

/** Rust «×N» over a year-axis hole when more than one piece shares the seat (#406). */
@Composable
private fun YearAxisQuantityMark(quantity: Int, modifier: Modifier = Modifier) {
    yearAxisQuantityMark(quantity)?.let { mark ->
        Text(
            mark,
            style = yearAxisQuantityStyle(),
            color = Paper.rust,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(end = 2.dp, bottom = 2.dp),
        )
    }
}

/**
 * Digit header of the year axis (atlas-315): «0»…«9» above the ten seats of every decade.
 *
 * The empty label column keeps the digits aligned with the holes, not with the decade years.
 */
@Composable
fun YearAxisDigitHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(YEAR_DECADE_LABEL_WIDTH))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AXIS_GAP),
        ) {
            for (digit in 0..9) {
                Text(
                    digit.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                )
            }
        }
    }
}

/**
 * One decade of the year axis: label and ten cells, each a coin, a ghost or bare cardboard
 * (ADR 0026 §9). Bare cardboard is a pinprick (atlas-315), so the calendar doesn't read as holes.
 * Coins show a count when several pieces share the year. Coin and ghost cells open Monedas on that
 * year.
 */
@Composable
fun YearAxisDecadeRow(
    decade: YearAxisDecade,
    images: Map<Int, TypeImages>,
    onYearClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byOffset = decade.cells.associateBy { it.year - decade.decade }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            decade.decade.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.width(YEAR_DECADE_LABEL_WIDTH),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AXIS_GAP),
        ) {
            for (offset in 0..9) {
                val cell = byOffset[offset]
                val year = cell?.year ?: (decade.decade + offset)
                val opens = cell?.state is YearCellState.Coin || cell?.state == YearCellState.Ghost
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .then(
                            if (opens) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = { onYearClick(year) },
                                )
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when (val state = cell?.state) {
                        is YearCellState.Coin -> {
                            val photo = state.typeId?.let {
                                images[it]?.printedPhoto(PrintedSide.Reverse)
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomEnd,
                            ) {
                                AlbumHole(photo = photo, modifier = Modifier.fillMaxSize())
                                YearAxisQuantityMark(state.quantity)
                            }
                        }
                        // No photograph on purpose: a ghost year is an empty seat, not a coin, so
                        // only the dashed hole is drawn.
                        YearCellState.Ghost -> AlbumHole(
                            photo = null,
                            absence = HoleAbsence.Missing,
                            modifier = Modifier.fillMaxSize(),
                        )
                        YearCellState.Bare, null -> BareYearDot()
                    }
                }
            }
        }
    }
}

/** The atlas's bare cardboard: a quiet pinprick that keeps the decade ten seats wide. */
@Composable
private fun BareYearDot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(BARE_DOT)) {
        drawCircle(color = Paper.muted.copy(alpha = 0.55f))
    }
}

@Composable
fun YearAxisCenturyHeader(century: YearAxisCentury, modifier: Modifier = Modifier) {
    Eyebrow(century.label, modifier = modifier.padding(top = 10.dp, bottom = 4.dp))
}

/**
 * Front island of the year axis: pieces outside the dated calendar (Romans), drawn like a country
 * block of loose coins so the calendar doesn't open empty centuries for them. A tap opens Monedas
 * on the island's issuer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearAxisIslandRow(
    island: YearAxisIsland,
    images: Map<Int, TypeImages>,
    onCountryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onCountryClick(island.title) }),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                island.title,
                style = MaterialTheme.typography.titleMedium,
                color = Paper.ink,
            )
            Text(
                island.coins.sumOf { it.quantity }.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Paper.rust,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AXIS_GAP),
            verticalArrangement = Arrangement.spacedBy(AXIS_GAP),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            for (coin in island.coins) {
                val photo = images[coin.typeId]?.printedPhoto(PrintedSide.Reverse)
                Box(
                    modifier = Modifier.size(AXIS_HOLE),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    AlbumHole(
                        photo = photo,
                        modifier = Modifier.size(AXIS_HOLE),
                    )
                    YearAxisQuantityMark(coin.quantity)
                }
            }
        }
    }
}

/** Items of the country axis for a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]. */
fun LazyGridScope.countryAxisItems(
    model: CountryAxisModel,
    images: Map<Int, TypeImages>,
    onCountryClick: (String) -> Unit,
    /** Countries whose fold the collector opened (#417). */
    expandedCountries: Set<String> = emptySet(),
    onToggleFold: (String) -> Unit = {},
) {
    items(model.body, key = { "country-${it.country}" }) { block ->
        CountryAxisRow(
            block = block,
            images = images,
            onCountryClick = onCountryClick,
            expanded = block.country in expandedCountries,
            onToggleFold = onToggleFold,
        )
    }
    if (model.tail.isNotEmpty()) {
        item(
            key = "country-tail",
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) {
            CountryAxisTail(
                blocks = model.tail,
                images = images,
                onCountryClick = onCountryClick,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Items of the year axis: islands first, then a sticky «0»…«9» header and the calendar (#406). The
 * header's paper background hides the decades scrolling under it.
 */
fun LazyGridScope.yearAxisItems(
    model: YearAxisModel,
    images: Map<Int, TypeImages>,
    onCountryClick: (String) -> Unit,
    onYearClick: (Int) -> Unit,
) {
    items(
        items = model.islands,
        key = { "island-${it.title}" },
        span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
    ) { island ->
        YearAxisIslandRow(
            island = island,
            images = images,
            onCountryClick = onCountryClick,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
    if (model.cells.isNotEmpty()) {
        stickyHeader(key = "year-digits") {
            YearAxisDigitHeader(
                modifier = Modifier
                    .background(Paper.paper)
                    .padding(top = 2.dp, bottom = 4.dp),
            )
        }
    }
    for (century in model.centuries) {
        item(
            key = "century-${century.century}",
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) {
            YearAxisCenturyHeader(century)
        }
        items(
            items = century.decades,
            // Century in the key: the grouping convention decides which century a decade falls in
            // (#407), and the key must stay unique under any convention.
            key = { "decade-${century.century}-${it.decade}" },
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) { decade ->
            YearAxisDecadeRow(
                decade = decade,
                images = images,
                onYearClick = onYearClick,
                modifier = Modifier.padding(vertical = YEAR_DECADE_ROW_PAD),
            )
        }
    }
}
