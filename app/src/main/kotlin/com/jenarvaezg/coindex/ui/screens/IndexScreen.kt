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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.ui.BudgetStatus
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.coverageLabel
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
 * The collection index: one card per current collection, in **one list and one order**.
 *
 * There is one species of collection (ADR 0021 §2) — a curated catalog, a curated grouping and a
 * box the collector typed — so there is no block, no section heading and no word of provenance
 * telling them apart. What replaced the three blocks of dispositions is a single comparator,
 * `(has ratio ↓, ratio ↓, denominator ↓, name ↑)` (ADR 0021 §6), applied in the domain: this
 * screen draws [CollectionState.index] in the order it arrives.
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
    onSync: () -> Unit,
    onOpenUnclassified: () -> Unit,
    onOpenDerivedCollection: (DerivedCollection) -> Unit,
    onOpenOwnGrouping: (groupingId: Long) -> Unit,
    onOpenPlate: (catalogId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            if (state.index.isEmpty()) {
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

            items(state.index, key = ::cardKey) { card ->
                CollectionCard(
                    card = card,
                    onOpen = {
                        when (card) {
                            is IndexCard.Derived -> onOpenDerivedCollection(card.collection)
                            is IndexCard.Box -> onOpenOwnGrouping(card.box.id)
                        }
                    },
                    onOpenPlate = onOpenPlate,
                )
            }
        }
    }
}

/**
 * What tells one card of the grid from another across recompositions.
 *
 * The identity of a card is its curated file wherever there is one (ADR 0021 §5), but a route is
 * still what opens it, so the key here is what the destination is addressed by: the box id, or the
 * variant key of a collection derived from the inventory.
 */
private fun cardKey(card: IndexCard): String = when (card) {
    is IndexCard.Derived -> "derived-${card.key}"
    is IndexCard.Box -> "box-${card.box.id}"
}

/**
 * One collection of the index, whichever of the two it is.
 *
 * **One composable and not two**, because ADR 0021 §2 makes the cases indistinguishable on screen:
 * two of them would drift apart the first time one grew a line. What varies is drawn from what the
 * card *has* — a physical variant, a ratio, a reachable plate — never from which case it is.
 *
 * The eyebrow is the country, said by the file wherever a file names this collection (ADR 0021 §9).
 * There is no word of provenance: what the card does is the only signal, and the third line is it —
 * `4 de 12 · te faltan 8` with an issue list, `3 tipos distintos · 4 piezas` without one.
 */
@Composable
private fun CollectionCard(
    card: IndexCard,
    onOpen: () -> Unit,
    onOpenPlate: (catalogId: String) -> Unit,
) {
    val derived = card as? IndexCard.Derived
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        card.issuer?.let { issuer -> Eyebrow(issuer) }
        LinkText(
            text = card.name,
            style = MaterialTheme.typography.titleLarge,
            onClick = onOpen,
        )
        // A box spans whatever the collector put in it, so it has no physical variant to state.
        derived?.collection?.let { collection ->
            Text(
                variantLabel(collection.weightMillioz, collection.finish, collection.metal),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            card.coverage?.let(::coverageLabel) ?: countLabel(card.distinctTypes, card.quantity),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        // The plate keeps its shortcut from the card, but as an action rather than as the title.
        // It is drawn only when it opens: the card already carries the answer resolvePlate would
        // give, so a dead button is never drawn — and since ADR 0021 §7 curating a catalog over
        // something the collector already owns is enough to light it.
        derived?.plateCatalogId?.let { catalogId ->
            CardAction(
                text = "Ver lámina",
                onClick = { onOpenPlate(catalogId) },
                modifier = Modifier.padding(top = 12.dp),
            )
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
