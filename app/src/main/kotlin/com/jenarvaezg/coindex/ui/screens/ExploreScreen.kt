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
                // The shelf **as shown**: the note under the orders counts what the collector is
                // looking at, so a search narrowed to three unvalued plates says three.
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
            absence = if (tile.mine) HoleAbsence.Filled else HoleAbsence.Missing,
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
 * The two orders of the shelf, both on the paper, and what the one in force could not place.
 *
 * `FilterShelf` is what a hierarchy with facets opens; this shelf has none — twenty plates at 0/N and
 * twelve countries buy no chip (ADR 0026 §8 clause 4) — so a disclosure that folded one row with two
 * words in it would be a fold with nothing folded.
 *
 * **Both orders are drawn, and the one in force is the filled one** (#513). It was a muted line beside
 * a framed `CardAction`, which is this album's convention upside down: the enclosed thing reads as the
 * chosen thing, so the shelf sorted by casillas announced itself as «Por coste de entrar» inside a
 * border. What it wears now is [FilterChip], the one drawing of «elegido» the album has — it says which
 * of a set is on without a sentence explaining it, and it says the same to a screen reader.
 *
 * The line under them is [showcaseOrderNote]: «por coste de entrar» sorts only what carries an amount
 * (ADR 0030 §8 clause 3), and a shelf where nothing has been valued does not visibly move when it is
 * pressed. Silence there is the one reading that leaves the collector believing the control is broken.
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
        // A FlowRow and not a Row: the two chips fit a 360 dp phone side by side and stop fitting at
        // the larger type sizes, where a Row would squeeze them against each other and each would
        // ellipse its own label ([FilterChip] is one line). Wrapping spends a line and lets each chip
        // ask for the width its words need.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ShowcaseSort.entries.forEach { order ->
                FilterChip(
                    label = order.label,
                    // No tally: what a filter chip counts is what it would leave, and an order leaves
                    // the whole shelf. The number beside it would be the same number twice.
                    count = null,
                    selected = order == sort,
                    onClick = { onSort(order) },
                    // The 30 dp of ink a chip is, and 48 dp of finger around it. Android's minimum
                    // bought the way the aspa of the search box and the year tag buy it — the target
                    // grows without the chip growing with it — because this row is one of the two
                    // controls of the whole screen and it is read with a thumb.
                    modifier = Modifier.minimumInteractiveComponentSize(),
                )
            }
        }
        showcaseOrderNote(sort, shelf)?.let { note ->
            Text(
                note,
                // The type of the line Ajustes prints under the pass and not the album's small caps:
                // this is a sentence explaining what the app did, and the versalitas of `labelMedium`
                // read as a rubric of the control above them — which is the register this line is
                // least able to afford.
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
            )
        }
    }
}

