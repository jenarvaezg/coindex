package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.ui.ShowcaseLabels
import com.jenarvaezg.coindex.ui.ShowcaseSort
import com.jenarvaezg.coindex.ui.ShowcaseTile
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.HoleAbsence
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.FilterChip
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.showcaseOrderNote
import com.jenarvaezg.coindex.ui.showcaseShelf
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.wishDoorLabel

/**
 * «Explorar»: the plates where something is missing (ADR 0030 §8, the annex of ADR 0026 §8). One
 * grid holds both curated plates the collector owns nothing of and their own plates with a marked
 * casilla, ordered «primero lo que busco». Entered from the last row of Colecciones; left with
 * «Volver».
 *
 * The two kinds share a grid because a wish mark is a state of a casilla, not a section
 * (ADR 0029 §2), and splitting them would sort by ownership. «Lo que busco» keeps its own screen
 * behind the door at the top, since it is the sheet taken to a fair (ADR 0029 §7).
 */
@Composable
fun ExploreScreen(
    tiles: List<ShowcaseTile>,
    /** Marked casillas; at zero there is no door to «Lo que busco». */
    wishes: Int,
    images: Map<Int, TypeImages>,
    onOpenPlate: (String) -> Unit,
    onOpenWishes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Survive rotation but not a relaunch, unlike the persisted narrowings of ADR 0021 §1: this
    // shelf has no filter chips, only an order and a search box.
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(ShowcaseSort.ByCasillas) }
    val shown = showcaseShelf(tiles, sort, query)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(SHELF_CARD_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SHELF_MARGIN, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(SHELF_GUTTER),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Same door shape as the index's door into this screen (ADR 0026 §8 clause 3).
                if (wishes > 0) {
                    AnnexDoor(label = wishDoorLabel(wishes), onOpen = onOpenWishes)
                }
                Text(
                    ShowcaseLabels.SENTENCE,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.muted,
                )
                // Browsing is free: said once for the shelf, not on every tile.
                Text(
                    ShowcaseLabels.FREE_SENTENCE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.muted,
                )
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = ShowcaseLabels.SEARCH_PLACEHOLDER,
                )
                // The shown tiles, so the note under the orders counts what is on screen.
                ShelfOrder(sort = sort, shelf = shown, onSort = { sort = it })
            }
        }
        if (shown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (tiles.isEmpty()) ShowcaseLabels.EMPTY else ShowcaseLabels.NO_MATCHES,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.muted,
                    )
                }
            }
        }
        items(shown, key = ShowcaseTile::catalogId) { tile ->
            ShelfTile(
                tile = tile,
                images = images[tile.typeId],
                onOpen = { onOpenPlate(tile.catalogId) },
            )
        }
    }
}

/** The index's margin, gutter and card width. */
private val SHELF_MARGIN = 12.dp
private val SHELF_GUTTER = 8.dp
private val SHELF_CARD_WIDTH = 104.dp

/**
 * One plate of the shelf, drawn like the index's `CollectionCard` and, like it, without a
 * travelling coin (ADR 0026 §3).
 */
@Composable
private fun ShelfTile(
    tile: ShowcaseTile,
    images: TypeImages?,
    onOpen: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        AlbumHole(
            photo = images?.printedPhoto(tile.printedSide),
            // Drawn from what the hole holds, not from which kind of plate it is (#556): the ghost
            // means «te falta», which is wrong for a plate the collector doesn't collect, and would
            // split the shelf by ownership. A coin not theirs gets the dotted rule (#520).
            absence = if (tile.coverOwned) HoleAbsence.Filled else HoleAbsence.Wanted,
            // No `otherSide`, as in `CollectionCard`: with one the hole takes the tap to flip the
            // coin and the plate never opens.
            modifier = Modifier.size(SHELF_CARD_WIDTH),
        )
        CollectionName(tile.name)
        Text(
            tile.footnote,
            style = MaterialTheme.typography.labelLarge,
            color = if (tile.mine) Paper.rust else Paper.moss,
            textAlign = TextAlign.Center,
        )
        // Why one of the collector's plates is here. Lower case, like the chip in the hole
        // (ADR 0029 §5).
        tile.marks?.let { marks ->
            Text(
                marks,
                style = MaterialTheme.typography.labelMedium,
                color = Paper.moss,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The shelf's two orders as chips, and a note on what the current one couldn't place.
 *
 * Not a `FilterShelf`: there are no facets to fold (ADR 0026 §8 clause 4). The order in force is a
 * selected [FilterChip] (#513). The note is [showcaseOrderNote]: «por coste de entrar» sorts only
 * what has an amount (ADR 0030 §8 clause 3), so on an unvalued shelf it visibly does nothing and
 * needs saying.
 */
@Composable
private fun ShelfOrder(
    sort: ShowcaseSort,
    shelf: List<ShowcaseTile>,
    onSort: (ShowcaseSort) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // FlowRow so the chips wrap at large font sizes instead of ellipsizing their one-line
        // labels.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ShowcaseSort.entries.forEach { order ->
                FilterChip(
                    label = order.label,
                    // No count: an order keeps the whole shelf.
                    count = null,
                    selected = order == sort,
                    onClick = { onSort(order) },
                    // 48 dp touch target around the 30 dp chip, without enlarging the chip.
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
            }
        }
        showcaseOrderNote(sort, shelf)?.let { note ->
            Text(
                note,
                // Body text, not `labelMedium` small caps, which would read as a heading for the
                // chips.
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
            )
        }
    }
}

