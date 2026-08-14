package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.showcaseShelf
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.wishDoorLabel

/**
 * «Explorar»: the plates where something is missing (ADR 0030 §8).
 *
 * The annex of ADR 0026 §8, and since the shelf window arrived it is a **shelf** rather than a list: the
 * twenty curated plates the collector owns nothing of, and their own plates holding a marked casilla, in
 * one grid ordered «primero lo que busco». It has no cell and no bar, it is entered from the last row of
 * the Colecciones list and it is left with «Volver».
 *
 * **The mark is a state of a casilla and not a section of a screen** (ADR 0029 §2), which is why the two
 * populations share a grid instead of being stacked: a screen that showed the twenty and hid the three
 * plates where the collector is actually hunting would be sorting by ownership, and ownership is the one
 * thing they are not thinking about in here.
 *
 * «Lo que busco» keeps a screen of its own behind the door at the head of the shelf, because what it is
 * for is the sheet taken to a fair (ADR 0029 §7).
 */
@Composable
fun ExploreScreen(
    tiles: List<ShowcaseTile>,
    /** How many casillas are marked, for the door. Zero prints no door: there is nothing behind it. */
    wishes: Int,
    images: Map<Int, TypeImages>,
    onOpenPlate: (String) -> Unit,
    onOpenWishes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The shelf's own two controls, and they live here rather than in the state that survives a launch
    // (ADR 0021 §1): what persists there is a **narrowing** — filters a collector can lose half their
    // collection behind — and this shelf carries no chips at all, so an order and a search box are the
    // whole of it. `rememberSaveable` keeps both across a rotation, which is where they would be missed.
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
                // The door of «Lo que busco», at the head of the shelf and on deeper paper: the same
                // shape that brought the collector here from the index (ADR 0026 §8 clause 3), one
                // level further in. Absent at zero, like that one.
                if (wishes > 0) {
                    AnnexDoor(label = wishDoorLabel(wishes), onOpen = onOpenWishes)
                }
                Text(
                    ShowcaseLabels.SENTENCE,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Paper.muted,
                )
                // That browsing is free, said once for the whole shelf and not on twenty tiles.
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
                ShelfOrder(sort = sort, onSort = { sort = it })
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

/** The page margin, the calle and the card of the index: this shelf is the album's, in another order. */
private val SHELF_MARGIN = 12.dp
private val SHELF_GUTTER = 8.dp
private val SHELF_CARD_WIDTH = 104.dp

/**
 * One plate of the shelf, whichever population it came from.
 *
 * The card of the index (`CollectionCard`) with one thing added and one taken away: the ghost, because a
 * plate of the window holds no coin of the collector's — the fantasma of ADR 0026 is what says a casilla
 * is empty, and here the whole plate is — and no travelling coin, because nothing flies from a hole that
 * is not the same object at both ends of the journey (ADR 0026 §3).
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
            // A plate of the window is a plate with nothing in it, so every one of its tiles wears the
            // ghost; a plate of the collector's shows its cover coin as its own card does.
            missing = !tile.mine,
            // **No `otherSide`**, exactly as `CollectionCard` passes none: the coin of a *casilla* turns
            // over, and a tile is not a casilla — handed the other face, the hole takes the tap to turn
            // it and the plate behind the tile never opens. Measured on the AVD, where the whole shelf
            // was inert.
            modifier = Modifier.size(SHELF_CARD_WIDTH),
        )
        CollectionName(tile.name)
        Text(
            tile.footnote,
            style = MaterialTheme.typography.labelLarge,
            color = if (tile.mine) Paper.rust else Paper.moss,
            textAlign = TextAlign.Center,
        )
        // What put this plate of the collector's on a shelf of things they do not have. Lower case,
        // because it is the same note the chip in the hole is (ADR 0029 §5).
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
 * The two orders of the shelf: the one it is read in, and the other one offered beside it.
 *
 * `FilterShelf` is what a hierarchy with facets opens; this shelf has none — twenty plates at 0/N and
 * twelve countries buy no chip (ADR 0026 §8 clause 4) — so a disclosure that folded one row with two
 * words in it would be a fold with nothing folded.
 *
 * **The line says which order is on and the action says where it goes.** Drawn as the action alone it
 * read as a label of the current order — measured on the AVD, where «Por coste de entrar» sat over a
 * shelf sorted by casillas — and that is the one thing a control of this shape must not do.
 */
@Composable
private fun ShelfOrder(sort: ShowcaseSort, onSort: (ShowcaseSort) -> Unit) {
    val other = when (sort) {
        ShowcaseSort.ByCasillas -> ShowcaseSort.ByEntryCost
        ShowcaseSort.ByEntryCost -> ShowcaseSort.ByCasillas
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            sort.label,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
        )
        CardAction(text = other.label, onClick = { onSort(other) })
    }
}

