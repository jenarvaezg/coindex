package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.jenarvaezg.coindex.ui.COIN_IN_ONE_COLLECTION
import com.jenarvaezg.coindex.ui.COIN_IN_SEVERAL_COLLECTIONS
import com.jenarvaezg.coindex.ui.COIN_VIEW_ON_NUMISTA
import com.jenarvaezg.coindex.ui.CoinTap
import com.jenarvaezg.coindex.ui.coinFichaIdentity
import com.jenarvaezg.coindex.ui.coinTap
import com.jenarvaezg.coindex.ui.components.AlbumCartouche
import com.jenarvaezg.coindex.ui.components.AlbumChrome
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Facet
import com.jenarvaezg.coindex.ui.components.FichaBrought
import com.jenarvaezg.coindex.ui.components.FichaRefresh
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.FilterChip
import com.jenarvaezg.coindex.ui.components.FilterShelf
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PaperGrain
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.components.SelectionControls
import com.jenarvaezg.coindex.ui.components.rememberPieceSelection
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.objectClassChip
import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.shelf.CoinSort
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.GramBand
import com.jenarvaezg.coindex.ui.shelf.Membership
import com.jenarvaezg.coindex.ui.shelf.YearBand
import com.jenarvaezg.coindex.ui.shelf.coinAlbumFootnote
import com.jenarvaezg.coindex.ui.shelf.coinRows
import com.jenarvaezg.coindex.ui.shelf.coinsFacetCounts
import com.jenarvaezg.coindex.ui.shelf.coinsShelfSummary
import com.jenarvaezg.coindex.ui.shelf.coinsTally
import com.jenarvaezg.coindex.ui.shelf.issuers
import com.jenarvaezg.coindex.ui.shelf.narrow
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
@OptIn(ExperimentalMaterial3Api::class)
fun CoinsScreen(
    state: CollectionState,
    shelf: CoinsShelf,
    /** Every curated `short_name`, so a new box cannot be baptised one of them (ADR 0021 §4). */
    curatedNames: Set<String>,
    onNarrow: (CoinsShelf) -> Unit,
    onOpen: (CardDestination) -> Unit,
    onCreateBox: (name: String, typeIds: List<Int>) -> Unit,
    onAddToBox: (boxId: Long, typeIds: List<Int>) -> Unit,
    onOpenSource: (url: String) -> Unit,
    onSettings: () -> Unit,
    /**
     * How old each ficha is and how to ask for it again (#185, ADR 0025).
     *
     * This bottom sheet is the surface that has to carry it. A type whose ficha looks like an
     * unpublished draft derives no card at all (#186), so its pieces are only ever reachable from
     * here — and that is exactly the coin the issue was opened about: N#596807, whose family reads
     * «The» until a referee publishes the page and somebody on this side can ask again.
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

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().background(Paper.paper),
    ) {
        val columns = indexColumns(maxWidth)
        PaperGrain(Modifier.matchParentSize())
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
                        collections = state.index.size,
                        coins = state.items.sumOf { item -> item.quantity },
                        types = rows.size,
                        onSettings = onSettings,
                    )
                    SearchField(value = query, onValueChange = { query = it })
                    FilterShelf(
                        summary = coinsShelfSummary(shelf),
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
                val images = state.images[row.typeId]
                val photo = images?.reverse?.takeIf { it.hasPicture } ?: images?.obverse
                CoinAlbumCell(
                    row = row,
                    photo = photo,
                    picking = selection.active,
                    picked = selection.isPicked(row.typeId),
                    onTap = {
                        when (coinTap(selection.active)) {
                            CoinTap.ToggleSelection -> selection.toggle(row.typeId)
                            CoinTap.OpenFicha -> selectedTypeId = row.typeId
                        }
                    },
                )
            }
        }
    }

    rows.firstOrNull { it.typeId == selectedTypeId }?.let { selected ->
        ModalBottomSheet(
            onDismissRequest = { selectedTypeId = null },
            containerColor = Paper.paper,
            contentColor = Paper.ink,
            tonalElevation = 0.dp,
        ) {
            CoinFicha(
                row = selected,
                ficha = ficha(selected.typeId),
                onOpenSource = {
                    selectedTypeId = null
                    onOpenSource(
                        state.typeMeta[selected.typeId]?.numistaUrl ?: numistaTypeUrl(selected.typeId),
                    )
                },
                onOpen = { destination ->
                    selectedTypeId = null
                    onOpen(destination)
                },
            )
        }
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

    Facet("Orden") {
        CoinSort.entries.forEach { sort ->
            FilterChip(
                label = sort.label,
                count = null,
                selected = shelf.sort == sort,
                onClick = { onNarrow(shelf.copy(sort = sort)) },
            )
        }
    }
    Facet("País") {
        FilterChip(
            label = "Todos",
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
    Facet("Peso") {
        FilterChip(
            label = "Cualquiera",
            count = null,
            selected = shelf.weight == null,
            onClick = { onNarrow(shelf.copy(weight = null)) },
        )
        GramBand.entries.forEach { band ->
            FilterChip(
                label = band.label,
                count = counts.weight.of(band),
                selected = shelf.weight == band,
                onClick = { onNarrow(shelf.copy(weight = band)) },
            )
        }
    }
    Facet("Año") {
        FilterChip(
            label = "Cualquiera",
            count = null,
            selected = shelf.year == null,
            onClick = { onNarrow(shelf.copy(year = null)) },
        )
        // «Sin año» is one of these and not an extra: on the seeded cache it is the two unpublished
        // submissions of #186, the only coins whose ficha a referee still owes.
        YearBand.entries.forEach { band ->
            FilterChip(
                label = band.label,
                count = counts.year.of(band),
                selected = shelf.year == band,
                onClick = { onNarrow(shelf.copy(year = band)) },
            )
        }
    }
    Facet("Clase") {
        FilterChip(
            label = "Todo",
            count = null,
            selected = shelf.objectClass == null,
            onClick = { onNarrow(shelf.copy(objectClass = null)) },
        )
        ObjectClass.entries.forEach { value ->
            FilterChip(
                label = objectClassChip(value),
                count = counts.objectClass.of(value),
                selected = shelf.objectClass == value,
                onClick = { onNarrow(shelf.copy(objectClass = value)) },
            )
        }
    }
    Facet("Colección") {
        FilterChip(
            label = "Da igual",
            count = null,
            selected = shelf.membership == null,
            onClick = { onNarrow(shelf.copy(membership = null)) },
        )
        Membership.entries.forEach { value ->
            FilterChip(
                label = value.label,
                count = counts.membership.of(value),
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
            backed = row.unclaimedPieces == 0,
            modifier = Modifier.size(104.dp),
        )
        AlbumCartouche(row.name, modifier = Modifier.padding(top = 5.dp))
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

/** Exact identity and upkeep live inside the coin instead of being repeated under every hole. */
@Composable
private fun CoinFicha(
    row: CoinRow,
    ficha: FichaRefresh,
    onOpenSource: () -> Unit,
    onOpen: (CardDestination) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Text(
            row.rawTitle,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            coinFichaIdentity(row),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
        )
        FichaBrought(ficha, modifier = Modifier.padding(top = 8.dp))
        ExternalLink(
            text = COIN_VIEW_ON_NUMISTA,
            onClick = onOpenSource,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (row.claims.isNotEmpty()) {
            Text(
                if (row.claims.size == 1) COIN_IN_ONE_COLLECTION else COIN_IN_SEVERAL_COLLECTIONS,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
                modifier = Modifier.padding(top = 8.dp),
            )
            row.claims.forEach { claim ->
                LinkText(
                    text = claim.name,
                    style = MaterialTheme.typography.bodyLarge,
                    onClick = { onOpen(claim.destination) },
                    maxLines = 1,
                )
            }
        }
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
            if (everything) {
                "Todavía no hay monedas. Sincroniza para traer tu colección de Numista."
            } else {
                "Ninguna moneda pasa por lo que has puesto."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Paper.muted,
        )
        if (!everything) {
            CardAction(
                text = "Quitar los filtros",
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
