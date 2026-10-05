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

/** The album cell: one coin and two short lines under it. */
private val MIN_CARD_WIDTH = 104.dp

/** Every card reserves two name lines, so the fractions share a baseline across a row. */
internal const val COLLECTION_NAME_LINES = 2

private val PAGE_MARGIN = 12.dp
private val INDEX_GUTTER = 8.dp

/** Cards that fit side by side in [availableWidth]. */
internal fun indexColumns(availableWidth: Dp): Int {
    val usable = availableWidth - PAGE_MARGIN * 2 + INDEX_GUTTER
    val perColumn = MIN_CARD_WIDTH + INDEX_GUTTER
    return (usable / perColumn).toInt().coerceAtLeast(1)
}

/**
 * The collection index: one card per collection, in one list and one order.
 *
 * Catalogs, groupings and boxes are one kind of collection (ADR 0021 §2), so nothing on screen
 * tells them apart. The order comes from the domain (ADR 0021 §6); this screen draws
 * [CollectionState.index] as it arrives. Wider screens add cells rather than stretching the holes.
 */
@Composable
fun IndexScreen(
    state: CollectionState,
    loading: Boolean,
    lastSync: SyncRecord?,
    shelf: IndexShelf,
    /**
     * For the país chip: a card spans every country its plate names, not just its members' (#170).
     */
    catalogs: List<CollectionCatalog>,
    onNarrow: (IndexShelf) -> Unit,
    onOpen: (CardDestination) -> Unit,
    /** Opens Monedas pre-narrowed, from country or year axis seats. */
    onOpenCoins: (CoinsShelf) -> Unit,
    /** Computed once above the three roots so they all show the same counts. */
    sewnEdge: SewnEdgeCounts?,
    /**
     * The marked casillas, drawn on the row at the top (ADR 0029 §6, #520), last marked first.
     * Empty means no row. Computed over the whole collection, never the narrowing: these coins are
     * not index cards.
     */
    wishes: List<DrawnWish>,
    /**
     * Curated plates the collector owns nothing of, for the row at the foot (ADR 0030 §8). Excludes
     * the marked plates «Explorar» also shows, which are already in this list. Zero means no row.
     */
    showcase: Int,
    /** Opens «Lo que busco» from the row at the top (ADR 0030 §8, #520). */
    onOpenWishes: () -> Unit,
    /** Opens «Explorar» from the row at the foot. */
    onOpenShowcase: () -> Unit,
    onOpenPhone: () -> Unit,
    /**
     * The last-used print options (#228). The export sheet edits a copy, saved only on export.
     */
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    /**
     * Turns the shown cards, plus the loose coins under the same narrowing (#275), into printable
     * pages for a configuration. Takes the lists so the notebook is exactly what is on screen.
     * Called when the export sheet opens and on each switch change, never on idle recomposition.
     */
    notebook: (List<IndexCard>, List<CollectedItem>, NotebookOptions) -> List<PrintPage>,
    onMessage: (UiNotice) -> Unit,
    /**
     * Whether an export is running. The export wants all four image-loader slots, so the background
     * photo prefetch (which holds two) pauses meanwhile and resumes afterwards (#191).
     */
    onExporting: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openCard: (IndexCard) -> Unit = { card -> onOpen(destinationOf(card)) }
    // Null when idle. The job freezes the pages at the tap, so a sync mid-export can't reshuffle
    // them.
    var printing by remember { mutableStateOf<NotebookJob?>(null) }
    var step by remember { mutableStateOf<NotebookExportStep>(NotebookExportStep.Drawing(0, "")) }
    // Only whether the sheet is open, not a snapshot of the cards: the shelf stays live above it,
    // and the pages freeze only when the export starts.
    var configuring by remember { mutableStateOf(false) }
    // Discarded on «Cancelar»; reset from the stored options each time the sheet opens.
    var draft by remember { mutableStateOf(notebookOptions) }
    // Driven by the state, not the tap, so cancel and failure report it too.
    LaunchedEffect(printing != null) { onExporting(printing != null) }
    // Once per collection, not once per counted chip.
    val facts = remember(state, catalogs) { indexFacts(state, catalogs) }
    // Saved across a rotation and never persisted (ADR 0021 §1), unlike the shelf above it.
    var query by rememberSaveable { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    // Countries whose absences are unfolded (#417). Like the search, survives rotation but is never
    // persisted.
    var unfolded by rememberSaveable(
        saver = listSaver<MutableState<Set<String>>, String>(
            save = { it.value.toList() },
            restore = { mutableStateOf(it.toSet()) },
        ),
    ) { mutableStateOf(emptySet<String>()) }
    // What prints is what the index shows (ADR 0021 §13): the filter is the selection.
    val shown = remember(facts, shelf, query) { shelf.narrow(facts, query) }
    // What the empty card names and undoes, and what the wishes row says it ignores (#515).
    val narrowing = shelfNarrowing(filters = shelf.active, query = query)
    // Loose coins are found against the whole index, then narrowed by the shelf (#275): a filter
    // can't make a boxed coin loose.
    val loose = remember(state) { unclaimedFacts(state) }
    val looseShown = remember(loose, shelf, query) { shelf.narrowUnclaimed(loose, query) }
    // What survives the shelf, so the country and year axes honour the same chips as the plate
    // axis.
    val keptCatalogIds = remember(shown) {
        shown.mapNotNull { card -> (card as? IndexCard.Derived)?.plateCatalogId }.toSet()
    }
    val keptLooseIds = remember(looseShown) { looseShown.map { it.id }.toSet() }
    val countryModel = remember(state, keptCatalogIds, keptLooseIds, shelf.axis, shelf.issuer) {
        if (shelf.axis != NotebookAxis.ByCountry) {
            null
        } else {
            countryAxis(
                state = state,
                keptCatalogIds = keptCatalogIds,
                keptLooseIds = keptLooseIds,
                keptCountry = shelf.issuer,
            )
        }
    }
    val yearModel = remember(state, keptCatalogIds, shelf.axis) {
        if (shelf.axis != NotebookAxis.ByYear) {
            null
        } else {
            // Only pieces in a kept card or kept loose; a hidden card's pieces stay off the axis.
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
    // Recounted when a switch or the narrowing changes. Kept outside the grid: a lazy item is
    // disposed when it scrolls off, and would resolve every plate again on scrolling back.
    val preview = remember(configuring, shown, looseShown, draft) {
        if (!configuring) {
            null
        } else {
            ExportPreview(
                // The loose-coin lámina counts as one card.
                cards = shown.size + if (draft.unclaimed && looseShown.isNotEmpty()) 1 else 0,
                pages = notebook(shown, looseShown, draft),
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        // Computed here rather than with GridCells.Adaptive because the heading needs it too. The
        // country and year axes are a single column of blocks.
        val columns = when (shelf.axis) {
            NotebookAxis.ByPlate -> indexColumns(maxWidth)
            NotebookAxis.ByCountry, NotebookAxis.ByYear -> 1
        }


        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = PAGE_MARGIN, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(INDEX_GUTTER),
            // 6 dp leaves 3.68 rows (11 cards) above the fold on a Pixel 7.
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

            // «Lo que busco» at the top, with its casillas drawn (#520): a shopping list for a fair
            // is the most actionable thing here, and the foot of the index is several screens down
            // (ADR 0026 §8 clause 3, amended). Absent when nothing is marked, as in #418.
            if (wishes.isNotEmpty()) {
                fullWidth {
                    AnnexDoor(
                        label = wishDoorLabel(wishes.size),
                        // The search doesn't narrow its count, and the note says so rather than
                        // look stale (#515). Only this row carries it, not the one at the foot.
                        note = wishDoorNote(searching = query.isNotBlank()),
                        onOpen = onOpenWishes,
                    ) {
                        WishedCoins(wishes = wishes, images = state.images)
                    }
                }
            }

            // The print switches and their cost, before any page is drawn (#228), in the slot the
            // progress card later takes.
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
                                // The folio's first plate; it may share the folio (#232).
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
                        // Greys out «Sin colección» when the narrowing leaves no loose coin.
                        loose = looseShown.size,
                        onChange = { draft = it },
                        onDownload = { begin(ExportDestination.Download) },
                        onShare = { begin(ExportDestination.Share) },
                        onDismiss = { configuring = false },
                    )
                }
            }

            // Progress and a cancel action for a long export (#169).
            printing?.let { job ->
                fullWidth {
                    ExportProgress(
                        step = step,
                        pages = job.pages.size,
                        // Cancellable except while writing, which would close the document under
                        // the thread serializing it.
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

            // When the shelf hides everything, offer the undo here: the folded shelf may hide the
            // responsible chip.
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
                        // Undo only what narrows (#515): filters but not axis or sort, and the
                        // search box only if it has text.
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
                        // Only cards that open a plate have a casilla for the coin to fly to
                        // (ADR 0026 §3).
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

            // The door to «Explorar», last on every axis (ADR 0026 §8 clause 3). One name, one
            // destination (#520); absent at zero.
            showcaseDoorLabel(plates = showcase)?.let { label ->
                fullWidth {
                    AnnexDoor(label = label, onOpen = onOpenShowcase)
                }
            }
        }

        // Outside the grid: a lazy item is disposed when it scrolls off, and would take the export
        // with it.
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
 * A row leading to an annex screen: deeper paper, its name with a count, and a forward arrow.
 *
 * An annex is neither a bar cell nor an index collection, so its entrance is a row, not a card
 * (ADR 0026 §8 clause 3, amended by #520). The index has two (marks at the top, the shelf window at
 * the foot) and «Explorar» a third into «Lo que busco»; all three share this composable so they
 * can't drift apart.
 *
 * The arrow is drawn because neither typeface has the glyph (#298); it is «Volver»'s chevron,
 * mirrored. [content] (the marked casillas on the index's row) sits inside the tap target.
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
        // The note is a sentence, so body text rather than small caps (as in #513), but small so
        // the name stays the loudest thing in the row.
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

/** Marked casillas the index's row draws before switching to a count (#520). */
private const val WISHED_COINS_DRAWN = 3

/** Coin diameter on that row; not a tap target of its own. */
private val WISHED_COIN = 40.dp

/**
 * The marked casillas as coins on the row that opens «Lo que busco» (#520), last marked first.
 *
 * Three and then a count, so each coin stays big enough to recognise. Drawn whole
 * ([HoleAbsence.Wanted]), not as a ghost: these are being hunted, not missing from a plate. No
 * year, name or price: the list behind the row has them.
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
 * What the export sheet would produce: the card count and the pages (#228). Holding the pages
 * themselves lets «Descargar» and «Compartir» print exactly what the sheet described.
 */
private data class ExportPreview(val cards: Int, val pages: List<PrintPage>)

/** A running export: the pages frozen at the tap, and Descargas or share (#285). */
private data class NotebookJob(
    val pages: List<PrintPage>,
    val destination: ExportDestination,
)

/**
 * The lazy-grid key: the box id, or the variant key of a derived collection, i.e. what its route is
 * addressed by (ADR 0021 §5).
 */
private fun cardKey(card: IndexCard): String = when (card) {
    is IndexCard.Derived -> "derived-${card.key}"
    is IndexCard.Box -> "box-${card.box.id}"
}

/**
 * One collection card. A single composable for every kind, since ADR 0021 §2 makes them identical
 * on screen; anything that varies is drawn from what the card has, never from its kind.
 *
 * Under the hole only the name and its ratio or count remain (ADR 0026 §12). The whole card is one
 * target that opens the collection (ADR 0021 §9).
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
        // Simple + Auto wraps at spaces and hyphens, hyphenating by dictionary. HighQuality split
        // «Ibero-American» as «Ibero-America» / «n» on the three-column card (#405).
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
 * The chip rows of Colecciones: axis (what a cell is, ADR 0026 §9), then sort (why a card is on
 * top, ADR 0021 §6), then the filters.
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

/** An item spanning every column, for headings and notices. */
private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}
