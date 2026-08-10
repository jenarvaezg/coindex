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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.components.ShareGlyph
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
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics
import kotlinx.coroutines.flow.first

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
    onOpenSource: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (result) {
        is PlateResult.Unavailable -> UnavailablePlate(result.reason, modifier)
        is PlateResult.Available -> AvailablePlate(
            plate = remember(result) { plateSubject(result) },
            images = images,
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
    onOpenSource: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exporting by remember { mutableStateOf<ExportDestination?>(null) }
    // Held here and not in the header of the grid, which is an item and is disposed on the way down:
    // the ink falls once per opening of the sheet, and scrolling back up finds it dry (ADR 0026 §3).
    val ink = rememberInkFall(plate.complete)

    Box(modifier = modifier) {
        PlateGrid(
            plate = plate,
            images = images,
            ink = ink,
            exporting = exporting != null,
            onOpenSource = onOpenSource,
            onDownload = { exporting = ExportDestination.Download },
            onShare = { exporting = ExportDestination.Share },
        )
        // The whole export cycle is [SheetExport] (#219): what a plate contributes is the four
        // values that make it a plate rather than a sheet of pieces. Descargas is the default;
        // the share sheet is the secondary action on the same heading (#285).
        exporting?.let { destination ->
            SheetExport(
                key = plate.catalogId,
                items = plate.cells,
                images = images,
                typeId = { it.numistaTypeId },
                printedSide = plate.printedSide,
                sheet = SharedSheet.PLATE,
                tally = plateSheetTally(plate.cells.size),
                fileName = plateFileName(plate.catalogId),
                destination = destination,
                onFinished = { message ->
                    exporting = null
                    onMessage(message)
                },
            ) { layout, onImageSettled, recording ->
                PlateSheet(
                    plate = plate,
                    images = images,
                    layout = layout,
                    onImageSettled = onImageSettled,
                    modifier = recording,
                )
            }
        }
    }
}

@Composable
private fun PlateGrid(
    plate: PlateSubject,
    images: Map<Int, TypeImages>,
    ink: State<Float>,
    exporting: Boolean,
    onOpenSource: (String) -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Which cells share a row is what decides where the tags line up, and the grid will not say
        // it until it measures, so the same arithmetic it uses is read off the width here (#337).
        val columns = plateColumns(maxWidth - PLATE_MARGIN * 2)
        // A row reserves the name box when one of its cells is named — and not the whole plate,
        // which would hang 54 dp of empty cardboard under the twenty date-run casillas of the
        // 1 Bolívar for the sake of the two titled `1945 (acuñada en 1947)`.
        val namedRows = plate.cells.chunked(columns).map { row -> row.any { it.label != it.year } }
        val grid = rememberLazyGridState()
        OpenWhereTheCoinLands(plate.landingCell, plate.cells.size, grid)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            state = grid,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PLATE_MARGIN, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
            verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Catálogo curado")
                    PlateHeading(
                        title = plate.title,
                        ratio = plate.ratio,
                        complete = plate.complete,
                        ink = ink,
                    )
                    SpecificationCard(
                        entries = plateEntriesBesideRatio(plate.entries),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Descargar is what this screen is for (#285); compartir stays beside it as the
                    // secondary action, because Jose still hands the PNG to another app.
                    PrimaryAction(
                        text = if (exporting) {
                            "Preparando la lámina…"
                        } else {
                            "Descargar lámina"
                        },
                        onClick = onDownload,
                        enabled = !exporting,
                    )
                    CardAction(
                        text = "Compartir",
                        onClick = onShare,
                        enabled = !exporting,
                        icon = { ShareGlyph(color = Paper.ink) },
                    )
                    ExternalLink(
                        text = "Fuente en Numista",
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
                    named = namedRows.getOrElse(index / columns) { false },
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
 * Opens the sheet at the casilla the coin is flying to, when it would otherwise land off screen.
 *
 * The four Bolívares the father owns are casillas 19 to 22 of 22, so a plate that always opened at
 * the top would promise «es la misma moneda» and then land it below the fold (#304). It is only ever
 * a jump the collector never sees, made before the first frame they do: [LazyGridState.scrollToItem]
 * and not `animateScrollToItem`, which would race the shared element it exists to serve.
 *
 * **Nothing moves when the landing is already visible**, which is every complete plate: the first
 * casilla a complete sheet owns *is* its first casilla, so the ceremony falls where the eye is.
 */
@Composable
private fun OpenWhereTheCoinLands(landingCell: Int?, casillas: Int, grid: LazyGridState) {
    LaunchedEffect(landingCell) {
        if (landingCell == null) return@LaunchedEffect
        // The grid has measured nothing yet on the frame this effect runs in, so what is visible is
        // asked for once there is a layout to ask: reading it earlier reports an empty sheet and
        // scrolls every plate, complete ones included.
        val layout = snapshotFlow { grid.layoutInfo }.first { it.totalItemsCount > 0 }
        // Whatever the grid holds that is not a casilla comes first — the heading, today, as one
        // spanning item — so the offset is counted rather than written down as a 1 that a second
        // header would quietly break.
        val item = layout.totalItemsCount - casillas + landingCell
        if (layout.visibleItemsInfo.none { it.index == item }) grid.scrollToItem(item)
    }
}

@Composable
private fun PlateCell(
    cell: DrawnCell,
    images: TypeImages?,
    printedSide: PrintedSide,
    named: Boolean,
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
        if (named) {
            PlateCellName(name = cell.label.takeIf { it != cell.year }.orEmpty())
        }
        cell.year?.let { year -> RecessedYearTag(year = year, onOpen = open) }
    }
}

/**
 * The name under a hole, in the two-line range every cell of the plate reserves.
 *
 * The plate is the last of the three surfaces that print a name under a hole to get this: the
 * index card autosizes and truncates since #348 and the Coins cartouche since #350, while the cell
 * of the plate let the name decide its own height. With 235 of the 1.188 members in `data/` past
 * two lines — 66 of them in one single plate — the year of a row landed on three different
 * baselines and the tallest name pushed its neighbours' apart.
 *
 * The label is the curator's and is never shortened here: 1.086 of those 1.188 are not a year, and
 * many are legitimate descriptions of the issue. What gives is the type on screen.
 *
 * It is plain ink and no longer a link: what opens Numista is the year's recessed tag underneath
 * (#302), and a title that kept its arrow would print twenty-two of them in the system typeface on a
 * sheet of paper — neither Bitter nor Barlow has that glyph (#298).
 *
 * [name] is empty for a casilla whose label is already its year: the box stays, so the tags of a
 * row still share a baseline.
 */
@Composable
internal fun PlateCellName(name: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.titleMedium
    // Reserved in dp and not in lines: two lines of a name that shrank to 13 sp are shorter than
    // two lines of one that did not, so `minLines` alone still left three years of a row on three
    // baselines. The box is always the two tallest lines the cell can print.
    val reserved = with(LocalDensity.current) {
        style.lineHeight.toDp() * PLATE_CELL_NAME_LINES + NAME_PADDING * 2
    }
    Box(
        modifier = modifier.fillMaxWidth().height(reserved),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = name,
            style = style,
            textAlign = TextAlign.Center,
            autoSize = PLATE_CELL_NAME_AUTO_SIZE,
            maxLines = PLATE_CELL_NAME_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = NAME_PADDING),
        )
    }
}

/** Every cell reserves the same name range, so a row's years share one baseline. */
private const val PLATE_CELL_NAME_LINES = 2

/** The breathing room the name keeps between the hole above it and the tag below. */
private val NAME_PADDING = 6.dp

/** Bitter shrinks before the cell cuts, the same ladder the index card walks down (#348). */
private val PLATE_CELL_NAME_AUTO_SIZE = TextAutoSize.StepBased(
    minFontSize = 13.sp,
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
        Eyebrow("Lámina no disponible")
        Text(explanation, style = MaterialTheme.typography.bodyLarge)
    }
}
