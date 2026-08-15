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
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinValue
import com.jenarvaezg.coindex.ui.SewnEdgeCounts
import com.jenarvaezg.coindex.ui.coinAlbumFaces
import com.jenarvaezg.coindex.ui.components.AlbumCartouche
import com.jenarvaezg.coindex.ui.components.AlbumChrome
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Facet
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.FilterChip
import com.jenarvaezg.coindex.ui.components.FilterShelf
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.components.SelectionControls
import com.jenarvaezg.coindex.ui.components.rememberPieceSelection
import com.jenarvaezg.coindex.ui.components.travellingTypeCoin
import com.jenarvaezg.coindex.ui.objectClassChip
import com.jenarvaezg.coindex.ui.shelf.ANY_FILTER
import com.jenarvaezg.coindex.ui.shelf.AXIS_FACET
import com.jenarvaezg.coindex.ui.shelf.CLASS_FACET
import com.jenarvaezg.coindex.ui.shelf.CLEAR_FILTERS_ACTION
import com.jenarvaezg.coindex.ui.shelf.COUNTRY_FACET
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.shelf.CoinSort
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.GramBand
import com.jenarvaezg.coindex.ui.shelf.MEMBERSHIP_FACET
import com.jenarvaezg.coindex.ui.shelf.Membership
import com.jenarvaezg.coindex.ui.shelf.NotebookAxis
import com.jenarvaezg.coindex.ui.shelf.SORT_FACET
import com.jenarvaezg.coindex.ui.shelf.WEIGHT_FACET
import com.jenarvaezg.coindex.ui.shelf.YEAR_FACET
import com.jenarvaezg.coindex.ui.shelf.YearFilter
import com.jenarvaezg.coindex.ui.shelf.coinAlbumFootnote
import com.jenarvaezg.coindex.ui.shelf.coinRows
import com.jenarvaezg.coindex.ui.shelf.coinsEmptyLabel
import com.jenarvaezg.coindex.ui.shelf.coinsFacetCounts
import com.jenarvaezg.coindex.ui.shelf.coinsShelfSummary
import com.jenarvaezg.coindex.ui.shelf.coinsTally
import com.jenarvaezg.coindex.ui.shelf.issuers
import com.jenarvaezg.coindex.ui.shelf.narrow
import com.jenarvaezg.coindex.ui.shelf.years
import com.jenarvaezg.coindex.ui.theme.Paper

/**
 * Coins: the other hierarchy of the top level (ADR 0021 §1).
 *
 * **Not a view inside a collection and not a reading of the index.** A piece exists here whether or
 * not any collection claims it, which is what retired «Sin clasificar» as a screen: those coins are
 * the «Sin colección» chip of this list, reached from where they already live instead of from a
 * screen that existed to apologise for them.
 *
 * What it deliberately does **not** carry is a reason line (§12). The four per-piece reasons of
 * ADR 0010 §3 left the app for the field report: this screen answers *which* coins no collection
 * claims, never *why*, because the collector's channel for correcting the matching is telling the
 * curator, and that already works.
 */
@Composable
fun CoinsScreen(
    state: CollectionState,
    shelf: CoinsShelf,
    /** Every curated `short_name`, so a new box cannot be baptised one of them (ADR 0021 §4). */
    curatedNames: Set<String>,
    onNarrow: (CoinsShelf) -> Unit,
    onOpen: (CardDestination) -> Unit,
    onCreateBox: (name: String, typeIds: List<Int>) -> Unit,
    onAddToBox: (boxId: Long, typeIds: List<Int>) -> Unit,
    /** The one way out to the browser here, behind the sheet's «Ver en Numista» and its arrow (#508). */
    onOpenNumista: (typeId: Int) -> Unit,
    /** The sewn-edge census, assembled once above the three roots so this screen cannot invent its own. */
    sewnEdge: SewnEdgeCounts?,
    onSettings: () -> Unit,
    /**
     * What one coin is worth, or null while the market has not landed (ADR 0028 §7).
     *
     * Handed in the way [ficha] is, and built in the same place: the value of a piece also heads its
     * plate, and two screens computing it apart is two totals that can disagree about one coin.
     */
    value: (Int) -> CoinValue?,
    /**
     * How old each ficha is and how to ask for it again (#185, ADR 0025).
     *
     * This bottom sheet is the surface that has to carry it. A type whose ficha looks like an
     * unpublished draft derives no card at all (#186), and neither does one whose family is a
     * half-typed «The» (#404), so their pieces are only ever reachable from here — which is
     * exactly the coin the issue was opened about: N#596807, whose page Numista has since
     * published with its `series` still cut after the article. The fix is upstream, and it reaches
     * this phone only when somebody asks for the ficha again on this very sheet.
     */
    ficha: (typeId: Int) -> FichaRefresh,
    modifier: Modifier = Modifier,
) {
    // Recomputed only when the collection changes: 192 rows joined against the type cache is work
    // the screen does not owe on every keystroke.
    val rows = remember(state) { coinRows(state) }
    // Saved across a rotation and never persisted (ADR 0021 §1): reopening the app with a stale
    // word here and half the collection hidden reads as an app that has lost something.
    var query by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    var selectedTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
    val shown = remember(rows, shelf, query) { shelf.narrow(rows, query) }
    val selection = rememberPieceSelection()
    // The seed exists only while something is narrowing the list: without a filter «Agrupar estas
    // 192» would offer the whole collection, and the two-coin box would be made by unticking 190.
    val seeded = shelf.active > 0 || query.isNotBlank()
    val taken = remember(curatedNames, state.ownGroupings) {
        curatedNames + state.ownGroupings.map { it.name }
    }
    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = indexColumns(maxWidth)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                // The two-range cartouche itself pays the measured height cost. Reusing the album's
                // 6 dp row seam keeps that cost from being paid a second time as empty cardboard.
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                coinFullWidth {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AlbumChrome(
                            counts = sewnEdge,
                            onSettings = onSettings,
                        )
                        SearchField(value = query, onValueChange = { query = it })
                        FilterShelf(
                            summary = coinsShelfSummary(shelf, expanded = open),
                            tally = coinsTally(shown.size, rows.size),
                            expanded = open,
                            onToggle = { open = !open },
                        ) {
                            CoinsFacets(rows = rows, shelf = shelf, query = query, onNarrow = onNarrow)
                        }
                        if (rows.isNotEmpty()) {
                            SelectionControls(
                                selection = selection,
                                existing = state.ownGroupings,
                                taken = taken,
                                shown = shown.map { it.typeId },
                                seeded = seeded,
                                onCreate = onCreateBox,
                                onAddTo = onAddToBox,
                            )
                        }
                    }
                }

                if (shown.isEmpty()) {
                    coinFullWidth {
                        EmptyCoins(
                            everything = rows.isEmpty(),
                            onClear = { onNarrow(CoinsShelf()); query = "" },
                        )
                    }
                }

                items(shown, key = { it.typeId }) { row ->
                    val (photo, _) = coinAlbumFaces(state.images[row.typeId])
                    CoinAlbumCell(
                        row = row,
                        photo = photo,
                        // The cell yields the coin while its ficha is open, so the shared element
                        // has one owner at a time (#370).
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

        // The one sheet of a coin, which every surface of the app now opens (#508). Here it is also
        // the second end of the journey of ADR 0026 §3: the cell yields its photograph on the way in.
        CoinSheetOverlay(
            typeId = selectedTypeId,
            coin = { typeId -> rows.firstOrNull { it.typeId == typeId } },
            faces = { typeId -> coinAlbumFaces(state.images[typeId]) },
            ficha = ficha,
            value = value,
            onDismiss = { selectedTypeId = null },
            onOpenNumista = onOpenNumista,
            onOpenClaim = onOpen,
            travelling = true,
        )
    }
}

/**
 * The five chip rows, each counted with its own choice dropped.
 *
 * Country goes first because it is the one the collector reaches for, and «colección» last because it
 * is the one that answers a question about the rest of the app rather than about the coin.
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
        // Exact years, newest first — same shape as País, so a seat on the year axis and a chip
        // here say the same number. «Sin año» is one of these when the ficha still owes a date.
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
            .then(if (picked) Modifier.border(2.dp, Paper.rust) else Modifier)
            .semantics(mergeDescendants = true) { selected = picking && picked }
            .clickable(role = Role.Button, onClick = onTap)
            .padding(bottom = 4.dp),
    ) {
        AlbumHole(
            photo = photo,
            backed = row.claims.isNotEmpty(),
            modifier = Modifier
                .size(104.dp)
                .travellingTypeCoin(row.typeId, visible = travelling),
        )
        AlbumCartouche(row.name, modifier = Modifier.padding(top = 5.dp))
        // The year stays outside this cartouche; #337 owns its separate rendering change.
        Text(
            coinAlbumFootnote(row),
            style = MaterialTheme.typography.labelMedium,
            color = Paper.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/**
 * Nothing to show, and which of the two reasons it is.
 *
 * A filter that hides everything must offer the way out on the spot: the shelf enters folded, so the
 * chip responsible may be two taps away, and «0 de 191» with no action is where an app looks broken.
 */
@Composable
private fun EmptyCoins(everything: Boolean, onClear: () -> Unit) {
    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            coinsEmptyLabel(anyCoins = !everything),
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        if (!everything) {
            CardAction(
                text = CLEAR_FILTERS_ACTION,
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
