package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.DrawnWish
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.WishLabels
import com.jenarvaezg.coindex.ui.WishSubject
import com.jenarvaezg.coindex.ui.coinAlbumFaces
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.HoleAbsence
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.HoleStamp
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.components.YearTagMetrics
import com.jenarvaezg.coindex.ui.printedName
import com.jenarvaezg.coindex.ui.plateSheetTally
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.printedFaces
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.wishListFileName
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * «Lo que busco»: the casillas the collector marked, on one sheet (ADR 0029 §6). Part of the annex
 * of ADR 0026 §8, not a hierarchy: reached from Colecciones and «Explorar» (ADR 0030 §8), left with
 * «Volver», no bar cell.
 *
 * Rows are drawn like the plate's casillas (hole, sunken year, name), plus the lámina they belong
 * to and «Quitar». It stays a full screen rather than folding into the shelf because its purpose is
 * «Exportar la lista», the sheet taken to a fair. One order, last marked first, so no shelf.
 */
@Composable
fun WishesScreen(
    subject: WishSubject,
    images: Map<Int, TypeImages>,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    /** The coin sheet a row's year opens, the same one its plate opens (#508). */
    sheet: CoinSheetSurface,
    onRemove: (WishKey) -> Unit,
    onMessage: (UiNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A row, not a type: the face up comes from the row's plate, and two plates can declare
    // different sides for one type (ADR 0020, #227).
    var openRowId by rememberSaveable { mutableStateOf<String?>(null) }
    val openRow = subject.rows.firstOrNull { it.id == openRowId }
    Box(modifier = modifier) {
        // The shared export flow (#430), exporting «la lista».
        SheetExportFlow(
            sheet = SharedSheet.LIST,
            key = WishLabels.DESTINATION,
            fileName = wishListFileName(),
            notebookOptions = notebookOptions,
            onNotebookPrinted = onNotebookPrinted,
            notebookPages = notebookPages,
            onExporting = onExporting,
            onMessage = onMessage,
            tally = plateSheetTally(subject.rows.size),
            modifier = Modifier.fillMaxSize(),
        ) { export ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(104.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = WISH_MARGIN, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
                // Same row gap as the plate (see [PlateSpacing]).
                verticalArrangement = Arrangement.spacedBy(PlateSpacing.rowGap),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // No heading: the top bar already says «Lo que busco» (ADR 0026 §5).
                        Text(
                            WishLabels.SENTENCE,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Paper.muted,
                        )
                        subject.census?.let { census ->
                            Text(
                                census,
                                style = MaterialTheme.typography.labelLarge,
                                color = Paper.rust,
                            )
                        }
                        if (subject.rows.isEmpty()) {
                            FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    WishLabels.EMPTY_EXPLANATION,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Paper.muted,
                                )
                            }
                        } else {
                            // Hidden while the panel is open (#512).
                            SheetExportDoorButton(export.door)
                            export.options?.invoke()
                            export.progress?.invoke()
                        }
                    }
                }
                items(subject.rows, key = DrawnWish::id) { row ->
                    WishCell(
                        row = row,
                        images = images[row.typeId],
                        onOpenCoin = { openRowId = row.id },
                        onRemove = { onRemove(row.key) },
                    )
                }
            }
        }
        CoinSheetOverlay(
            typeId = openRow?.typeId,
            surface = sheet,
            // The row's plate's side; if a sync removed the row while the sheet was open, the
            // album's reverse-first rule.
            faces = { typeId ->
                val side = openRow?.printedSide
                if (side == null) coinAlbumFaces(images[typeId]) else printedFaces(images[typeId], side)
            },
            onDismiss = { openRowId = null },
        )
    }
}

/** Same page margin as the plate. */
private val WISH_MARGIN = 20.dp

/**
 * One marked casilla in the list.
 *
 * No «lo busco» chip: every casilla here is marked, so it would distinguish nothing (ADR 0026 §5);
 * the chip only shows the price. «Quitar» on each row is fine here, unlike per-casilla toggles on a
 * plate (ADR 0029 §5): the list is just the marks, and removing one is its only upkeep, like a
 * box's per-piece action (ADR 0021 §9).
 */
@Composable
private fun WishCell(
    row: DrawnWish,
    images: TypeImages?,
    onOpenCoin: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AlbumHole(
                photo = images?.printedPhoto(row.printedSide),
                // Drawn whole, not as a ghost, so the coin can be recognised at a fair; the dotted
                // rule says it isn't owned (#520).
                absence = HoleAbsence.Wanted,
                otherSide = images?.printedPhoto(row.printedSide.other),
                modifier = Modifier.size(104.dp),
            )
            HoleStamp(cost = row.cost, wished = false)
        }
        // The year's target height is reserved even without a year, as on the plate (#473).
        Box(
            modifier = Modifier.height(YearTagMetrics.target),
            contentAlignment = Alignment.Center,
        ) {
            row.year?.let { year ->
                RecessedYearTag(year = year, onOpen = onOpenCoin)
            }
        }
        row.printedName?.let { name -> PlateCellName(name = name) }
        // The lámina this casilla belongs to, since the list crosses plates.
        Text(
            row.plate,
            style = MaterialTheme.typography.labelMedium,
            color = Paper.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        CardAction(text = WishLabels.REMOVE_ACTION, onClick = onRemove)
    }
}
