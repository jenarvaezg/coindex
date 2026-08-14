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
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.NUMISTA_SOURCE_LINK
import com.jenarvaezg.coindex.ui.PLATE_UNAVAILABLE_EYEBROW
import com.jenarvaezg.coindex.ui.PlateMoney
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.HoleStamp
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.components.StampedRatio
import com.jenarvaezg.coindex.ui.components.YearTagMetrics
import com.jenarvaezg.coindex.ui.components.rememberInkFall
import com.jenarvaezg.coindex.ui.components.travellingCoin
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.plateEntriesBesideRatio
import com.jenarvaezg.coindex.ui.plateFileName
import com.jenarvaezg.coindex.ui.plateSheetTally
import com.jenarvaezg.coindex.ui.plateSubject
import com.jenarvaezg.coindex.ui.plateUnavailableLabel
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.printedName
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * The plate of a followed collection against its curated catalog.
 *
 * Owned members are shown at full colour; missing ones keep their catalog design as a 14% ghost
 * inside a dotted die-cut rule. Every issued member links to its Numista page.
 *
 * The plate is worded once, here, and the grid and the exported sheet are handed the same
 * [PlateSubject] (#218): the specification used to be rebuilt in the body of the lazy grid on every
 * recomposition, and once more, in parallel, by the sheet while the export was in flight.
 */
@Composable
fun PlateScreen(
    result: PlateResult,
    images: Map<Int, TypeImages>,
    /**
     * What this plate is worth, what closing it would cost and what each hole costs — or nothing at
     * all while the market has not landed (ADR 0028 §7).
     *
     * A function of the resolution and not a value, because the plate is resolved here: the album the
     * three readings walk only exists on the other side of `result`.
     */
    money: (PlateResult.Available) -> PlateMoney,
    /** The marks on this plate's casillas, and the gesture that toggles one (ADR 0029 §5). */
    marking: PlateMarking,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    onMessage: (UiNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (result) {
        is PlateResult.Unavailable -> UnavailablePlate(result.reason, modifier)
        is PlateResult.Available -> AvailablePlate(
            plate = remember(result, money, marking.wished) {
                plateSubject(result, money(result), marking.wished)
            },
            marking = marking,
            images = images,
            notebookOptions = notebookOptions,
            onNotebookPrinted = onNotebookPrinted,
            notebookPages = notebookPages,
            onExporting = onExporting,
            onOpenSource = onOpenSource,
            onMessage = onMessage,
            modifier = modifier,
        )
    }
}

/**
 * The wish gesture as a plate receives it: what is marked, and what toggling one mark does.
 *
 * One parameter rather than two because the two are halves of one subject, and a screen handed them
 * apart could draw marks it cannot toggle. **Not a `data class` and not compared**, which is where it
 * differs from [PlateMoney]: it holds a lambda, so equality would be about the lambda's identity, and
 * what the plate keys its subject on is [wished] — the set — and never the holder.
 *
 * **Whether the mode is open is not in here** — that is the screen's own state, like the export panel's
 * (ADR 0029 §5): nothing outside this plate needs to know the collector is marking, and a mode kept in
 * the ViewModel would still be open on the next plate they walk into.
 */
class PlateMarking(
    val wished: Set<WishKey>,
    val onToggle: (WishKey) -> Unit,
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
    onMessage: (UiNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Held here and not in the header of the grid, which is an item and is disposed on the way down:
    // the ink falls once per opening of the sheet, and scrolling back up finds it dry (ADR 0026 §3).
    val ink = rememberInkFall(plate.complete)

    // The machine itself is [SheetExportFlow] and lives in one place (#430), and since #431 the
    // drawing does too: what a plate brings is its name, its file and what it says it holds.
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
        )
    }
}

@Composable
private fun PlateGrid(
    plate: PlateSubject,
    marking: PlateMarking,
    images: Map<Int, TypeImages>,
    ink: State<Float>,
    export: SheetExportSurface,
    onOpenSource: (String) -> Unit,
) {
    // Whether the collector is marking right now, which is this screen's own state and nothing else's
    // (ADR 0029 §5). It survives a scroll because it is held outside the lazy grid, and it does not
    // survive leaving the plate: the mode is the gesture, not a setting.
    var picking by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Which casilla the sheet opens on depends on how many of them share a row, and the grid
        // will not say it until it measures, so the same arithmetic it uses is read off the width
        // here. Nothing about the name is decided from it any more: see [PlateCell] (#473).
        val columns = plateColumns(maxWidth - PLATE_MARGIN * 2)
        // Where the sheet opens is the grid's **initial state** and not an effect that runs on it,
        // which is the whole of #396: see [plateOpeningItem].
        val grid = rememberLazyGridState(
            initialFirstVisibleItemIndex = plateOpeningItem(plate.landingCell, columns),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            state = grid,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PLATE_MARGIN, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
            // Wider than the gutter, and it is the sheet's proximity and not its air: see
            // [PlateSpacing]. Two members side by side are never confused; two rows were.
            verticalArrangement = Arrangement.spacedBy(PlateSpacing.rowGap),
        ) {
            // The one item of this grid that is not a casilla, and what [PLATE_LEAD_ITEMS] counts.
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow(CURATED_CATALOG_EYEBROW)
                    PlateHeading(
                        title = plate.title,
                        ratio = plate.ratio,
                        complete = plate.complete,
                        ink = ink,
                    )
                    PlateMoneyLines(value = plate.value, cost = plate.cost)
                    SpecificationCard(
                        entries = plateEntriesBesideRatio(plate.entries),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // One door into «Cómo se exporta», the same shape the index has (#434): the
                    // panel owns Descargar / Compartir / Cancelar, and asking the destination
                    // twice — once on the way in and once on the way out — was the whole defect.
                    PrimaryAction(
                        text = export.label,
                        onClick = export.onExport,
                        enabled = export.enabled,
                    )
                    export.options?.invoke()
                    export.progress?.invoke()
                    // The door into the marking mode, last thing before the casillas it is about
                    // (ADR 0029 §5). Absent on a plate with nothing left to look for: a closed plate
                    // has no empty casilla, and a word offering to mark nothing is furniture.
                    if (plate.cells.any { it.missing && it.wishKey != null }) {
                        CardAction(
                            text = if (picking) {
                                WishLabels.MARK_DONE_ACTION
                            } else {
                                WishLabels.MARK_ACTION
                            },
                            onClick = { picking = !picking },
                        )
                        if (picking) {
                            Text(
                                WishLabels.MARK_HINT,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Paper.muted,
                            )
                        }
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
                    // Where the coin of the index card is flying to, and nowhere else: it is the
                    // same casilla the plate is scrolled to, so the landing is the one thing the
                    // journey promised — «es la misma moneda» (ADR 0026 §3).
                    travellingFrom = plate.catalogId.takeIf { index == plate.landingCell },
                    onOpenSource = onOpenSource,
                    // Only while the mode is open, which is what turns the body of an empty hole
                    // from «turn the coin over» into «lo busco» and back (ADR 0029 §5).
                    onMark = if (picking) marking.onToggle else null,
                )
            }
        }
    }
}

/** The page margin the plate keeps on both sides, and what the columns are measured inside. */
private val PLATE_MARGIN = 20.dp

/**
 * How many casillas `GridCells.Adaptive(104.dp)` will put on a row of [available] width.
 *
 * The grid keeps this to itself until it measures, and the plate has to know it one step earlier,
 * to open on the right casilla — see [plateOpeningItem]. Same arithmetic, said out loud and tested.
 *
 * It used to answer a second question, which was which casillas shared a name box (#337, #412).
 * Nobody asks that any more: since #473 the tags of a row line up because they all hang off their
 * own hole by the same [PlateSpacing.underTheHole], and geometry needs no arithmetic to agree.
 */
internal fun plateColumns(
    available: Dp,
    minimum: Dp = 104.dp,
    gutter: Dp = PlateMetrics.gutter,
): Int = maxOf(1, ((available + gutter) / (minimum + gutter)).toInt())

/**
 * The plate's own heading: the title, and the ratio raised out of the specification onto it.
 *
 * The figure sits at the top right because that is where the stamp lands (#304): the ceremony eats
 * the datum that was already on the sheet instead of adding a line of its own, and it fires on
 * opening — when you are at the top — so a stamp anywhere further down would be pressed off screen.
 *
 * A plate with no measurable denominator has no figure to raise, and then the title takes the width.
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
 * The two figures of money a plate can carry, in two lines of the same weight (#493).
 *
 * **The hierarchy is not in the type size, it is in the words** — the proportions that decided it are
 * in `FiguresLabels.PLATE_VALUE_LABEL`, where the words themselves are. What this composable owes them
 * is the one thing a label cannot say: that neither line is drawn smaller than the other.
 *
 * The second line is **absent** on a closed plate rather than zero, and absent over the threshold of
 * ADR 0028 §1 where those prices were never asked for. What is left then is one named line, and it
 * reads as well alone as in company.
 *
 * Four dp between them and not the ten the header spaces everything else by: the two lines are one
 * statement about money, and at ten they read as two blocks that happen to be adjacent.
 */
@Composable
internal fun PlateMoneyLines(value: String?, cost: String?) {
    if (value == null && cost == null) return
    Column(verticalArrangement = Arrangement.spacedBy(PLATE_MONEY_LINE_GAP)) {
        listOfNotNull(value, cost).forEach { line ->
            Text(
                line,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.rust,
            )
        }
    }
}

/** What separates the two figures of money, which are one statement and not two blocks. */
internal val PLATE_MONEY_LINE_GAP = 4.dp

/** Whatever the grid holds ahead of the casillas: the heading, today, as one spanning item. */
private const val PLATE_LEAD_ITEMS = 1

/**
 * The item the sheet opens on, so that the casilla the coin is flying to is there to receive it.
 *
 * The four Bolívares the father owns are casillas 19 to 22 of 22, so a plate that always opened at
 * the top would promise «es la misma moneda» and then land it below the fold (#304).
 *
 * **This is the grid's initial state and not a scroll performed on it**, which is the whole of #396.
 * The jump used to be a `LaunchedEffect` that waited for a layout to ask what was visible, and by
 * then it was a frame late for the one thing that depended on it: on the frame Compose matches the
 * two ends of the journey the landing casilla was not composed yet, so the coin had somewhere to
 * take off from and nowhere to land, and the flight in never happened. The way home never failed,
 * because by then the sheet had been parked at the casilla for a while. Nine journeys were filmed
 * and the split was exact: the plates that had to jump were the plates whose coin did not fly.
 *
 * **Nothing moves when the landing is on the first row**, which is every complete plate: the first
 * casilla a complete sheet owns *is* its first casilla, so the ceremony falls where the eye is. The
 * test is arithmetic — [columns] is the same count the grid measures with (#337) — because a
 * question answered by measuring can only be answered a frame too late.
 */
internal fun plateOpeningItem(landingCell: Int?, columns: Int): Int =
    if (landingCell == null || landingCell < columns) 0 else PLATE_LEAD_ITEMS + landingCell

/**
 * One casilla, read downwards: the hole, the year sunk into the cardboard, and the name (#473).
 *
 * **The tag hangs off the hole and the name hangs off the tag**, which is where an album sheet puts
 * its label and what the #411 ticket had named as the alternative it did not take. The order the
 * plate had until then put the name in between, and it cost a whole apparatus: the tags of a row
 * only lined up if every casilla on it reserved a box of one height (#337), measured per row against
 * real Bitter (#412) — and a casilla with no name reserved that box **empty**, which hung 54 dp of
 * bare cardboard between its coin and its year against the 42 dp that separate two rows. That is the
 * inversion #473 reported, and there was no width of gap that could close it: the box was measured
 * in `sp` and the gap in `dp`, so enlarging the type reopened it every time.
 *
 * This way round nobody measures anything. Every tag is [PlateSpacing.underTheHole] under its own
 * hole, so a row shares a baseline by construction; what a name does not use falls at the **foot**
 * of the casilla, where the grid adds it to the gap between rows instead of subtracting it from it.
 */
@Composable
internal fun PlateCell(
    cell: DrawnCell,
    images: TypeImages?,
    printedSide: PrintedSide,
    /** The catalog whose card this casilla receives the coin from, and null for every other one. */
    travellingFrom: String?,
    onOpenSource: (String) -> Unit,
    /**
     * What toggling this casilla's mark does, while the marking mode is open (ADR 0029 §5).
     *
     * Null everywhere else, and that is the whole of the mode inside a cell: the body of a hole has
     * **one** target at a time, so while the collector is marking it marks, and the rest of the time it
     * turns the coin over as it always did. Two gestures on one 104 dp hole would be a long press
     * nobody announced.
     */
    onMark: ((WishKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // An announced member has no Numista page to open: the coin is not in the catalogue.
    val open = cell.numistaTypeId?.let { typeId -> { onOpenSource(numistaTypeUrl(typeId)) } }
    // Only an empty casilla the app can name: a full one is not something you are looking for, and an
    // announced design has no coin to look for (ADR 0029 §1).
    val mark = cell.wishKey
        ?.takeIf { cell.missing }
        ?.let { key -> onMark?.let { toggle -> { toggle(key) } } }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        // The hole and what is laid inside it. The chip is drawn here and not by `AlbumHole`,
        // which the axes and the loose coins share: what a casilla costs is the plate's business,
        // and the hole is the same hole it was (#493).
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
                missing = cell.missing,
                // Two targets on a casilla and not one (#302): the body of the hole turns the coin
                // over, and the year under it goes out to Numista. **Unless it is being marked**, and
                // then the far face is withheld rather than the tap overridden: `AlbumHole` takes its
                // tap from having a second face, so this is what hands the body over to the mark
                // without giving the hole a second rule about which of two things a press means.
                otherSide = if (mark == null) images?.printedPhoto(printedSide.other) else null,
                modifier = Modifier
                    .size(104.dp)
                    .travellingCoin(travellingFrom),
            )
            HoleStamp(cost = cell.cost, wished = cell.wished)
        }
        // The tag's own target, reserved whether or not there is a tag to put in it: an announced
        // member has no year, and one that has a year but no Numista page does not buy the 48 dp
        // that `minimumInteractiveComponentSize` gives the rest. Either would otherwise pull its
        // name up against the hole while its neighbours' stayed down. It is a constant and it is
        // not measured — which is the whole point of this order.
        Box(
            modifier = Modifier.height(YearTagMetrics.target),
            contentAlignment = Alignment.Center,
        ) {
            cell.year?.let { year -> RecessedYearTag(year = year, onOpen = open) }
        }
        cell.printedName?.let { name -> PlateCellName(name = name) }
    }
}

/**
 * The name at the foot of a casilla, under the year it glosses.
 *
 * The plate is the last of the three surfaces that print a name under a hole to get the autosize
 * ladder: the index card since #348 and the Coins cartouche since #350, while the cell of the plate
 * let the name decide its own height. It does again, and this time nobody pays for it — since #473
 * the name is the **last** thing the casilla prints, so a tall one lengthens its own row and a short
 * one leaves its blank where the gap between rows already is. The reservation that used to make the
 * tags of a row share a baseline is gone with the order that needed it: see [PlateCell].
 *
 * The label is the curator's and is never shortened here: 1.082 of the 1.184 print a name, and
 * many are legitimate descriptions of the issue. What gives is the type on screen.
 *
 * **Three lines is still the cut**, and the reason is now the paper and not the cardboard: #412 set
 * the rule that no name the screen prints whole may be an ellipsis in the notebook, and the printed
 * cartouche has 16 mm that the page count is measured from (#350). Of the 1.082 names, 18 need a
 * fourth line or a fifth, and they are cut on both surfaces alike.
 *
 * It is plain ink and no longer a link: what opens Numista is the year's recessed tag above it
 * (#302), and a title that kept its arrow would print twenty-two of them in the system typeface on a
 * sheet of paper — neither Bitter nor Barlow has that glyph (#298).
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
 * What a casilla prints of a name before it cuts, and what the notebook can print too (#412).
 *
 * A fourth line no longer costs the plate any cardboard — it costs the **paper**, which cannot grow
 * its cartouche without moving the page count, and parity is the rule: 18 of 1.082 names are cut,
 * and they are cut on both surfaces.
 */
private const val PLATE_CELL_NAME_MAX_LINES = 3

/** The smallest Bitter a casilla prints before it gives up and cuts. */
private val PLATE_CELL_NAME_MIN_SIZE = 13.sp

/** Bitter shrinks before the cell cuts, the same ladder the index card walks down (#348). */
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
