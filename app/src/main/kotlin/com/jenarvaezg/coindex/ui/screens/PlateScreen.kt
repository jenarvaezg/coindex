package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.DrawnCell
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.ExternalLink
import com.jenarvaezg.coindex.ui.components.Eyebrow
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.SpecificationCard
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.plateFileName
import com.jenarvaezg.coindex.ui.plateScreenEntries
import com.jenarvaezg.coindex.ui.plateSheetTally
import com.jenarvaezg.coindex.ui.plateSubject
import com.jenarvaezg.coindex.ui.plateUnavailableLabel
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
    var exporting by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        PlateGrid(
            plate = plate,
            images = images,
            exporting = exporting,
            onOpenSource = onOpenSource,
            onExport = { exporting = true },
        )
        // The whole export cycle is [SheetExport] (#219): what a plate contributes is the four
        // values that make it a plate rather than a sheet of pieces.
        if (exporting) {
            SheetExport(
                key = plate.catalogId,
                items = plate.cells,
                images = images,
                typeId = { it.numistaTypeId },
                printedSide = plate.printedSide,
                sheet = SharedSheet.PLATE,
                tally = plateSheetTally(plate.cells.size),
                fileName = plateFileName(plate.catalogId),
                onFinished = { message ->
                    exporting = false
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
    exporting: Boolean,
    onOpenSource: (String) -> Unit,
    onExport: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
        verticalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow("Catálogo curado")
                Text(plate.title, style = MaterialTheme.typography.headlineMedium)
                SpecificationCard(
                    entries = plateScreenEntries(plate.entries),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Exporting the plate is what this screen is for, so it is the only filled
                // button on it; as a bare text button it read as another section heading.
                PrimaryAction(
                    text = if (exporting) {
                        "Preparando la lámina…"
                    } else {
                        "Exportar lámina como imagen"
                    },
                    onClick = onExport,
                    enabled = !exporting,
                    share = !exporting,
                )
                ExternalLink(
                    text = "Fuente en Numista",
                    onClick = { onOpenSource(plate.source) },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(plate.cells, key = { it.id }) { cell ->
            PlateCell(
                cell = cell,
                images = cell.numistaTypeId?.let { images[it] },
                printedSide = plate.printedSide,
                onOpenSource = onOpenSource,
            )
        }
    }
}

@Composable
private fun PlateCell(
    cell: DrawnCell,
    images: TypeImages?,
    printedSide: PrintedSide,
    onOpenSource: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AlbumHole(
            photo = images?.printedPhoto(printedSide),
            missing = cell.missing,
            modifier = Modifier.size(104.dp),
        )
        // An announced member has no Numista page to open: the coin is not in the catalogue.
        val typeId = cell.numistaTypeId
        PlateCellName(
            name = cell.label,
            onOpen = typeId?.let { { onOpenSource(numistaTypeUrl(it)) } },
        )
        // Only what tells this cell apart, which is at most the year: in a date run the title is
        // already it, and the type is reached by tapping the title right above.
        cell.footnote?.let { footnote ->
            Text(
                footnote,
                style = MaterialTheme.typography.labelLarge,
                color = Paper.muted,
                textAlign = TextAlign.Center,
            )
        }
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
 * [onOpen] is null for an announced member, which has no Numista page to open.
 */
@Composable
internal fun PlateCellName(
    name: String,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
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
        if (onOpen != null) {
            ExternalLink(
                text = name,
                style = style,
                textAlign = TextAlign.Center,
                autoSize = PLATE_CELL_NAME_AUTO_SIZE,
                maxLines = PLATE_CELL_NAME_LINES,
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
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
}

/** Every cell reserves the same name range, so a row's years share one baseline. */
private const val PLATE_CELL_NAME_LINES = 2

/** The breathing room [ExternalLink] adds around its own text, matched by the plain branch. */
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
