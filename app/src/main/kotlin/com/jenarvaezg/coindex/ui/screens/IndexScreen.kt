package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionTitles
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.DerivedCollectionDisposition
import com.jenarvaezg.coindex.ui.BudgetStatus
import com.jenarvaezg.coindex.ui.DerivedCollectionStance
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.DispositionActions
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.issuerEyebrow
import com.jenarvaezg.coindex.ui.lastSyncLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import com.jenarvaezg.coindex.ui.variantLabel

/** Narrower than this a card cannot hold its own action row on one line. */
private val MIN_CARD_WIDTH = 340.dp

private val PAGE_MARGIN = 20.dp

/** How many cards fit side by side, counted from the page rather than from the device. */
internal fun indexColumns(availableWidth: Dp): Int {
    val usable = availableWidth - PAGE_MARGIN * 2 + PlateMetrics.gutter
    val perColumn = MIN_CARD_WIDTH + PlateMetrics.gutter
    return (usable / perColumn).toInt().coerceAtLeast(1)
}

/**
 * The collection index: one card per current collection, in three blocks.
 *
 * Collections are derived from the pieces the collector owns right now. Following one
 * never invents a gap. Every title opens its card, catalog or no catalog: the plate and the
 * source moved into that screen, because a title that opens something only when a curated
 * catalog happens to exist is a title that looks broken the rest of the time.
 *
 * The cards are laid out as a grid rather than a column. In portrait that is the same single
 * column it always was; held sideways, or on a tablet, the second column is free — and the
 * heading folds into two so the first card is not pushed below the fold by it.
 */
@Composable
fun IndexScreen(
    state: CollectionState,
    budget: BudgetStatus,
    loading: Boolean,
    syncing: Boolean,
    lastSync: SyncRecord?,
    catalogs: List<CollectionCatalog>,
    titles: CollectionTitles,
    onSync: () -> Unit,
    onOpenUnclassified: () -> Unit,
    onOpenDerivedCollection: (DerivedCollection) -> Unit,
    onOpenOwnGrouping: (groupingId: Long) -> Unit,
    onOpenPlate: (catalogId: String) -> Unit,
    onDisposition: (DerivedCollection, DerivedCollectionDisposition?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showIgnored by remember { mutableStateOf(false) }
    val derivedCollections = state.derivedCollections

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Counted here rather than left to GridCells.Adaptive, because the heading needs the
        // same answer: one column is a page, two are a spread.
        val columns = indexColumns(maxWidth)

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = PAGE_MARGIN, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
            verticalArrangement = Arrangement.spacedBy(PlateMetrics.cardStack),
        ) {
            fullWidth {
                IndexHeading(
                    unclassified = state.unclassified.size,
                    budget = budget,
                    syncing = syncing,
                    lastSync = lastSync,
                    spread = columns > 1,
                    onSync = onSync,
                    onOpenUnclassified = onOpenUnclassified,
                )
            }

            // An incomplete sync outlives its snackbar: what it left half-done is a property of
            // the collection on screen, not a notice about something four seconds old.
            lastSync?.partialFailure?.let { failure ->
                fullWidth {
                    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                        Eyebrow("Sincronización incompleta")
                        Text(
                            "La última sincronización no terminó, así que puede faltar alguna " +
                                "pieza o ficha. Vuelve a sincronizar cuando puedas.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            failure,
                            style = MaterialTheme.typography.labelLarge,
                            color = Paper.rust,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            // Reading the collection off the database takes a frame or two, and «todavía no hay
            // colecciones» in that gap is a lie about a collection that is on the device already.
            if (derivedCollections.isEmpty && state.ownGroupings.isEmpty()) {
                fullWidth {
                    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (loading) {
                                "Leyendo tu colección…"
                            } else {
                                "Todavía no hay colecciones. Sincroniza para traer " +
                                    "tu colección de Numista."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Paper.muted,
                        )
                    }
                }
            }

            // The collector's own headings come first: they are the only ones nobody derived.
            if (state.ownGroupings.isNotEmpty()) {
                blockHeading("Tus agrupaciones")
                items(state.ownGroupings, key = { "own-${it.id}" }) { grouping ->
                    FieldCard(modifier = Modifier.fillMaxWidth()) {
                        Eyebrow("Agrupación tuya")
                        LinkText(
                            text = grouping.name,
                            style = MaterialTheme.typography.titleLarge,
                            onClick = { onOpenOwnGrouping(grouping.id) },
                        )
                        Text(
                            countLabel(grouping.distinctTypes, grouping.quantity),
                            style = MaterialTheme.typography.labelLarge,
                            color = Paper.muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            derivedCollectionBlock(
                title = "Seguidas",
                derivedCollections = derivedCollections.followed,
                stance = DerivedCollectionStance.Followed,
                state = state,
                catalogs = catalogs,
                titles = titles,
                onOpenDerivedCollection = onOpenDerivedCollection,
                onOpenPlate = onOpenPlate,
                onDisposition = onDisposition,
            )
            derivedCollectionBlock(
                title = "Disponibles",
                derivedCollections = derivedCollections.available,
                stance = DerivedCollectionStance.Available,
                state = state,
                catalogs = catalogs,
                titles = titles,
                onOpenDerivedCollection = onOpenDerivedCollection,
                onOpenPlate = onOpenPlate,
                onDisposition = onDisposition,
            )

            if (derivedCollections.ignored.isNotEmpty()) {
                fullWidth {
                    // In a Row so the button keeps its own width: a grid cell measures its
                    // content at the full column width, and a button as wide as the page reads
                    // as a section rather than as a control.
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CardAction(
                            // The arrow is what says whether the list is open: without it the
                            // same button read as «show them» in both states.
                            text = if (showIgnored) {
                                "▾ Ocultar las ignoradas · ${derivedCollections.ignored.size}"
                            } else {
                                "▸ Colecciones ignoradas · ${derivedCollections.ignored.size}"
                            },
                            onClick = { showIgnored = !showIgnored },
                        )
                    }
                }
                if (showIgnored) {
                    items(derivedCollections.ignored, key = { it.key().toString() }) { collection ->
                        DerivedCollectionCard(
                            collection = collection,
                            stance = DerivedCollectionStance.Ignored,
                            title = titles.of(collection),
                            catalog = catalogs.firstOrNull { it.key() == collection.key() },
                            state = state,
                            onOpenDerivedCollection = onOpenDerivedCollection,
                            onOpenPlate = onOpenPlate,
                            onDisposition = onDisposition,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The masthead of the page: what this screen is, what it can do, and where the budget stands.
 *
 * [spread] lays the two halves side by side. Stacked they are around 300dp of heading, which on
 * a phone held sideways is the whole viewport — the review found the index looking empty while
 * holding twenty cards.
 */
@Composable
private fun IndexHeading(
    unclassified: Int,
    budget: BudgetStatus,
    syncing: Boolean,
    lastSync: SyncRecord?,
    spread: Boolean,
    onSync: () -> Unit,
    onOpenUnclassified: () -> Unit,
) {
    val title = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow("Cuaderno de colección")
            Text("Láminas de plata", style = MaterialTheme.typography.displayLarge)
            Text(
                "Colecciones a partir de las piezas que tienes ahora mismo.",
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.muted,
            )
        }
    }
    val actions = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryAction(
                    text = if (syncing) "Sincronizando…" else "Sincronizar",
                    onClick = onSync,
                    enabled = !syncing,
                )
                CardAction(
                    text = "Sin clasificar · $unclassified",
                    onClick = onOpenUnclassified,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    lastSync?.let { lastSyncLabel(it, System.currentTimeMillis()) }
                        ?: "Todavía no has sincronizado con Numista.",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                )
                Text(
                    "Presupuesto de la API: ${budget.used} / ${budget.cap} llamadas este mes",
                    style = MaterialTheme.typography.labelLarge,
                    color = Paper.muted,
                )
            }
        }
    }

    if (spread) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) { title() }
            Column(modifier = Modifier.weight(1f)) { actions() }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            title()
            actions()
        }
    }
}

/** A row of the page rather than a card of the grid: headings and notices span every column. */
private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.blockHeading(title: String) {
    fullWidth {
        Column {
            HorizontalDivider(color = Paper.line)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun LazyGridScope.derivedCollectionBlock(
    title: String,
    derivedCollections: List<DerivedCollection>,
    stance: DerivedCollectionStance,
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    titles: CollectionTitles,
    onOpenDerivedCollection: (DerivedCollection) -> Unit,
    onOpenPlate: (String) -> Unit,
    onDisposition: (DerivedCollection, DerivedCollectionDisposition?) -> Unit,
) {
    if (derivedCollections.isEmpty()) return
    blockHeading(title)
    items(derivedCollections, key = { "$title-${it.key()}" }) { collection ->
        DerivedCollectionCard(
            collection = collection,
            stance = stance,
            title = titles.of(collection),
            catalog = catalogs.firstOrNull { it.key() == collection.key() },
            state = state,
            onOpenDerivedCollection = onOpenDerivedCollection,
            onOpenPlate = onOpenPlate,
            onDisposition = onDisposition,
        )
    }
}

@Composable
private fun DerivedCollectionCard(
    collection: DerivedCollection,
    stance: DerivedCollectionStance,
    title: String,
    catalog: CollectionCatalog?,
    state: CollectionState,
    onOpenDerivedCollection: (DerivedCollection) -> Unit,
    onOpenPlate: (String) -> Unit,
    onDisposition: (DerivedCollection, DerivedCollectionDisposition?) -> Unit,
) {
    // The plate keeps its shortcut from the card, but as an action rather than as the title:
    // the same conditions resolvePlate applies, checked here so a dead button is never drawn.
    val plateCatalog = catalog?.takeIf {
        stance == DerivedCollectionStance.Followed && it.id in state.evidencedCatalogIds
    }
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        // The issuer, where every card used to repeat «EVIDENCIA DE COLECCIÓN» — which is what
        // the section heading right above it already says.
        issuerEyebrow(state.itemsByKey[collection.key()].orEmpty(), state.typeMeta)?.let { issuer ->
            Eyebrow(issuer)
        }
        LinkText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            onClick = { onOpenDerivedCollection(collection) },
        )
        Text(
            variantLabel(collection.weightMillioz, collection.finish, collection.metal),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            countLabel(collection.distinctTypes, collection.quantity),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        DispositionActions(
            stance = stance,
            onDisposition = { disposition -> onDisposition(collection, disposition) },
            onOpenPlate = plateCatalog?.let { curated -> { onOpenPlate(curated.id) } },
        )
    }
}
