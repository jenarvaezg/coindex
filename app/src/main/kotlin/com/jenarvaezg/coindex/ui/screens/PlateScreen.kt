package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.CURATED_CATALOG_EYEBROW
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CellPlaque
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.FiguresLabels
import com.jenarvaezg.coindex.ui.NUMISTA_SOURCE_LINK
import com.jenarvaezg.coindex.ui.PLATE_UNAVAILABLE_EYEBROW
import com.jenarvaezg.coindex.ui.PlateFinance
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.HoleAbsence
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.HoleStamp
import com.jenarvaezg.coindex.ui.components.ModeBand
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.RecessedNameTag
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.components.StampedRatio
import com.jenarvaezg.coindex.ui.components.YearTagMetrics
import com.jenarvaezg.coindex.ui.components.outsideTheMode
import com.jenarvaezg.coindex.ui.components.rememberInkFall
import com.jenarvaezg.coindex.ui.components.sheetUnderMode
import com.jenarvaezg.coindex.ui.components.travellingCoin
import com.jenarvaezg.coindex.ui.plateEntriesBesideRatio
import com.jenarvaezg.coindex.ui.plateFileName
import com.jenarvaezg.coindex.ui.plateSheetTally
import com.jenarvaezg.coindex.ui.plateSubject
import com.jenarvaezg.coindex.ui.showcaseValueAction
import com.jenarvaezg.coindex.ui.plateUnavailableLabel
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.printedName
import com.jenarvaezg.coindex.ui.printedFaces
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * The plate of a collection against its curated catalog.
 *
 * Owned members are in full colour; missing ones show the catalog design as a faint ghost inside a
 * dotted die-cut rule. A casilla's year opens that coin's sheet in the app (#508), and the sheet
 * links out to Numista.
 *
 * The plate is worded once here as a [PlateSubject] and handed to both the grid and the exported
 * sheet (#218).
 */
@Composable
fun PlateScreen(
    result: PlateResult,
    images: Map<Int, TypeImages>,
    /**
     * The plate's value, closing cost, per-hole costs and tasación cost, or nothing until market
     * prices arrive (ADR 0028 §7). Asked with [result] because the album those readings walk lives
     * there.
     *
     * One object rather than lambdas because the subject below is keyed on it: a lambda literal is
     * new on every recomposition and would re-walk the album each frame of the entrance (#541).
     */
    finance: PlateFinance,
    /** The marks on this plate's casillas and how to toggle one (ADR 0029 §5). */
    marking: PlateMarking,
    /**
     * Whether this plate's tasación is running (ADR 0030 §3). Kept out of [finance] because it
     * flips twice per press without any amount changing, and would rebuild it and its album walk
     * (#541).
     */
    valuing: Boolean,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    /** The coin sheet a casilla's year opens over this lámina (#508). */
    sheet: CoinSheetSurface,
    onMessage: (UiNotice) -> Unit,
    /** For the age of a hand-asked price (ADR 0030 §4); read once per opening. */
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    when (result) {
        is PlateResult.Unavailable -> UnavailablePlate(result.reason, modifier)
        is PlateResult.Available -> {
            // [finance] is built from the marks and the clock, so its identity covers both.
            val plate = remember(result, finance) {
                plateSubject(result, finance.money(result), marking.wished, nowMillis)
            }
            // Screen-local, like the marking mode (ADR 0029 §5), so a sheet left open doesn't
            // reappear on the next lámina.
            var openTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
            Box(modifier = modifier) {
                // ADR 0030 §6: the collector's plates get the export flow; shelf-window plates get
                // the tasación button instead, since there is nothing of theirs to export.
                if (plate.mine) {
                    AvailablePlate(
                        plate = plate,
                        marking = marking,
                        images = images,
                        notebookOptions = notebookOptions,
                        onNotebookPrinted = onNotebookPrinted,
                        notebookPages = notebookPages,
                        onExporting = onExporting,
                        onOpenSource = onOpenSource,
                        onOpenCoin = { typeId -> openTypeId = typeId },
                        onMessage = onMessage,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ShowcasePlateSheet(
                        plate = plate,
                        marking = marking,
                        valuation = PlateValuation(
                            // Walks the album, so it is remembered.
                            calls = remember(result, finance) { finance.calls(result) },
                            running = valuing,
                            onValue = { finance.press(result) },
                        ),
                        images = images,
                        onOpenSource = onOpenSource,
                        onOpenCoin = { typeId -> openTypeId = typeId },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                CoinSheetOverlay(
                    typeId = openTypeId,
                    surface = sheet,
                    // The plate's `printed_side` up, as on the casilla (ADR 0020, #227).
                    faces = { typeId -> printedFaces(images[typeId], plate.printedSide) },
                    onDismiss = { openTypeId = null },
                    // Don't link back to this same lámina from its own casilla's sheet.
                    here = CardDestination.Plate(plate.catalogId),
                )
            }
        }
    }
}

/**
 * A shelf-window plate (ADR 0030): the same [PlateGrid] without a [SheetExportSurface], with the
 * tasación button in the export's place. No completion stamp: a plate at 0/N can't complete.
 */
@Composable
private fun ShowcasePlateSheet(
    plate: PlateSubject,
    marking: PlateMarking,
    valuation: PlateValuation,
    images: Map<Int, TypeImages>,
    onOpenSource: (String) -> Unit,
    onOpenCoin: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PlateGrid(
            plate = plate,
            marking = marking,
            images = images,
            ink = remember { mutableStateOf(0f) },
            export = null,
            valuation = valuation,
            onOpenSource = onOpenSource,
            onOpenCoin = onOpenCoin,
        )
    }
}

/**
 * The marked casillas and how to toggle one, together so a screen can't draw marks it can't toggle.
 * Not a `data class`: it holds a lambda, so the plate keys on [wished], not on this holder.
 *
 * Whether the marking mode is open is screen state, not here (ADR 0029 §5), so it doesn't carry
 * over to the next plate.
 */
class PlateMarking(
    val wished: Set<WishKey>,
    val onToggle: (WishKey) -> Unit,
)

/**
 * The tasación button on a shelf-window plate (ADR 0030 §3). [calls] is the cost shown before
 * pressing (#282, ADR 0028 §3). Whether the plate already has an amount («Volver a tasar») comes
 * from `PlateSubject.entry`, the same value the header shows.
 *
 * Built by the screen from [PlateFinance] and `valuing` separately: [calls] walks the album and is
 * remembered, [running] flips on every press and must not be.
 */
class PlateValuation(
    val calls: Int,
    val running: Boolean,
    val onValue: () -> Unit,
)

@Composable
private fun AvailablePlate(
    plate: PlateSubject,
    marking: PlateMarking,
    images: Map<Int, TypeImages>,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    onOpenCoin: (Int) -> Unit,
    onMessage: (UiNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Held outside the grid, whose header item is disposed on scroll: the stamp falls once per
    // opening and is already there on scrolling back (ADR 0026 §3).
    val ink = rememberInkFall(plate.complete)

    // Export flow and drawing are shared (#430, #431); this supplies the name, file and tally.
    SheetExportFlow(
        sheet = SharedSheet.PLATE,
        key = plate.catalogId,
        fileName = plateFileName(plate.catalogId),
        notebookOptions = notebookOptions,
        onNotebookPrinted = onNotebookPrinted,
        notebookPages = notebookPages,
        onExporting = onExporting,
        onMessage = onMessage,
        tally = plateSheetTally(plate.cells.size),
        modifier = modifier,
    ) { export ->
        PlateGrid(
            plate = plate,
            marking = marking,
            images = images,
            ink = ink,
            export = export,
            onOpenSource = onOpenSource,
            onOpenCoin = onOpenCoin,
        )
    }
}

/**
 * The sheet with the marking-mode band under it (#517).
 *
 * The mode is held here, outside the lazy grid, so it survives scrolling but not leaving the plate
 * (ADR 0029 §5). The band is a row of this column, not an overlay, so it never covers the last row
 * of holes; it holds the hint and the way out.
 */
@Composable
private fun PlateGrid(
    plate: PlateSubject,
    marking: PlateMarking,
    images: Map<Int, TypeImages>,
    ink: State<Float>,
    /** Null on a shelf-window plate. */
    export: SheetExportSurface?,
    /** Null on the collector's own plates. */
    valuation: PlateValuation? = null,
    /** The plate's «Fuente en Numista», the only link out of the app. */
    onOpenSource: (String) -> Unit,
    /** Opens the coin's sheet from a casilla's year (#508). */
    onOpenCoin: (Int) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        PlateSheet(
            plate = plate,
            marking = marking,
            images = images,
            ink = ink,
            export = export,
            valuation = valuation,
            onOpenSource = onOpenSource,
            onOpenCoin = onOpenCoin,
            picking = picking,
            onPicking = { picking = it },
            modifier = Modifier.weight(1f),
        )
        if (picking) {
            ModeBand(sentence = WishLabels.MARK_HINT) {
                CardAction(
                    text = WishLabels.MARK_DONE_ACTION,
                    onClick = { picking = false },
                )
            }
        }
    }
}

@Composable
private fun PlateSheet(
    plate: PlateSubject,
    marking: PlateMarking,
    images: Map<Int, TypeImages>,
    ink: State<Float>,
    export: SheetExportSurface?,
    valuation: PlateValuation?,
    onOpenSource: (String) -> Unit,
    onOpenCoin: (Int) -> Unit,
    /** Whether the marking mode is open (#517). */
    picking: Boolean,
    onPicking: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.sheetUnderMode(picking)) {
        // The opening casilla depends on the column count, which the grid only knows after
        // measuring; compute it the same way here.
        val columns = plateColumns(maxWidth - PLATE_MARGIN * 2)
        // An initial state, not a scroll effect (#396): see [plateOpeningItem].
        val grid = rememberLazyGridState(
            initialFirstVisibleItemIndex = plateOpeningItem(plate.landingCell, columns),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            state = grid,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PLATE_MARGIN, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
            // Wider than the gutter so rows don't run together: see [PlateSpacing].
            verticalArrangement = Arrangement.spacedBy(PlateSpacing.rowGap),
        ) {
            // The only non-casilla item, counted by [PLATE_LEAD_ITEMS].
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow(CURATED_CATALOG_EYEBROW)
                    PlateHeading(
                        title = plate.title,
                        ratio = plate.ratio,
                        complete = plate.complete,
                        ink = ink,
                    )
                    PlateMoneyLines(
                        value = plate.value,
                        cost = plate.cost,
                        entry = plate.entry ?: plate.entryNote,
                        waiting = plate.moneyWaiting,
                    )
                    SpecificationCard(
                        entries = plateEntriesBesideRatio(plate.entries),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // One button into «Cómo se exporta», as in the index (#434); the panel asks
                    // the destination. Hidden while the panel is open (#512).
                    export?.let { surface ->
                        SheetExportDoorButton(surface.door)
                        surface.options?.invoke()
                        surface.progress?.invoke()
                    }
                    // In the export's slot on a shelf-window plate (ADR 0030 §3).
                    valuation?.let { gesture ->
                        PrimaryAction(
                            text = showcaseValueAction(
                                calls = gesture.calls,
                                // Asked, not priced: a plate Numista has no price for still counts
                                // as valued.
                                valued = plate.entryValued,
                                valuing = gesture.running,
                            ),
                            onClick = gesture.onValue,
                            enabled = !gesture.running,
                        )
                    }
                    // Enters the marking mode (ADR 0029 §5); only when some casilla is markable.
                    // Once open, the hint and exit live in the band at the foot (#517).
                    if (!picking && plate.cells.any { it.missing && it.wishKey != null }) {
                        CardAction(
                            text = WishLabels.MARK_ACTION,
                            onClick = { onPicking(true) },
                        )
                    }
                    ExternalLink(
                        text = NUMISTA_SOURCE_LINK,
                        onClick = { onOpenSource(plate.source) },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            itemsIndexed(plate.cells, key = { _, cell -> cell.id }) { index, cell ->
                PlateCell(
                    cell = cell,
                    images = cell.numistaTypeId?.let { images[it] },
                    printedSide = plate.printedSide,
                    // The index card's coin lands only on the casilla the plate opens on
                    // (ADR 0026 §3).
                    travellingFrom = plate.catalogId.takeIf { index == plate.landingCell },
                    onOpenCoin = onOpenCoin,
                    // While the mode is open, tapping an empty hole marks it instead of flipping
                    // the coin (ADR 0029 §5).
                    onMark = if (picking) marking.onToggle else null,
                    picking = picking,
                )
            }
        }
    }
}

/** Horizontal page margin; columns are measured inside it. */
private val PLATE_MARGIN = 20.dp

/**
 * How many casillas `GridCells.Adaptive(104.dp)` puts on a row of [available] width, computed ahead
 * of measurement so the plate can open on the right casilla ([plateOpeningItem]).
 */
internal fun plateColumns(
    available: Dp,
    minimum: Dp = 104.dp,
    gutter: Dp = PlateMetrics.gutter,
): Int = maxOf(1, ((available + gutter) / (minimum + gutter)).toInt())

/**
 * The title and, at the top right, the ratio, which is where the completion stamp lands (#304). The
 * stamp fires on opening, so it has to be at the top. Without a ratio the title takes the width.
 */
@Composable
private fun PlateHeading(
    title: String,
    ratio: String?,
    complete: Boolean,
    ink: State<Float>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        ratio?.let { figure ->
            StampedRatio(ratio = figure, complete = complete, fall = ink)
        }
    }
}

/**
 * The plate's money lines, all at the same size (#493); the wording carries the hierarchy (see
 * `FiguresLabels.PLATE_VALUE_LABEL`).
 *
 * The cost line is absent, not zero, on a closed plate and beyond the ADR 0028 §1 threshold. With
 * [waiting] a single line replaces the figures, never alongside them: no half-done totals
 * (ADR 0028 §7, #519).
 */
@Composable
internal fun PlateMoneyLines(
    value: String?,
    cost: String?,
    entry: String? = null,
    waiting: Boolean = false,
) {
    // Muted, not rust: rust is for amounts (#519).
    if (waiting) {
        Text(
            FiguresLabels.PLATE_MONEY_WAITING,
            style = MaterialTheme.typography.labelLarge,
            color = Paper.muted,
        )
        return
    }
    if (value == null && cost == null && entry == null) return
    Column(verticalArrangement = Arrangement.spacedBy(PLATE_MONEY_LINE_GAP)) {
        // Never all three: the collector's plates have value and cost, shelf-window plates only
        // the entry (ADR 0030 §6).
        listOfNotNull(value, cost, entry).forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.rust,
            )
        }
    }
}

/** Tighter than the header's 10 dp so the money lines read as one statement. */
internal val PLATE_MONEY_LINE_GAP = 4.dp

/** Grid items before the casillas: the heading. */
private const val PLATE_LEAD_ITEMS = 1

/**
 * The grid item the plate opens on, so the casilla the index card's coin flies to is on screen
 * (#304).
 *
 * Used as the grid's initial state, not as a scroll (#396): a scroll lands a frame late, after the
 * shared-element transition has looked for the landing casilla and not found it. Plates whose
 * landing is on the first row, including every complete plate, don't move. [columns] must match
 * the grid's own count (#337).
 */
internal fun plateOpeningItem(landingCell: Int?, columns: Int): Int =
    if (landingCell == null || landingCell < columns) 0 else PLATE_LEAD_ITEMS + landingCell

/**
 * One casilla, top to bottom: the hole, the year sunk into the cardboard, and the name (#473).
 *
 * Every tag sits [PlateSpacing.underTheHole] below its hole, so a row shares a baseline without
 * measuring anything; the name comes last, so its unused height falls at the foot of the casilla
 * and adds to the row gap. With the name between hole and tag, rows needed per-row height
 * reservations that broke with font scaling (#337, #412).
 */
@Composable
internal fun PlateCell(
    cell: DrawnCell,
    images: TypeImages?,
    printedSide: PrintedSide,
    /** The catalog whose index card flies its coin to this casilla; null for all others. */
    travellingFrom: String?,
    /**
     * Opens this coin's sheet from the year tag (#508). It used to open Numista directly, with
     * nothing on the tag to say so (#298, #302).
     */
    onOpenCoin: (Int) -> Unit,
    /**
     * Toggles this casilla's mark while the marking mode is open (ADR 0029 §5); null otherwise.
     * The hole has one tap target at a time: marking in the mode, flipping the coin outside it.
     */
    onMark: ((WishKey) -> Unit)? = null,
    /**
     * Whether the marking mode is open, which is not the same as this casilla being markable
     * (#517). Non-markable casillas step back while it is open: drawn faint, with their year and
     * flip disabled.
     */
    picking: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Only empty casillas with a wish key: not full ones, nor announced designs (ADR 0029 §1).
    val mark = cell.wishKey
        ?.takeIf { cell.missing }
        ?.let { key -> onMark?.let { toggle -> { toggle(key) } } }
    // No tap for an announced member (no Numista type) or while the marking mode is open.
    val open = cell.numistaTypeId
        ?.takeUnless { picking }
        ?.let { typeId -> { onOpenCoin(typeId) } }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().outsideTheMode(picking && mark == null),
    ) {
        // The cost chip is drawn here, not in `AlbumHole`, which other surfaces share (#493).
        Box(
            contentAlignment = Alignment.Center,
            modifier = mark?.let { toggle ->
                Modifier.clickable(
                    role = Role.Checkbox,
                    onClickLabel = WishLabels.MARK_ACTION,
                    onClick = toggle,
                )
            } ?: Modifier,
        ) {
            AlbumHole(
                photo = images?.printedPhoto(printedSide),
                absence = if (cell.missing) HoleAbsence.Missing else HoleAbsence.Filled,
                // The hole flips the coin and the year opens its sheet (#302, #508). `AlbumHole` is
                // tappable only with a second face, so withholding it in the marking mode frees the
                // tap for the mark, on every casilla (#517).
                otherSide = if (!picking) images?.printedPhoto(printedSide.other) else null,
                modifier = Modifier
                    .size(104.dp)
                    .travellingCoin(travellingFrom),
            )
            // In the marking mode, markable holes show a ghost of the chip (#517).
            HoleStamp(cost = cell.cost, wished = cell.wished, markable = mark != null)
        }
        // The tag's target height is reserved even without a tag or without a tap (announced
        // members have no ficha), so names don't ride up against the hole.
        Box(
            // A minimum, not exact (#511): a name tag replacing the year may be taller.
            modifier = Modifier.heightIn(min = YearTagMetrics.target),
            contentAlignment = Alignment.Center,
        ) {
            when (val plaque = cell.plaque) {
                is CellPlaque.Year -> RecessedYearTag(year = plaque.year, onOpen = open)
                is CellPlaque.Name -> RecessedNameTag(name = plaque.name, onOpen = open)
                null -> Unit
            }
        }
        cell.printedName?.let { name -> PlateCellName(name = name) }
    }
}

/**
 * The name at the foot of a casilla, below its year. It shrinks before cutting, like the index
 * card (#348) and the Monedas cartouche (#350); being last, a tall name only lengthens its own row.
 *
 * The curator's label is never shortened here; only the type size gives. It is plain text, not a
 * link: the year tag opens the coin's sheet (#302, #508).
 */
@Composable
internal fun PlateCellName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        autoSize = PLATE_CELL_NAME_AUTO_SIZE,
        maxLines = PLATE_CELL_NAME_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth().padding(vertical = PlateSpacing.namePadding),
    )
}

/**
 * Lines before a casilla's name is cut. Matches what the notebook's fixed 16 mm cartouche can hold
 * (#350), so no name is whole on screen and cut on paper (#412).
 */
private const val PLATE_CELL_NAME_MAX_LINES = 3

/** Smallest name size before cutting. */
private val PLATE_CELL_NAME_MIN_SIZE = 13.sp

/** Same shrink steps as the index card (#348). */
private val PLATE_CELL_NAME_AUTO_SIZE = TextAutoSize.StepBased(
    minFontSize = PLATE_CELL_NAME_MIN_SIZE,
    maxFontSize = 17.sp,
    stepSize = 0.5.sp,
)

@Composable
private fun UnavailablePlate(reason: PlateUnavailable, modifier: Modifier = Modifier) {
    val explanation = plateUnavailableLabel(reason)
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow(PLATE_UNAVAILABLE_EYEBROW)
        Text(explanation, style = MaterialTheme.typography.bodyLarge)
    }
}
