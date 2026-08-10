package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * The measured gap of the year axis at phone size (atlas-315 / #340).
 *
 * Thirteen device pixels at 420 dpi is five density-independent pixels, and that is what keeps ten
 * year cells across a decade row without crushing the coin or opening a ditch.
 */
val AXIS_GAP = 5.dp

/** Dense hole of the country and year axes — many casillas at once, not a card. */
val AXIS_HOLE = 34.dp

/** Country-axis label column: name + fraction, left of the wrapping holes. */
private val COUNTRY_LABEL_WIDTH = 88.dp

/**
 * Decade label column of the year axis (atlas-315: «1870», «1960» left of the ten seats).
 *
 * Narrower than a country name: four digits of muted ink, and the ten year seats share what is
 * left so a phone still fits the calendar without scrolling sideways.
 */
private val YEAR_DECADE_LABEL_WIDTH = 36.dp

/** Pinprick of bare cardboard — the atlas's third state, not an empty Box. */
private val BARE_DOT = 3.dp

/**
 * One country block: label and ratio on the left, wrapping holes on the right (atlas-315).
 *
 * Loose cells keep the photograph and drop the cardboard ([AlbumHole.backed] = false), which is
 * how a piece no casilla claims still reads as a coin and not as a hole to fill.
 *
 * A tap opens Monedas with that country already on the shelf — the axis shows the album; the list
 * is where those pieces are read one by one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryAxisRow(
    block: CountryAxisBlock,
    images: Map<Int, TypeImages>,
    onCountryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = { onCountryClick(block.country) }),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AXIS_GAP),
            verticalArrangement = Arrangement.spacedBy(AXIS_GAP),
            modifier = Modifier.weight(1f),
        ) {
            for (cell in block.cells) {
                AxisHole(cell = cell, images = images)
            }
        }
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
        is CountryAxisCell.Slot -> AlbumHole(
            photo = photo,
            missing = !cell.owned,
            modifier = Modifier.size(AXIS_HOLE),
        )
        is CountryAxisCell.Loose -> AlbumHole(
            photo = photo,
            backed = false,
            modifier = Modifier.size(AXIS_HOLE),
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
 * One decade of the year axis: label + ten cells, coin / ghost / bare cardboard (ADR 0026 §9).
 *
 * Bare cardboard is a pinprick (atlas-315), not an empty seat — without it the calendar reads as
 * holes in the middle. Ghosts keep the dashed hole; coins carry a rust count when more than one
 * piece lands on the year. Cells share the width after the decade label so ten still fit a phone.
 *
 * Coin and ghost seats open Monedas on that year's era band; bare cardboard stays quiet — there is
 * nothing of this collection to read there.
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
                                if (state.quantity > 1) {
                                    Text(
                                        state.quantity.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Paper.rust,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(end = 1.dp, bottom = 1.dp),
                                    )
                                }
                            }
                        }
                        YearCellState.Ghost -> AlbumHole(
                            photo = null,
                            missing = true,
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
 * Front island of the year axis: pieces whose year falls outside the dated calendar (Romans).
 *
 * Same reading as a country block of only loose coins — name, count, photographs without a slot
 * to fill — so the calendar below never opens seventeen empty centuries for two denarii.
 *
 * A tap opens Monedas on that country: the island is already titled by issuer, and that is the
 * shelf facet that reaches those pieces without inventing an era they do not sit in.
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
                    if (coin.quantity > 1) {
                        Text(
                            coin.quantity.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Paper.rust,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(end = 1.dp, bottom = 1.dp),
                        )
                    }
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
) {
    items(model.body, key = { "country-${it.country}" }) { block ->
        CountryAxisRow(block = block, images = images, onCountryClick = onCountryClick)
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

/** Items of the year axis: islands first, then the digit header and the calendar. */
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
        item(
            key = "year-digits",
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) {
            YearAxisDigitHeader(modifier = Modifier.padding(bottom = 2.dp))
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
            // Century in the key: year 100 and year 101 share decade 100 across two centuries.
            key = { "decade-${century.century}-${it.decade}" },
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) { decade ->
            YearAxisDecadeRow(
                decade = decade,
                images = images,
                onYearClick = onYearClick,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
