package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.ui.BudgetStatus
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.Facet
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.FilterChip
import com.jenarvaezg.coindex.ui.components.FilterShelf
import com.jenarvaezg.coindex.ui.components.LinkText
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.coverageLabel
import com.jenarvaezg.coindex.ui.lastSyncLabel
import com.jenarvaezg.coindex.ui.notebookCancelledMessage
import com.jenarvaezg.coindex.ui.notebookExportLabel
import com.jenarvaezg.coindex.ui.notebookStepLabel
import com.jenarvaezg.coindex.ui.notebookWarmCancelledMessage
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.seriesLabel
import com.jenarvaezg.coindex.ui.shelf.IndexFacts
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.IndexSort
import com.jenarvaezg.coindex.ui.shelf.OunceBand
import com.jenarvaezg.coindex.ui.shelf.PlateStatus
import com.jenarvaezg.coindex.ui.shelf.StartBand
import com.jenarvaezg.coindex.ui.shelf.indexFacetCounts
import com.jenarvaezg.coindex.ui.shelf.indexFacts
import com.jenarvaezg.coindex.ui.shelf.indexShelfSummary
import com.jenarvaezg.coindex.ui.shelf.indexTally
import com.jenarvaezg.coindex.ui.shelf.issuers
import com.jenarvaezg.coindex.ui.shelf.narrow
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
    shelf: IndexShelf,
    onNarrow: (IndexShelf) -> Unit,
    onSync: () -> Unit,
    onOpen: (CardDestination) -> Unit,
    /**
     * How the notebook is printed, as it was left last time (#228).
     *
     * The export sheet opens on it and works on a copy: what the collector is dragging switches
     * around is a draft, and it only becomes how they print when they press «Exportar».
     */
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    /**
     * The cards this screen is showing, as printable pages on a given configuration.
     *
     * It takes the list rather than reading the index itself, so the notebook is what is on screen
     * — filters and search included — and not what the collector had just narrowed away. Called
     * once the export sheet is open and once per switch moved, never on an idle recomposition:
     * resolving every card's plate is work the index does not owe until somebody asks for paper.
     */
    notebook: (List<IndexCard>, NotebookOptions) -> List<PrintPage>,
    onMessage: (String) -> Unit,
    /**
     * Whether the notebook is being exported right now.
     *
     * The export wants all four of the loader's slots, and the background photograph prefetch holds
     * two of them (#191). This is what lets it stand aside for as long as the collector is watching
     * a progress bar — and start again, on whatever is still missing, when the PDF is out.
     */
    onExporting: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openCard: (IndexCard) -> Unit = { card -> onOpen(destinationOf(card)) }
    // Null while nothing is being printed. The list itself is the switch: what is being exported is
    // the notebook as it was when the button was pressed, so a sync landing mid-export cannot
    // reshuffle the pages under the printer.
    var printing by remember { mutableStateOf<List<PrintPage>?>(null) }
    var step by remember { mutableStateOf<NotebookExportStep>(NotebookExportStep.Drawing(0, "")) }
    // Whether the export sheet is open, and **not** the cards it was opened over: the shelf and the
    // search box stay live above it, so a sheet holding the list from the moment of the tap would go
    // on claiming «lo que hay en el índice ahora mismo» about a narrowing the collector had already
    // changed — and «Exportar» would print it. What freezes is the export, at the second tap.
    var configuring by remember { mutableStateOf(false) }
    // A draft, discarded on «Cancelar»: playing with the switches and backing out has not changed
    // how this collector prints. Reset from the stored configuration each time the sheet opens.
    var draft by remember { mutableStateOf(notebookOptions) }
    // Announced from the state and not from the tap, so cancelling and failing say it too: every
    // way out of an export goes through `printing` becoming null.
    LaunchedEffect(printing != null) { onExporting(printing != null) }
    // Joined once per collection, not once per chip counted: five facets over sixty cards would
    // otherwise walk the inventory thirty times a redraw.
    val facts = remember(state) { indexFacts(state) }
    // Saved across a rotation and never persisted (ADR 0021 §1), unlike the shelf above it.
    var query by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    // **What prints is what the index is showing** (ADR 0021 §13): the filter is the selection, so
    // the notebook needs no mechanism of its own to choose pages.
    val shown = remember(facts, shelf, query) { shelf.narrow(facts, query) }
    // What the export sheet is showing the cost of: recounted when a switch moves, and when the
    // narrowing under it moves. **Outside the grid**, like the export itself and for the same
    // reason: a lazy item is disposed the moment it scrolls off, and resolving sixty plates again
    // every time the sheet scrolls back into view is not what «recontado a cada toque» means.
    val preview = remember(configuring, shown, draft) {
        if (configuring) ExportPreview(shown.size, notebook(shown, draft)) else null
    }

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
                    budget = budget,
                    syncing = syncing,
                    lastSync = lastSync,
                    spread = columns > 1,
                    // One card, one page at least: what stays out of the notebook is a question
                    // for the index and not for the printer (#147).
                    exportableCards = shown.size,
                    exporting = printing != null,
                    onSync = onSync,
                    // The button no longer starts the printer: it opens the sheet that says what
                    // pressing it would cost, on the configuration the collector last used (#228).
                    //
                    // It stays enabled while the sheet is open, and a second tap is a no-op rather
                    // than a reset: the sheet is a card in the list and can be scrolled past, so a
                    // greyed button would be the only way back to it — and one that resets the
                    // switches under the thumb that was moving them is worse than none.
                    onExport = {
                        if (!configuring) draft = notebookOptions
                        configuring = true
                    },
                )
            }

            fullWidth {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchField(value = query, onValueChange = { query = it })
                    FilterShelf(
                        summary = indexShelfSummary(shelf),
                        tally = indexTally(shown.size, state.index.size),
                        expanded = open,
                        onToggle = { open = !open },
                    ) {
                        IndexFacets(
                            facts = facts,
                            shelf = shelf,
                            query = query,
                            onNarrow = onNarrow,
                        )
                    }
                }
            }

            // The five switches and what they cost, before a single page is drawn (#228). In the
            // same slot the progress card takes, because it is the same conversation: what is
            // about to come out of the printer.
            preview?.let { about ->
                fullWidth {
                    ExportOptions(
                        options = draft,
                        pages = about.pages.size,
                        cards = about.cards,
                        onChange = { draft = it },
                        onExport = {
                            val pages = about.pages
                            if (pages.isEmpty()) {
                                onMessage("No hay ninguna colección que llevar al papel.")
                            } else {
                                onNotebookPrinted(draft)
                                step = NotebookExportStep.Drawing(0, pages.first().section.title)
                                printing = pages
                            }
                            configuring = false
                        },
                        onDismiss = { configuring = false },
                    )
                }
            }

            // Visible progress and a way out, which is what a job of eighty-four pages and a
            // thousand photographs owes whoever pressed the button (#169).
            printing?.let { pages ->
                fullWidth {
                    ExportProgress(
                        step = step,
                        pages = pages.size,
                        // Every step but the write, which would close the document under the
                        // thread serializing it.
                        onCancel = when (val current = step) {
                            is NotebookExportStep.Warming -> {
                                {
                                    printing = null
                                    onMessage(
                                        notebookWarmCancelledMessage(
                                            current.photographsDone,
                                            current.photographs,
                                        ),
                                    )
                                }
                            }
                            is NotebookExportStep.Drawing -> {
                                {
                                    printing = null
                                    onMessage(
                                        notebookCancelledMessage(current.pagesDone, pages.size),
                                    )
                                }
                            }
                            NotebookExportStep.Writing -> null
                        },
                    )
                }
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
            //
            // A shelf that hides everything is the third case, and it owes the way out on the spot:
            // the shelf enters folded, so the chip responsible may be two taps away.
            if (shown.isEmpty()) {
                fullWidth {
                    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when {
                                loading -> "Leyendo tu colección…"
                                state.index.isNotEmpty() ->
                                    "Ninguna colección pasa por lo que has puesto."
                                else -> "Todavía no hay colecciones. Sincroniza para traer " +
                                    "tu colección de Numista."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Paper.muted,
                        )
                        if (!loading && state.index.isNotEmpty()) {
                            CardAction(
                                text = "Quitar los filtros",
                                onClick = { onNarrow(IndexShelf()); query = "" },
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }

            items(shown, key = ::cardKey) { card ->
                CollectionCard(card = card, onOpen = { openCard(card) })
            }
        }

        // Outside the grid on purpose: a lazy item is disposed the moment it scrolls off, and the
        // page being recorded would go with it — the export would restart, or stop, depending on
        // where the collector's thumb was.
        printing?.let { pages ->
            NotebookPdfExport(
                pages = pages,
                onStep = { step = it },
                onFinished = { message ->
                    printing = null
                    onMessage(message)
                },
            )
        }
    }
}

/**
 * What the export sheet is about to produce: how many collections, and how much paper (#228).
 *
 * The two travel together because they are recounted together — the láminas are what the filter
 * chose and the pages are what the configuration makes of them — and holding the pages themselves
 * rather than only their number is what lets «Exportar» start the printer on exactly what the sheet
 * had been describing.
 */
private data class ExportPreview(val cards: Int, val pages: List<PrintPage>)

/** What the notebook is doing right now, and the way out of it while there is one. */
@Composable
private fun ExportProgress(
    step: NotebookExportStep,
    pages: Int,
    onCancel: (() -> Unit)?,
) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("Exportando el cuaderno")
        Text(
            notebookStepLabel(step, pages),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Se comparte cuando esté entero. Puedes cancelar sin perder nada.",
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        onCancel?.let { cancel ->
            CardAction(
                text = "Cancelar",
                onClick = cancel,
                modifier = Modifier.padding(top = 10.dp),
            )
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
 *
 * **The title is the whole of what the card does.** It used to carry a «Ver lámina» action besides,
 * which was a second destination on a card that has one (ADR 0021 §9): where a plate exists the
 * title now opens it, and where it does not there was never a button to draw.
 */
@Composable
private fun CollectionCard(
    card: IndexCard,
    onOpen: () -> Unit,
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
    budget: BudgetStatus,
    syncing: Boolean,
    lastSync: SyncRecord?,
    spread: Boolean,
    exportableCards: Int,
    exporting: Boolean,
    onSync: () -> Unit,
    onExport: () -> Unit,
) {
    val title = @Composable {
        RootHeading(
            destination = "Colecciones",
            sentence = "Colecciones a partir de las piezas que tienes ahora mismo.",
        )
    }
    val actions = @Composable {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Two buttons where there were three: «Sin clasificar · N» was never a screen of its
            // own, it was the «Sin colección» chip of Coins (ADR 0021 §1), and the bottom bar is
            // now the way there.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryAction(
                    text = if (syncing) "Sincronizando…" else "Sincronizar",
                    onClick = onSync,
                    enabled = !syncing,
                )
                // Level 2 and not the filled button: what this screen exists for is the collection
                // arriving from Numista, and the notebook is what the collector then does with it.
                CardAction(
                    text = if (exporting) {
                        "Exportando…"
                    } else {
                        notebookExportLabel(exportableCards)
                    },
                    onClick = onExport,
                    enabled = !exporting && exportableCards > 0,
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

/**
 * The six chip rows of Collections: the sort first, then the five filters.
 *
 * The sort leads because it is the one control that answers «why is this card at the top?», which is
 * the question ADR 0021 §6 created by making the order a measured ratio rather than the alphabet.
 */
@Composable
private fun IndexFacets(
    facts: List<IndexFacts>,
    shelf: IndexShelf,
    query: String,
    onNarrow: (IndexShelf) -> Unit,
) {
    val counts = indexFacetCounts(facts, shelf, query)

    Facet("Orden") {
        IndexSort.entries.forEach { sort ->
            FilterChip(
                label = sort.label,
                count = null,
                selected = shelf.sort == sort,
                onClick = { onNarrow(shelf.copy(sort = sort)) },
            )
        }
    }
    // Numista's `collected_items` carries no date of any kind, so this order is by row id — «alta en
    // Numista», not «compra». Said here rather than left to be guessed at from a surprising order.
    if (shelf.sort == IndexSort.RecentlyAdded) {
        Text(
            "Numista no guarda fecha de compra, así que este orden es el del alta en Numista.",
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
            modifier = Modifier.padding(top = 6.dp),
        )
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
        OunceBand.entries.forEach { band ->
            FilterChip(
                label = band.label,
                count = counts.weight.of(band),
                selected = shelf.weight == band,
                onClick = { onNarrow(shelf.copy(weight = band)) },
            )
        }
    }
    Facet("Empieza en") {
        FilterChip(
            label = "Cualquier año",
            count = null,
            selected = shelf.startsIn == null,
            onClick = { onNarrow(shelf.copy(startsIn = null)) },
        )
        StartBand.entries.forEach { band ->
            FilterChip(
                label = band.label,
                count = counts.startsIn.of(band),
                selected = shelf.startsIn == band,
                onClick = { onNarrow(shelf.copy(startsIn = band)) },
            )
        }
    }
    Facet("Estado") {
        FilterChip(
            label = "Todas",
            count = null,
            selected = shelf.status == null,
            onClick = { onNarrow(shelf.copy(status = null)) },
        )
        PlateStatus.entries.forEach { status ->
            FilterChip(
                label = status.label,
                count = counts.status.of(status),
                selected = shelf.status == status,
                onClick = { onNarrow(shelf.copy(status = status)) },
            )
        }
    }
    Facet("Serie") {
        FilterChip(
            label = "Cualquiera",
            count = null,
            selected = shelf.series == null,
            onClick = { onNarrow(shelf.copy(series = null)) },
        )
        SeriesStatus.entries.forEach { status ->
            FilterChip(
                label = seriesLabel(status),
                count = counts.series.of(status),
                selected = shelf.series == status,
                onClick = { onNarrow(shelf.copy(series = status)) },
            )
        }
    }
}

/** A row of the page rather than a card of the grid: headings and notices span every column. */
private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}
