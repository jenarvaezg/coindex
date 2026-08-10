package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.shelf.CountryAxisBlock
import com.jenarvaezg.coindex.ui.shelf.CountryAxisCell
import com.jenarvaezg.coindex.ui.shelf.CountryAxisModel
import com.jenarvaezg.coindex.ui.shelf.YearAxisCentury
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
 * One country block: label and ratio on the left, wrapping holes on the right (atlas-315).
 *
 * Loose cells keep the photograph and drop the cardboard ([AlbumHole.backed] = false), which is
 * how a piece no casilla claims still reads as a coin and not as a hole to fill.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountryAxisRow(
    block: CountryAxisBlock,
    images: Map<Int, TypeImages>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
 * One decade of the year axis: ten cells, coin / ghost / bare cardboard (ADR 0026 §9).
 *
 * Bare cardboard draws nothing — the gap itself is the shape of the collection. Ghosts keep the
 * dashed hole; coins carry a rust count when more than one piece lands on the year.
 */
@Composable
fun YearAxisDecadeRow(
    decade: com.jenarvaezg.coindex.ui.shelf.YearAxisDecade,
    images: Map<Int, TypeImages>,
    modifier: Modifier = Modifier,
) {
    val byOffset = decade.cells.associateBy { it.year - decade.decade }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AXIS_GAP),
    ) {
        for (offset in 0..9) {
            val cell = byOffset[offset]
            Box(
                modifier = Modifier.size(AXIS_HOLE),
                contentAlignment = Alignment.BottomEnd,
            ) {
                when (val state = cell?.state) {
                    is YearCellState.Coin -> {
                        val photo = state.typeId?.let {
                            images[it]?.printedPhoto(PrintedSide.Reverse)
                        }
                        AlbumHole(photo = photo, modifier = Modifier.size(AXIS_HOLE))
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
                    YearCellState.Ghost -> AlbumHole(
                        photo = null,
                        missing = true,
                        modifier = Modifier.size(AXIS_HOLE),
                    )
                    YearCellState.Bare, null -> {
                        // Cartón desnudo, or a year outside the arc: the seat stays so the decade
                        // stays ten wide, and paints nothing.
                    }
                }
            }
        }
    }
}

@Composable
fun YearAxisCenturyHeader(century: YearAxisCentury, modifier: Modifier = Modifier) {
    Eyebrow(century.label, modifier = modifier.padding(top = 10.dp, bottom = 4.dp))
}

/** Items of the country axis for a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]. */
fun LazyGridScope.countryAxisItems(
    model: CountryAxisModel,
    images: Map<Int, TypeImages>,
) {
    items(model.body, key = { "country-${it.country}" }) { block ->
        CountryAxisRow(block = block, images = images)
    }
    if (model.tail.isNotEmpty()) {
        item(
            key = "country-tail",
            span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) },
        ) {
            CountryAxisTail(
                blocks = model.tail,
                images = images,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Items of the year axis, century by century. */
fun LazyGridScope.yearAxisItems(
    model: YearAxisModel,
    images: Map<Int, TypeImages>,
) {
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
            YearAxisDecadeRow(decade = decade, images = images)
        }
    }
}
