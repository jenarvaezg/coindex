package com.jenarvaezg.coindex.ui.screens

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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.SewnEdgeCounts
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.components.AlbumChrome
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.Facet
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.FilterChip
import com.jenarvaezg.coindex.ui.components.FilterShelf
import com.jenarvaezg.coindex.ui.components.SearchField
import com.jenarvaezg.coindex.ui.components.countryAxisItems
import com.jenarvaezg.coindex.ui.components.travellingCoin
import com.jenarvaezg.coindex.ui.components.yearAxisItems
import com.jenarvaezg.coindex.ui.countLabel
import com.jenarvaezg.coindex.ui.NOTEBOOK_EXPORTING_LABEL
import com.jenarvaezg.coindex.ui.NOTHING_TO_PRINT_MESSAGE
import com.jenarvaezg.coindex.ui.PARTIAL_SYNC_EXPLANATION
import com.jenarvaezg.coindex.ui.PARTIAL_SYNC_EYEBROW
import com.jenarvaezg.coindex.ui.indexCoverageLabel
import com.jenarvaezg.coindex.ui.notebookCancelledMessage
import com.jenarvaezg.coindex.ui.notebookExportLabel
import com.jenarvaezg.coindex.ui.notebookWarmCancelledMessage
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.seriesLabel
import com.jenarvaezg.coindex.ui.shelf.ANY_FILTER
import com.jenarvaezg.coindex.ui.shelf.AXIS_FACET
import com.jenarvaezg.coindex.ui.shelf.CLEAR_FILTERS_ACTION
import com.jenarvaezg.coindex.ui.shelf.COUNTRY_FACET
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexFacts
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.IndexSort
import com.jenarvaezg.coindex.ui.shelf.NotebookAxis
import com.jenarvaezg.coindex.ui.shelf.OunceBand
import com.jenarvaezg.coindex.ui.shelf.PlateStatus
import com.jenarvaezg.coindex.ui.shelf.RECENTLY_ADDED_NOTE
import com.jenarvaezg.coindex.ui.shelf.SERIES_FACET
import com.jenarvaezg.coindex.ui.shelf.SORT_FACET
import com.jenarvaezg.coindex.ui.shelf.STARTS_IN_FACET
import com.jenarvaezg.coindex.ui.shelf.STATUS_FACET
import com.jenarvaezg.coindex.ui.shelf.StartBand
import com.jenarvaezg.coindex.ui.shelf.WEIGHT_FACET
import com.jenarvaezg.coindex.ui.shelf.YearFilter
import com.jenarvaezg.coindex.ui.shelf.countryAxis
import com.jenarvaezg.coindex.ui.shelf.countryAxisTally
import com.jenarvaezg.coindex.ui.shelf.indexEmptyLabel
import com.jenarvaezg.coindex.ui.shelf.indexFacetCounts
import com.jenarvaezg.coindex.ui.shelf.indexFacts
import com.jenarvaezg.coindex.ui.shelf.indexShelfSummary
import com.jenarvaezg.coindex.ui.shelf.indexTally
import com.jenarvaezg.coindex.ui.shelf.issuers
import com.jenarvaezg.coindex.ui.shelf.narrow
import com.jenarvaezg.coindex.ui.shelf.narrowUnclaimed
import com.jenarvaezg.coindex.ui.shelf.unclaimedFacts
import com.jenarvaezg.coindex.ui.shelf.yearAxis
import com.jenarvaezg.coindex.ui.shelf.yearAxisTally
import com.jenarvaezg.coindex.ui.theme.Paper

/** The album cell: one round coin and two short lines under it. */
private val MIN_CARD_WIDTH = 104.dp

/** Every card reserves the same name range, so the fractions form one baseline across a row. */
internal const val COLLECTION_NAME_LINES = 2

private val PAGE_MARGIN = 12.dp
private val INDEX_GUTTER = 8.dp

/** How many cards fit side by side, counted from the page rather than from the device. */
internal fun indexColumns(availableWidth: Dp): Int {
    val usable = availableWidth - PAGE_MARGIN * 2 + INDEX_GUTTER
    val perColumn = MIN_CARD_WIDTH + INDEX_GUTTER
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
 * Three compact cells fit across the measured Pixel 7. Wider screens keep adding cells rather than
 * stretching the holes, so the album keeps the same reading density in every orientation.
 */
@Composable
fun IndexScreen(
    state: CollectionState,
    loading: Boolean,
    lastSync: SyncRecord?,
    shelf: IndexShelf,
    /**
     * The curated catalogs, so the country and year axes can walk every evidenced plate
     * (ADR 0026 §9). The index of cards is not enough: a member's country is not the card's.
     */
    catalogs: List<CollectionCatalog>,
    onNarrow: (IndexShelf) -> Unit,
    onOpen: (CardDestination) -> Unit,
    /**
     * Open Monedas with a shelf already narrowed — country or year axis seats that want the list,
     * not another plate.
     */
    onOpenCoins: (CoinsShelf) -> Unit,
    /** The sewn-edge census, assembled once above the three roots so this screen cannot invent its own. */
    sewnEdge: SewnEdgeCounts?,
    onSettings: () -> Unit,
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
     *
     * The second list is the coins no collection claims (#275), narrowed by the same shelf: what
     * belongs in the notebook is this screen's answer, and the printer only decides where it goes.
     */
    notebook: (List<IndexCard>, List<CollectedItem>, NotebookOptions) -> List<PrintPage>,
    onMessage: (UiNotice) -> Unit,
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
    // Null while nothing is being printed. The job itself is the switch: what is being exported is
    // the notebook as it was when the button was pressed, so a sync landing mid-export cannot
    // reshuffle the pages under the printer. [destination] says whether it lands in Descargas or
    // leaves for another app (#285).
    var printing by remember { mutableStateOf<NotebookJob?>(null) }
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
    // The coins no collection claims, measured against the **whole** index and then narrowed by the
    // same shelf (#275): having a collection is a fact about the coin, so a filter cannot orphan one
    // that lives in a box — but a filtered notebook still takes only the loose coins it is about.
    val loose = remember(state) { unclaimedFacts(state) }
    val looseShown = remember(loose, shelf, query) { shelf.narrowUnclaimed(loose, query) }
    // Catalogs and loose rows that survive the shelf: the country/year axes honour the same chips
    // the plate axis does, so a weight filter does not leave Rusia painted in full beside a
    // narrowed card grid.
    val keptCatalogIds = remember(shown) {
        shown.mapNotNull { card -> (card as? IndexCard.Derived)?.plateCatalogId }.toSet()
    }
    val keptLooseIds = remember(looseShown) { looseShown.map { it.id }.toSet() }
    val countryModel = remember(state, catalogs, keptCatalogIds, keptLooseIds, shelf.axis) {
        if (shelf.axis != NotebookAxis.ByCountry) {
            null
        } else {
            countryAxis(
                state = state,
                catalogs = catalogs,
                keptCatalogIds = keptCatalogIds,
                keptLooseIds = keptLooseIds,
            )
        }
    }
    val yearModel = remember(state, catalogs, keptCatalogIds, shelf.axis) {
        if (shelf.axis != NotebookAxis.ByYear) {
            null
        } else {
            // Years walk every owned piece that still belongs to a kept catalog or is loose-kept;
            // pieces inside a hidden card stay off the arc with it.
            val keptItemIds = buildSet {
                for (card in shown) {
                    when (card) {
                        is IndexCard.Derived -> {
                            state.itemsByKey[card.key]?.forEach { add(it.id) }
                        }
                        is IndexCard.Box -> card.box.items.forEach { add(it.id) }
                    }
                }
                addAll(keptLooseIds)
            }
            yearAxis(
                state = state,
                catalogs = catalogs,
                keptCatalogIds = keptCatalogIds,
                keptItemIds = keptItemIds,
            )
        }
    }
    val axisTally = when (shelf.axis) {
        NotebookAxis.ByPlate -> indexTally(shown.size, state.index.size)
        NotebookAxis.ByCountry -> countryModel?.let {
            countryAxisTally(it.ownedSlots, it.totalSlots)
        } ?: indexTally(shown.size, state.index.size)
        NotebookAxis.ByYear -> yearModel?.let {
            yearAxisTally(it.ownedYears, it.totalYears)
        } ?: indexTally(shown.size, state.index.size)
    }
    // What the export sheet is showing the cost of: recounted when a switch moves, and when the
    // narrowing under it moves. **Outside the grid**, like the export itself and for the same
    // reason: a lazy item is disposed the moment it scrolls off, and resolving sixty plates again
    // every time the sheet scrolls back into view is not what «recontado a cada toque» means.
    val preview = remember(configuring, shown, looseShown, draft) {
        if (!configuring) {
            null
        } else {
            ExportPreview(
                // The lámina of the loose coins counts as one, because it is one: the sheet counts
                // what this configuration produces, which is the whole reason it recounts at all.
                cards = shown.size + if (draft.unclaimed && looseShown.isNotEmpty()) 1 else 0,
                pages = notebook(shown, looseShown, draft),
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        // Counted here rather than left to GridCells.Adaptive, because the heading needs the
        // same answer: one column is a page, two are a spread. The country and year axes are one
        // column of blocks; the plate axis keeps the album density.
        val columns = when (shelf.axis) {
            NotebookAxis.ByPlate -> indexColumns(maxWidth)
            NotebookAxis.ByCountry, NotebookAxis.ByYear -> 1
        }


        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = PAGE_MARGIN, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(INDEX_GUTTER),
            // 6dp is the measured pitch that leaves 3.68 rows in the Pixel 7 fold: 11.04 cards.
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            fullWidth {
                AlbumChrome(
                    counts = sewnEdge,
                    onSettings = onSettings,
                )
            }

            fullWidth {
                Column {
                    SearchField(value = query, onValueChange = { query = it })
                    FilterShelf(
                        summary = indexShelfSummary(shelf, expanded = open),
                        tally = axisTally,
                        expanded = open,
                        onToggle = { open = !open },
                        actionLabel = if (printing != null) {
                            NOTEBOOK_EXPORTING_LABEL
                        } else {
                            notebookExportLabel()
                        },
                        actionEnabled = printing == null && shown.isNotEmpty(),
                        onAction = {
                            if (!configuring) draft = notebookOptions
                            configuring = true
                        },
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

            // The six switches and what they cost, before a single page is drawn (#228). In the
            // same slot the progress card takes, because it is the same conversation: what is
            // about to come out of the printer.
            preview?.let { about ->
                fullWidth {
                    fun begin(destination: ExportDestination) {
                        val pages = about.pages
                        if (pages.isEmpty()) {
                            onMessage(UiNotice(NOTHING_TO_PRINT_MESSAGE))
                        } else {
                            onNotebookPrinted(draft)
                            step = NotebookExportStep.Drawing(
                                0,
                                // The plate at the top of the folio, which since #232 may not
                                // be the only one on it.
                                pages.first().blocks.first().section.title,
                            )
                            printing = NotebookJob(pages, destination)
                        }
                        configuring = false
                    }
                    ExportOptions(
                        options = draft,
                        pages = about.pages.size,
                        cards = about.cards,
                        // What «Sin colección» has left to offer under the current narrowing, so a
                        // switch with no lámina behind it is greyed instead of ticked and inert.
                        loose = looseShown.size,
                        onChange = { draft = it },
                        onDownload = { begin(ExportDestination.Download) },
                        onShare = { begin(ExportDestination.Share) },
                        onDismiss = { configuring = false },
                    )
                }
            }

            // Visible progress and a way out, which is what a job of eighty-four pages and a
            // thousand photographs owes whoever pressed the button (#169).
            printing?.let { job ->
                fullWidth {
                    ExportProgress(
                        step = step,
                        pages = job.pages.size,
                        // Every step but the write, which would close the document under the
                        // thread serializing it.
                        onCancel = when (val current = step) {
                            is NotebookExportStep.Warming -> {
                                {
                                    printing = null
                                    onMessage(
                                        UiNotice(
                                            notebookWarmCancelledMessage(
                                                current.photographsDone,
                                                current.photographs,
                                            ),
                                        ),
                                    )
                                }
                            }
                            is NotebookExportStep.Drawing -> {
                                {
                                    printing = null
                                    onMessage(
                                        UiNotice(
                                            notebookCancelledMessage(
                                                current.pagesDone,
                                                job.pages.size,
                                            ),
                                        ),
                                    )
                                }
                            }
                            NotebookExportStep.Writing -> null
                        },
                    )
                }
            }

            if (lastSync?.partialFailure != null) {
                fullWidth {
                    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                        Eyebrow(PARTIAL_SYNC_EYEBROW)
                        Text(
                            PARTIAL_SYNC_EXPLANATION,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            // A shelf that hides everything owes the way out on the spot: the shelf enters
            // folded, so the chip responsible may be two taps away.
            val axisEmpty = when (shelf.axis) {
                NotebookAxis.ByPlate -> shown.isEmpty()
                NotebookAxis.ByCountry -> countryModel?.blocks.isNullOrEmpty()
                NotebookAxis.ByYear -> yearModel?.cells.isNullOrEmpty() == true
            }
            if (axisEmpty) {
                fullWidth {
                    FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            indexEmptyLabel(loading, anyCollections = state.index.isNotEmpty()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Paper.muted,
                        )
                        if (!loading && state.index.isNotEmpty()) {
                            CardAction(
                                text = CLEAR_FILTERS_ACTION,
                                onClick = { onNarrow(IndexShelf()); query = "" },
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }

            when (shelf.axis) {
                NotebookAxis.ByPlate -> items(shown, key = ::cardKey) { card ->
                    val images = card.cover?.let { cover -> state.images[cover.typeId] }
                    val photo = when (card.cover?.printedSide) {
                        PrintedSide.Obverse -> images?.obverse
                        PrintedSide.Reverse -> images?.reverse
                        null -> null
                    }
                    CollectionCard(
                        card = card,
                        photo = photo,
                        // A coin only flies where it has a casilla of its own to land in, which is
                        // exactly the cards that open a plate (ADR 0026 §3). The other 20 of the
                        // father's 69 have no ratio, and that is what tells them apart before touching.
                        travelsTo = (card as? IndexCard.Derived)?.plateCatalogId,
                        onOpen = { openCard(card) },
                    )
                }
                NotebookAxis.ByCountry -> countryModel?.let { model ->
                    countryAxisItems(
                        model = model,
                        images = state.images,
                        onCountryClick = { country ->
                            onOpenCoins(
                                CoinsShelf(
                                    issuer = country,
                                    axis = NotebookAxis.ByCountry,
                                ),
                            )
                        },
                    )
                }
                NotebookAxis.ByYear -> yearModel?.let { model ->
                    yearAxisItems(
                        model = model,
                        images = state.images,
                        onCountryClick = { country ->
                            onOpenCoins(
                                CoinsShelf(
                                    issuer = country,
                                    axis = NotebookAxis.ByCountry,
                                ),
                            )
                        },
                        onYearClick = { year ->
                            onOpenCoins(
                                CoinsShelf(
                                    year = YearFilter.Of(year),
                                    axis = NotebookAxis.ByYear,
                                ),
                            )
                        },
                    )
                }
            }
        }

        // Outside the grid on purpose: a lazy item is disposed the moment it scrolls off, and the
        // page being recorded would go with it — the export would restart, or stop, depending on
        // where the collector's thumb was.
        printing?.let { job ->
            NotebookPdfExport(
                pages = job.pages,
                destination = job.destination,
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
 * rather than only their number is what lets «Descargar» / «Compartir» start the printer on exactly
 * what the sheet had been describing.
 */
private data class ExportPreview(val cards: Int, val pages: List<PrintPage>)

/**
 * The notebook in flight: the pages frozen at the tap, and whether they land in Descargas or leave
 * for another app (#285).
 */
private data class NotebookJob(
    val pages: List<PrintPage>,
    val destination: ExportDestination,
)

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
 * The photograph now carries the hierarchy the former country eyebrow and physical-variant line
 * were doing badly. Under the hole only the card name and its ratio/count remain (ADR 0026 §12).
 *
 * **The card is the whole of what the card does.** It used to carry a «Ver lámina» action besides,
 * which was a second destination on a card that has one (ADR 0021 §9): where a plate exists the
 * hole, name and fraction now open it as one target.
 */
@Composable
private fun CollectionCard(
    card: IndexCard,
    photo: CoinPhoto?,
    travelsTo: String?,
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
            photo = photo,
            modifier = Modifier
                .size(104.dp)
                .travellingCoin(travelsTo),
        )
        CollectionName(card.name)
        Text(
            card.coverage?.let(::indexCoverageLabel) ?: countLabel(card.distinctTypes, card.quantity),
            style = MaterialTheme.typography.labelLarge,
            color = Paper.rust,
            textAlign = TextAlign.Center,
        )
    }
}

/** The fixed two-line cartouche shared by every collection card in a grid row. */
@Composable
internal fun CollectionName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        autoSize = TextAutoSize.StepBased(
            minFontSize = 13.sp,
            maxFontSize = 17.sp,
            stepSize = 0.5.sp,
        ),
        textAlign = TextAlign.Center,
        minLines = COLLECTION_NAME_LINES,
        maxLines = COLLECTION_NAME_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

/**
 * The chip rows of Collections: the axis first, then the sort, then the five filters.
 *
 * The axis leads because it is the one control that answers «what is a cell?» (ADR 0026 §9), and
 * the sort follows because it answers «why is this card at the top?» — which is the question
 * ADR 0021 §6 created by making the order a measured ratio rather than the alphabet.
 */
@Composable
private fun IndexFacets(
    facts: List<IndexFacts>,
    shelf: IndexShelf,
    query: String,
    onNarrow: (IndexShelf) -> Unit,
) {
    val counts = indexFacetCounts(facts, shelf, query)

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
        IndexSort.entries.forEach { sort ->
            FilterChip(
                label = sort.label,
                count = null,
                selected = shelf.sort == sort,
                onClick = { onNarrow(shelf.copy(sort = sort)) },
            )
        }
    }
    if (shelf.sort == IndexSort.RecentlyAdded) {
        Text(
            RECENTLY_ADDED_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.muted,
            modifier = Modifier.padding(top = 6.dp),
        )
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
        OunceBand.entries.forEach { band ->
            FilterChip(
                label = band.label,
                count = counts.weight.of(band),
                selected = shelf.weight == band,
                onClick = { onNarrow(shelf.copy(weight = band)) },
            )
        }
    }
    Facet(STARTS_IN_FACET) {
        FilterChip(
            label = ANY_FILTER,
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
    Facet(STATUS_FACET) {
        FilterChip(
            label = ANY_FILTER,
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
    Facet(SERIES_FACET) {
        FilterChip(
            label = ANY_FILTER,
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
