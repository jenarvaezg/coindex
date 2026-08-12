package com.jenarvaezg.coindex.ui.screens

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.CURATED_CATALOG_EYEBROW
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.NUMISTA_SOURCE_LINK
import com.jenarvaezg.coindex.ui.PLATE_UNAVAILABLE_EYEBROW
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.PlateValue
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.components.StampedRatio
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
     * What the coins of this plate are worth, or null while the market has not landed (ADR 0028 §7).
     *
     * A function of the resolution and not a value, because the plate is resolved here: the album it
     * needs to know which casillas are filled only exists on the other side of `result`.
     */
    value: (PlateResult.Available) -> PlateValue?,
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
            plate = remember(result, value) { plateSubject(result, value(result)) },
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

@Composable
private fun AvailablePlate(
    plate: PlateSubject,
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
    images: Map<Int, TypeImages>,
    ink: State<Float>,
    export: SheetExportSurface,
    onOpenSource: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Which cells share a row is what decides where the tags line up, and the grid will not say
        // it until it measures, so the same arithmetic it uses is read off the width here (#337).
        val available = maxWidth - PLATE_MARGIN * 2
        val columns = plateColumns(available)
        // A row reserves the name box when one of its cells is named — and not the whole plate,
        // which would hang 54 dp of empty cardboard under the twenty date-run casillas of the
        // 1 Bolívar for the sake of the two titled `1945 (acuñada en 1947)`. How many lines that
        // box holds is the row's answer too, and it is measured and not guessed (#412).
        val nameLines = rememberPlateNameLines(
            cells = plate.cells,
            columns = columns,
            cellWidth = plateCellWidth(available, columns),
        )
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
                    // The value of what is in these casillas, and never the cost of closing them:
                    // per plate the first is a shopping companion and the second is a plan, and both
                    // become wealth management the moment they are totalled (ADR 0026 §10).
                    plate.value?.let { value ->
                        Text(
                            value,
                            style = MaterialTheme.typography.labelLarge,
                            color = Paper.rust,
                        )
                    }
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
                    nameLines = nameLines.getOrElse(index / columns) { 0 },
                    // Where the coin of the index card is flying to, and nowhere else: it is the
                    // same casilla the plate is scrolled to, so the landing is the one thing the
                    // journey promised — «es la misma moneda» (ADR 0026 §3).
                    travellingFrom = plate.catalogId.takeIf { index == plate.landingCell },
                    onOpenSource = onOpenSource,
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
 * The grid keeps this to itself until it measures, and the plate has to know it one step earlier:
 * which cells share a row is what decides which of them reserve a name box, and a row whose tags
 * do not line up is the whole defect (#337). Same arithmetic, said out loud and tested.
 */
internal fun plateColumns(
    available: Dp,
    minimum: Dp = 104.dp,
    gutter: Dp = PlateMetrics.gutter,
): Int = maxOf(1, ((available + gutter) / (minimum + gutter)).toInt())

/**
 * How wide one casilla of a row of [columns] is, which is the width a name has to fit in.
 *
 * The companion of [plateColumns] and read off the same width for the same reason: the box a name
 * is given is decided before the grid measures, so the plate has to know the column too.
 */
internal fun plateCellWidth(
    available: Dp,
    columns: Int,
    gutter: Dp = PlateMetrics.gutter,
): Dp = (available - gutter * (columns - 1)) / columns

/**
 * How many lines of name each row of casillas reserves: none, or between two and three (#412).
 *
 * **The row decides, and every casilla on it obeys** — the rule #337 bought, because the tags of a
 * row line up exactly when they all hang off boxes of one height. What is new is that the height is
 * the row's own: 54 of the 75 catalogs of `data/` have no name past two lines and are drawn exactly
 * as before, 14 are made whole by the third line, and inside those the rows that hold no long name
 * pay nothing either. «V centenario de la primera vuelta al mundo» used to die in an ellipsis with no
 * gesture anywhere to read the rest of it.
 *
 * **The third line is the last one, and what stops the fourth is the blank and not the name.** Of
 * the 1.082 members that print a name, measured with real Bitter at 13 sp in the 113 dp column,
 * 79 need three lines, 16 need four and 2 need five: cuts go from 97 to 18, and a fourth line would
 * rescue 16 more. What it would cost is `PlateSpacing.reservedNameLine` twice over — a casilla of one
 * line on a row that reserved four hangs 69 dp of empty cardboard above its name, against the 42 dp
 * that separate two rows, where three lines hang 48 and two hang 27. The third line already puts
 * that blank past the gap between rows, and it falls under the hole where the coin above it is the
 * only thing it can belong to (#411, #473). Past three the name is still cut, and still whole in
 * semantics, which is what search and the screen reader read. See `docs/ux/implementacion-412/`.
 *
 * [linesOf] is the text measurer, injected: what a name costs is a question about Bitter at a given
 * width and only Compose can answer it, while the rule on top is arithmetic and testable without a
 * device. A casilla whose label is its year is never asked — see [printedName].
 */
internal fun plateRowNameLines(
    cells: List<DrawnCell>,
    columns: Int,
    linesOf: (String) -> Int,
): List<Int> = cells.chunked(columns).map { row ->
    val needed = row.mapNotNull { it.printedName }.maxOfOrNull(linesOf) ?: return@map 0
    needed.coerceIn(PLATE_CELL_NAME_MIN_LINES, PLATE_CELL_NAME_MAX_LINES)
}

/**
 * [plateRowNameLines] with Bitter actually measured, once per plate and per width.
 *
 * Measured at the **smallest** type the cell will fall back to, because that is the size a name
 * that needs the room ends up at: asking at 17 sp would buy a third line for a name that only
 * wanted the ladder of #348, and every casilla of the plate would pay for it.
 */
@Composable
private fun rememberPlateNameLines(cells: List<DrawnCell>, columns: Int, cellWidth: Dp): List<Int> {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.titleMedium.copy(fontSize = PLATE_CELL_NAME_MIN_SIZE)
    val density = LocalDensity.current
    return remember(cells, columns, cellWidth, style, density, measurer) {
        val width = with(density) { cellWidth.roundToPx() }
        // One plate repeats a name across its casillas — the twenty-two «Onza Troy» of a date run —
        // and Bitter is measured once for each distinct one.
        val measured = mutableMapOf<String, Int>()
        plateRowNameLines(cells, columns) { name ->
            measured.getOrPut(name) {
                measurer.measure(
                    text = name,
                    style = style,
                    constraints = Constraints(maxWidth = width),
                ).lineCount
            }
        }
    }
}

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

@Composable
private fun PlateCell(
    cell: DrawnCell,
    images: TypeImages?,
    printedSide: PrintedSide,
    /** What this casilla's **row** reserves for a name, and zero on a row that prints none. */
    nameLines: Int,
    /** The catalog whose card this casilla receives the coin from, and null for every other one. */
    travellingFrom: String?,
    onOpenSource: (String) -> Unit,
) {
    // An announced member has no Numista page to open: the coin is not in the catalogue.
    val open = cell.numistaTypeId?.let { typeId -> { onOpenSource(numistaTypeUrl(typeId)) } }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AlbumHole(
            photo = images?.printedPhoto(printedSide),
            missing = cell.missing,
            // Two targets on a casilla and not one (#302): the body of the hole turns the coin
            // over, and the year under it goes out to Numista.
            otherSide = images?.printedPhoto(printedSide.other),
            modifier = Modifier
                .size(104.dp)
                .travellingCoin(travellingFrom),
        )
        if (nameLines > 0) {
            PlateCellName(name = cell.printedName.orEmpty(), lines = nameLines)
        }
        cell.year?.let { year -> RecessedYearTag(year = year, onOpen = open) }
    }
}

/**
 * The name under a hole, in the range of lines its row reserved.
 *
 * The plate is the last of the three surfaces that print a name under a hole to get this: the
 * index card autosizes and truncates since #348 and the Coins cartouche since #350, while the cell
 * of the plate let the name decide its own height. With 235 of the 1.188 members in `data/` past
 * two lines — 66 of them in one single plate — the year of a row landed on three different
 * baselines and the tallest name pushed its neighbours' apart. That 235 is the count of #361, before
 * the autosize ladder existed and at the type size it made unnecessary; measured again at 13 sp for
 * #412 it is 97, which is the figure the reservation above is argued from.
 *
 * The label is the curator's and is never shortened here: 1.082 of the 1.184 print a name, and
 * many are legitimate descriptions of the issue. What gives is the type on screen — and since #412
 * the box too, up to a third line where the row asked for one.
 *
 * It is plain ink and no longer a link: what opens Numista is the year's recessed tag underneath
 * (#302), and a title that kept its arrow would print twenty-two of them in the system typeface on a
 * sheet of paper — neither Bitter nor Barlow has that glyph (#298).
 *
 * [name] is empty for a casilla whose label is already its year: the box stays, so the tags of a
 * row still share a baseline.
 */
@Composable
internal fun PlateCellName(
    name: String,
    modifier: Modifier = Modifier,
    lines: Int = PLATE_CELL_NAME_MIN_LINES,
) {
    val style = MaterialTheme.typography.titleMedium
    // Reserved in dp and not in lines: two lines of a name that shrank to 13 sp are shorter than
    // two lines of one that did not, so `minLines` alone still left three years of a row on three
    // baselines. The box is always the tallest lines the cell can print.
    val reserved = with(LocalDensity.current) {
        style.lineHeight.toDp() * lines + PlateSpacing.namePadding * 2
    }
    Box(
        modifier = modifier.fillMaxWidth().height(reserved),
        // The name sits at the **bottom** of what its cell reserved, so that a name of one line
        // and a name of two hand their year the same 16 dp (#411). Top-aligned, the line a short
        // name did not use fell between the name and the year, which left the year floating
        // further from the name it belongs to than from the coins of the next row. The blank
        // rises to under the hole, where the coin above it is the only thing it can belong to.
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = name,
            style = style,
            textAlign = TextAlign.Center,
            autoSize = PLATE_CELL_NAME_AUTO_SIZE,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = PlateSpacing.namePadding),
        )
    }
}

/**
 * What a row reserves when its longest name asks for less, which is 54 of the 75 catalogs.
 *
 * The floor and no longer the whole rule (#412): the two lines are what the plate always reserved,
 * and a row that needs none of the second one keeps it anyway so that nothing shifts where nothing
 * was wrong. What every casilla of a row shares is the reservation, whatever it came out at.
 */
internal const val PLATE_CELL_NAME_MIN_LINES = 2

/** And no row buys a fourth line: 16 casillas saved would cost 21 dp of cardboard (#412). */
private const val PLATE_CELL_NAME_MAX_LINES = 3

/** The smallest Bitter a casilla prints, and therefore the size a name is measured at. */
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
