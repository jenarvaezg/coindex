package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
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
import com.jenarvaezg.coindex.ui.components.ForwardGlyph
import com.jenarvaezg.coindex.ui.components.HoleAbsence
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
import com.jenarvaezg.coindex.ui.shelf.COUNTRY_FACET
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.INDEX_SEARCH_PLACEHOLDER
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
import com.jenarvaezg.coindex.ui.shelf.ShelfNarrowing
import com.jenarvaezg.coindex.ui.shelf.StartBand
import com.jenarvaezg.coindex.ui.shelf.WEIGHT_FACET
import com.jenarvaezg.coindex.ui.shelf.YearFilter
import com.jenarvaezg.coindex.ui.shelf.clearNarrowingAction
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
import com.jenarvaezg.coindex.ui.shelf.shelfNarrowing
import com.jenarvaezg.coindex.ui.shelf.unclaimedFacts
import com.jenarvaezg.coindex.ui.shelf.yearAxis
import com.jenarvaezg.coindex.ui.shelf.yearAxisTally
import com.jenarvaezg.coindex.ui.DrawnWish
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.showcaseDoorLabel
import com.jenarvaezg.coindex.ui.wishDoorLabel
import com.jenarvaezg.coindex.ui.wishDoorMoreLabel
import com.jenarvaezg.coindex.ui.wishDoorNote
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
    /**
     * The casillas the collector is looking for, which the row at the head of the sheet names and draws
     * (ADR 0029 §6, #520).
     *
     * **The rows and not a count**, since #520: the row draws the first few of them as coins, so it needs
     * the type and the face each casilla rests on. Empty means there is no row at all — they are crossed
     * with the collection above this screen, over the whole of it and never over the narrowing, because a
     * filter on the shelf is about the cards of the index and these coins are not in it.
     *
     * In the list's own order, the last marked first: what the row shows is what was just marked.
     */
    wishes: List<DrawnWish>,
    /**
     * How many curated plates the collector owns nothing of, which the row at the foot names (ADR 0030 §8).
     *
     * The **twenty and not the twenty-three**: what is behind that row which this list does not already
     * hold is the shelf window. Zero means no row, which is the same clause the marks above keep.
     */
    showcase: Int,
    /** Into «Lo que busco», from the row at the head: the annex's sibling room (ADR 0030 §8, #520). */
    onOpenWishes: () -> Unit,
    /** Into «Explorar», from the row at the foot. Two rows, two destinations, one name each. */
    onOpenShowcase: () -> Unit,
    onOpenPhone: () -> Unit,
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
    val facts = remember(state, catalogs) { indexFacts(state, catalogs) }
    // Saved across a rotation and never persisted (ADR 0021 §1), unlike the shelf above it.
    var query by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    // Countries whose absences the collector unfolded (#417). Saved across a rotation like the
    // search box and, like it, never persisted: which country you were reading is not a preference
    // the app should still hold tomorrow, and the sheet opens folded so the axis keeps its measure.
    var unfolded by rememberSaveable(
        saver = listSaver<MutableState<Set<String>>, String>(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toSet()) },
        ),
    ) { mutableStateOf(emptySet<String>()) }
    // **What prints is what the index is showing** (ADR 0021 §13): the filter is the selection, so
    // the notebook needs no mechanism of its own to choose pages.
    val shown = remember(facts, shelf, query) { shelf.narrow(facts, query) }
    // What is narrowing right now, which the empty card answers and the door of the annex declares
    // itself outside of (#515).
    val narrowing = shelfNarrowing(filters = shelf.active, query = query)
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
    val countryModel = remember(state, catalogs, keptCatalogIds, keptLooseIds, shelf.axis, shelf.issuer) {
        if (shelf.axis != NotebookAxis.ByCountry) {
            null
        } else {
            countryAxis(
                state = state,
                catalogs = catalogs,
                keptCatalogIds = keptCatalogIds,
                keptLooseIds = keptLooseIds,
                keptCountry = shelf.issuer,
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
                    onOpenPhone = onOpenPhone,
                )
            }

            fullWidth {
                Column {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = INDEX_SEARCH_PLACEHOLDER,
                    )
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

            // «Lo que busco», at the **head** of the sheet and with its casillas drawn (#520).
            //
            // The one row of this screen that is not about what the collector has, and the reason it
            // is up here rather than at the foot with the shelf's: a list for a fair is the most
            // actionable thing the app holds, and the foot of sixty-nine cards is four folds away.
            // ADR 0026 §8 clause 3 is amended for it — an annex has two possible places now, and
            // which one it takes is whether its population is a shopping list or a window.
            //
            // **Not printed at zero**, the clause the sewn edge keeps while it reads (#418): with
            // nothing marked the first view is exactly the one it was before this ticket.
            if (wishes.isNotEmpty()) {
                fullWidth {
                    AnnexDoor(
                        label = wishDoorLabel(wishes.size),
                        // Its count is of another population, so a search running above it leaves it
                        // where it was — and the row says that rather than looking stale (#515). It
                        // is said **here and not at the foot**: two rows, one sentence.
                        note = wishDoorNote(searching = query.isNotBlank()),
                        onOpen = onOpenWishes,
                    ) {
                        WishedCoins(wishes = wishes, images = state.images)
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
                            indexEmptyLabel(
                                loading,
                                anyCollections = state.index.isNotEmpty(),
                                narrowing = narrowing,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Paper.muted,
                        )
                        // Exactly what is narrowing, undone by the name it is offered under (#515):
                        // the chips go without taking the axis and the sort with them, and the box
                        // is emptied only where something was typed into it.
                        val undo = clearNarrowingAction(narrowing).takeIf {
                            !loading && state.index.isNotEmpty()
                        }
                        undo?.let { action ->
                            CardAction(
                                text = action,
                                onClick = {
                                    if (narrowing != ShelfNarrowing.Search) {
                                        onNarrow(shelf.withoutFilters())
                                    }
                                    if (narrowing != ShelfNarrowing.Filters) query = ""
                                },
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
                        expandedCountries = unfolded,
                        onToggleFold = { country ->
                            unfolded = if (country in unfolded) {
                                unfolded - country
                            } else {
                                unfolded + country
                            }
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

            // The door of «Explorar», and the last row of this list whatever axis it is read on
            // (ADR 0026 §8 clause 3). **One name and one destination** since #520: it used to carry a
            // composed label that named the marks too and opened only this, which is a row promising
            // two rooms from one tap. **Not printed at zero**, and it carries no note: the sentence
            // about the search box lives on the row at the head, once.
            showcaseDoorLabel(plates = showcase)?.let { label ->
                fullWidth {
                    AnnexDoor(label = label, onOpen = onOpenShowcase)
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
 * A door of the annex: deeper paper, its name with its count, and the way on.
 *
 * **A row of the sheet and not a card**, which is the whole of ADR 0026 §8 clause 3: an annex is not a
 * fourth cell of the bar and not a collection of the index, so its entrance is a row on paper a shade
 * deeper, the way the sewn edge is deeper than the leaf.
 *
 * **Where that row goes is no longer «the last thing on the page»** (§8 clause 3 as amended by #520). The
 * index hangs two of them: the marks at the head, because a list for a fair is what the collector came to
 * act on, and the shelf window at the foot, because browsing what you do not own is where a page ends.
 * «Explorar» hangs a third at its own head, into «Lo que busco». **One drawing for the three** — two
 * drawings of one shape is how the second one comes to be a shade off.
 *
 * The arrow is drawn rather than typed, because neither of the album's two typefaces has that glyph
 * (#298), and it is the same chevron «Volver» uses, mirrored: the two halves of one journey.
 *
 * [content] is what a row draws under its name — the marked casillas, on the index's own row — and it is
 * inside the target, because it is part of what the row is about rather than a second thing to press.
 */
@Composable
internal fun AnnexDoor(
    label: String,
    onOpen: () -> Unit,
    note: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Paper.paperDeep)
            .semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The note is under the name and inside the target, so it reads as this row saying something
        // about itself. Body and not the album's versalitas — it is a sentence about what the app did
        // with a word, which is the shape #513 gave the line under the shelf's orders — but the small
        // body and not #513's: there the line answered the control above it, and here it sits under
        // the name of the door, which has to stay the loudest thing in the row.
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Paper.ink)
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Paper.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            content?.let {
                Spacer(Modifier.height(8.dp))
                it()
            }
        }
        ForwardGlyph()
    }
}

/** How many marked casillas the index's row draws before it starts counting them instead (#520). */
private const val WISHED_COINS_DRAWN = 3

/** The diameter of a coin on that row: a quarter of the album's, and not a target of its own. */
private val WISHED_COIN = 40.dp

/**
 * The marked casillas as coins, on the row that opens «Lo que busco» (#520).
 *
 * **The first three and then a count**, because what the drawing is for is recognising a coin: seven of
 * them across 411 dp would be 32 dp each with nothing left for the cardboard, and a coin nobody can
 * recognise is furniture ADR 0026 §5 would price. The order is the list's own — the last marked first —
 * so the row shows what the collector just marked rather than a fixed three.
 *
 * **Whole and not in penumbra** ([HoleAbsence.Wanted]): these are casillas he is hunting, not casillas
 * missing from a plate he is filling. On the prototype the 14 % ghost at this size measured as two grey
 * discs, which is what #520 changed and what #556 will decide for the rest of the app.
 *
 * No year, no name and no price under them: the row is a door, and what is behind it is the list where
 * each casilla carries all three.
 */
@Composable
private fun WishedCoins(wishes: List<DrawnWish>, images: Map<Int, TypeImages>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        wishes.take(WISHED_COINS_DRAWN).forEach { wish ->
            AlbumHole(
                photo = images[wish.typeId]?.printedPhoto(wish.printedSide),
                absence = HoleAbsence.Wanted,
                modifier = Modifier.size(WISHED_COIN),
            )
        }
        wishDoorMoreLabel(rest = wishes.size - WISHED_COINS_DRAWN)?.let { more ->
            Text(
                more,
                style = MaterialTheme.typography.labelMedium,
                color = Paper.muted,
                modifier = Modifier.padding(start = 4.dp),
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
internal fun CollectionName(
    name: String,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Text(
        text = name,
        // Simple + Auto: wrap at spaces/hyphens (with a real hyphen when the dictionary
        // splits a word). Without them, HighQuality was carving «Ibero-American» into
        // «Ibero-America» / «n» on the three-column card (#405).
        style = MaterialTheme.typography.titleMedium.copy(
            lineBreak = LineBreak.Simple,
            hyphens = Hyphens.Auto,
        ),
        autoSize = TextAutoSize.StepBased(
            minFontSize = 13.sp,
            maxFontSize = 17.sp,
            stepSize = 0.5.sp,
        ),
        textAlign = TextAlign.Center,
        minLines = COLLECTION_NAME_LINES,
        maxLines = COLLECTION_NAME_LINES,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = onTextLayout,
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
        counts.weight.populatedIn(OunceBand.entries, keep = shelf.weight).forEach { (band, count) ->
            FilterChip(
                label = band.label,
                count = count,
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
        counts.startsIn.populatedIn(StartBand.entries, keep = shelf.startsIn).forEach { (band, count) ->
            FilterChip(
                label = band.label,
                count = count,
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
        counts.status.populatedIn(PlateStatus.entries, keep = shelf.status).forEach { (status, count) ->
            FilterChip(
                label = status.label,
                count = count,
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
        counts.series.populatedIn(SeriesStatus.entries, keep = shelf.series).forEach { (status, count) ->
            FilterChip(
                label = seriesLabel(status),
                count = count,
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
