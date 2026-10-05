package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.ui.SewnEdgeCounts
import com.jenarvaezg.coindex.ui.coinAlbumFaces
import com.jenarvaezg.coindex.ui.components.AlbumCartouche
import com.jenarvaezg.coindex.ui.components.AlbumChrome
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Facet
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.FilterChip
import com.jenarvaezg.coindex.ui.components.FilterShelf
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.components.SelectionBand
import com.jenarvaezg.coindex.ui.components.SelectionDoor
import com.jenarvaezg.coindex.ui.components.rememberPieceSelection
import com.jenarvaezg.coindex.ui.components.sheetUnderMode
import com.jenarvaezg.coindex.ui.components.travellingTypeCoin
import com.jenarvaezg.coindex.ui.objectClassChip
import com.jenarvaezg.coindex.ui.shelf.ANY_FILTER
import com.jenarvaezg.coindex.ui.shelf.AXIS_FACET
import com.jenarvaezg.coindex.ui.shelf.CLASS_FACET
import com.jenarvaezg.coindex.ui.shelf.COINS_SEARCH_PLACEHOLDER
import com.jenarvaezg.coindex.ui.shelf.COUNTRY_FACET
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.shelf.CoinSort
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.GramBand
import com.jenarvaezg.coindex.ui.shelf.MEMBERSHIP_FACET
import com.jenarvaezg.coindex.ui.shelf.Membership
import com.jenarvaezg.coindex.ui.shelf.NotebookAxis
import com.jenarvaezg.coindex.ui.shelf.SORT_FACET
import com.jenarvaezg.coindex.ui.shelf.ShelfNarrowing
import com.jenarvaezg.coindex.ui.shelf.WEIGHT_FACET
import com.jenarvaezg.coindex.ui.shelf.YEAR_FACET
import com.jenarvaezg.coindex.ui.shelf.YearFilter
import com.jenarvaezg.coindex.ui.shelf.clearNarrowingAction
import com.jenarvaezg.coindex.ui.shelf.coinAlbumFootnote
import com.jenarvaezg.coindex.ui.shelf.coinRows
import com.jenarvaezg.coindex.ui.shelf.coinsEmptyLabel
import com.jenarvaezg.coindex.ui.shelf.coinsFacetCounts
import com.jenarvaezg.coindex.ui.shelf.coinsShelfSummary
import com.jenarvaezg.coindex.ui.shelf.coinsTally
import com.jenarvaezg.coindex.ui.shelf.issuers
import com.jenarvaezg.coindex.ui.shelf.narrow
import com.jenarvaezg.coindex.ui.shelf.shelfNarrowing
import com.jenarvaezg.coindex.ui.shelf.slotYears
import com.jenarvaezg.coindex.ui.shelf.years
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Monedas: every piece, whether or not a collection claims it (ADR 0021 §1). Unclaimed coins are
 * its «Sin colección» chip rather than a screen of their own.
 *
 * It says which coins no collection claims but never why (§12): the per-piece reasons of
 * ADR 0010 §3 live in the field report, and corrections go through the curator.
 */
@Composable
fun CoinsScreen(
    state: CollectionState,
    shelf: CoinsShelf,
    /** Every curated `short_name`, so a new box cannot be baptised one of them (ADR 0021 §4). */
    curatedNames: Set<String>,
    onNarrow: (CoinsShelf) -> Unit,
    onCreateBox: (name: String, typeIds: List<Int>) -> Unit,
    onAddToBox: (boxId: Long, typeIds: List<Int>) -> Unit,
    /** Computed once above the three roots so they all show the same counts. */
    sewnEdge: SewnEdgeCounts?,
    onOpenPhone: () -> Unit,
    /**
     * The coin sheet (#508), which holds the ficha's upkeep (#185, ADR 0025). Pieces whose ficha
     * derives no card (an unpublished draft, #186, or a placeholder family, #404) are reachable
     * only from here, so this is where their ficha gets asked for again.
     */
    sheet: CoinSheetSurface,
    modifier: Modifier = Modifier,
) {
    // Only when the collection changes, not per keystroke. A row also matches on the years of the
    // casillas it fills (#550).
    val rows = remember(state) { coinRows(state, slotYears(state)) }
    // Survives rotation but is never persisted (ADR 0021 §1): a stale word on reopening would hide
    // half the collection.
    var query by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    var selectedTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
    val shown = remember(rows, shelf, query) { shelf.narrow(rows, query) }
    val selection = rememberPieceSelection()
    // Seeding a box from the shown coins only makes sense while something narrows the list;
    // otherwise it would offer the whole collection.
    val seeded = shelf.active > 0 || query.isNotBlank()
    // What the empty card names and undoes (#515).
    val narrowing = shelfNarrowing(filters = shelf.active, query = query)
    val taken = remember(curatedNames, state.ownGroupings) {
        curatedNames + state.ownGroupings.map { it.name }
    }
    Box(modifier = modifier.fillMaxSize()) {
        // The selection band is a row of this column, not an overlay, so it never covers the last
        // row of coins (#517).
        Column(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.weight(1f).sheetUnderMode(selection.active)) {
                val columns = indexColumns(maxWidth)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    // See [CoinsSpacing].
                    verticalArrangement = Arrangement.spacedBy(CoinsSpacing.rowSeam),
                ) {
                    coinFullWidth {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AlbumChrome(
                                counts = sewnEdge,
                                onOpenPhone = onOpenPhone,
                            )
                            SearchField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = COINS_SEARCH_PLACEHOLDER,
                            )
                            FilterShelf(
                                summary = coinsShelfSummary(shelf, expanded = open),
                                tally = coinsTally(shown.size, rows.size),
                                expanded = open,
                                onToggle = { open = !open },
                            ) {
                                CoinsFacets(rows = rows, shelf = shelf, query = query, onNarrow = onNarrow)
                            }
                            if (rows.isNotEmpty()) {
                                SelectionDoor(
                                    selection = selection,
                                    shown = shown.map { it.typeId },
                                    seeded = seeded,
                                )
                            }
                        }
                    }

                    if (shown.isEmpty()) {
                        coinFullWidth {
                            EmptyCoins(
                                everything = rows.isEmpty(),
                                narrowing = narrowing,
                                // Undo only what narrows (#515): filters but not axis or sort, and
                                // the search box only if it has text.
                                onClear = {
                                    if (narrowing != ShelfNarrowing.Search) {
                                        onNarrow(shelf.withoutFilters())
                                    }
                                    if (narrowing != ShelfNarrowing.Filters) query = ""
                                },
                            )
                        }
                    }

                    items(shown, key = { it.typeId }) { row ->
                        val (photo, _) = coinAlbumFaces(state.images[row.typeId])
                        CoinAlbumCell(
                            row = row,
                            photo = photo,
                            // The cell yields the coin while its sheet is open, so the shared
                            // element has one owner at a time (#370).
                            travelling = selectedTypeId != row.typeId,
                            picking = selection.active,
                            picked = selection.isPicked(row.typeId),
                            onTap = {
                                if (selection.active) selection.toggle(row.typeId)
                                else selectedTypeId = row.typeId
                            },
                        )
                    }
                }
            }
            SelectionBand(
                selection = selection,
                existing = state.ownGroupings,
                taken = taken,
                shown = shown.map { it.typeId },
                seeded = seeded,
                onCreate = onCreateBox,
                onAddTo = onAddToBox,
            )
        }

        // Here the sheet is also the far end of ADR 0026 §3's transition from the cell.
        CoinSheetOverlay(
            typeId = selectedTypeId,
            surface = sheet,
            faces = { typeId -> coinAlbumFaces(state.images[typeId]) },
            onDismiss = { selectedTypeId = null },
            travelling = true,
        )
    }
}

/**
 * The chip rows, each counted with its own choice dropped. «Colección» goes last because it is
 * about the rest of the app rather than the coin.
 */
@Composable
private fun CoinsFacets(
    rows: List<CoinRow>,
    shelf: CoinsShelf,
    query: String,
    onNarrow: (CoinsShelf) -> Unit,
) {
    val counts = coinsFacetCounts(rows, shelf, query)

    Facet(AXIS_FACET) {
        NotebookAxis.entries.forEach { axis ->
            FilterChip(
                label = axis.label,
                count = null,
                selected = shelf.axis == axis,
                onClick = { onNarrow(shelf.copy(axis = axis)) },
            )
        }
    }
    Facet(SORT_FACET) {
        CoinSort.entries.forEach { sort ->
            FilterChip(
                label = sort.label,
                count = null,
                selected = shelf.sort == sort,
                onClick = { onNarrow(shelf.copy(sort = sort)) },
            )
        }
    }
    Facet(COUNTRY_FACET) {
        FilterChip(
            label = ANY_FILTER,
            count = null,
            selected = shelf.issuer == null,
            onClick = { onNarrow(shelf.copy(issuer = null)) },
        )
        counts.issuer.issuers().forEach { (issuer, count) ->
            FilterChip(
                label = issuer,
                count = count,
                selected = shelf.issuer == issuer,
                onClick = { onNarrow(shelf.copy(issuer = issuer)) },
            )
        }
    }
    Facet(WEIGHT_FACET) {
        FilterChip(
            label = ANY_FILTER,
            count = null,
            selected = shelf.weight == null,
            onClick = { onNarrow(shelf.copy(weight = null)) },
        )
        counts.weight.populatedIn(GramBand.entries, keep = shelf.weight).forEach { (band, count) ->
            FilterChip(
                label = band.label,
                count = count,
                selected = shelf.weight == band,
                onClick = { onNarrow(shelf.copy(weight = band)) },
            )
        }
    }
    Facet(YEAR_FACET) {
        FilterChip(
            label = ANY_FILTER,
            count = null,
            selected = shelf.year == null,
            onClick = { onNarrow(shelf.copy(year = null)) },
        )
        // Exact years, newest first, so a chip and the year axis count the same. «Sin año» appears
        // when a ficha has no date.
        counts.year.years().forEach { (filter, count) ->
            FilterChip(
                label = filter.label,
                count = count,
                selected = shelf.year == filter,
                onClick = { onNarrow(shelf.copy(year = filter)) },
            )
        }
    }
    Facet(CLASS_FACET) {
        FilterChip(
            label = ANY_FILTER,
            count = null,
            selected = shelf.objectClass == null,
            onClick = { onNarrow(shelf.copy(objectClass = null)) },
        )
        counts.objectClass.populatedIn(ObjectClass.entries, keep = shelf.objectClass).forEach { (value, count) ->
            FilterChip(
                label = objectClassChip(value),
                count = count,
                selected = shelf.objectClass == value,
                onClick = { onNarrow(shelf.copy(objectClass = value)) },
            )
        }
    }
    Facet(MEMBERSHIP_FACET) {
        FilterChip(
            label = ANY_FILTER,
            count = null,
            selected = shelf.membership == null,
            onClick = { onNarrow(shelf.copy(membership = null)) },
        )
        counts.membership.populatedIn(Membership.entries, keep = shelf.membership).forEach { (value, count) ->
            FilterChip(
                label = value.label,
                count = count,
                selected = shelf.membership == value,
                onClick = { onNarrow(shelf.copy(membership = value)) },
            )
        }
    }
}

@Composable
private fun CoinAlbumCell(
    row: CoinRow,
    photo: CoinPhoto?,
    travelling: Boolean,
    picking: Boolean,
    picked: Boolean,
    onTap: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .then(pickingBorder(picking = picking, picked = picked))
            .semantics(mergeDescendants = true) { selected = picking && picked }
            .clickable(role = Role.Button, onClick = onTap)
            .padding(bottom = CoinsSpacing.cardFoot),
    ) {
        AlbumHole(
            photo = photo,
            backed = row.claims.isNotEmpty(),
            modifier = Modifier
                .size(104.dp)
                .travellingTypeCoin(row.typeId, visible = travelling),
        )
        AlbumCartouche(row.name, modifier = Modifier.padding(top = 5.dp))
        // [CoinsSpacing] keeps the year reading as this card's, not the next row's.
        Text(
            coinAlbumFootnote(row),
            style = MaterialTheme.typography.labelMedium,
            color = Paper.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = CoinsSpacing.underTheCartouche),
        )
    }
}

/**
 * A card's frame while selecting: solid once picked, faint until then, none outside the mode
 * (#517). The faint frame marks which cards the selection band is talking about.
 */
private fun pickingBorder(picking: Boolean, picked: Boolean): Modifier = when {
    picked -> Modifier.border(PICK_RULE, Paper.rust)
    picking -> Modifier.border(PICK_RULE, Paper.rust.copy(alpha = GHOST_PICK_OPACITY))
    else -> Modifier
}

private val PICK_RULE = 2.dp

/** Faint enough to read as an empty box, dark enough to show over the paper grain. */
private const val GHOST_PICK_OPACITY = 0.3f

/**
 * The empty state. When a narrowing hides everything it offers the undo right here, since the
 * folded shelf may hide the responsible chip; the action is named after what is narrowing (#515).
 */
@Composable
private fun EmptyCoins(everything: Boolean, narrowing: ShelfNarrowing, onClear: () -> Unit) {
    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            coinsEmptyLabel(anyCoins = !everything, narrowing = narrowing),
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        clearNarrowingAction(narrowing).takeIf { !everything }?.let { action ->
            CardAction(
                text = action,
                onClick = onClear,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** A shelf or empty state spans the album page rather than occupying one coin slot. */
private fun LazyGridScope.coinFullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}
