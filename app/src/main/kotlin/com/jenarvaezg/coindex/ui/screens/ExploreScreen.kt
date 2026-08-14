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
import com.jenarvaezg.coindex.ui.components.AlbumHole
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.HoleStamp
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.components.RecessedYearTag
import com.jenarvaezg.coindex.ui.components.YearTagMetrics
import com.jenarvaezg.coindex.ui.numistaTypeUrl
import com.jenarvaezg.coindex.ui.printedName
import com.jenarvaezg.coindex.ui.plateSheetTally
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.printedPhoto
import com.jenarvaezg.coindex.ui.wishListFileName
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.theme.PlateMetrics

/**
 * «Lo que busco»: the casillas the collector marked, in one sheet (ADR 0029 §6).
 *
 * It is an **annex** by ADR 0026 §8 and not a hierarchy: it hangs off Colecciones, it is entered
 * through the last row of that list and left with «Volver», and it carries no cell in the bar. What it
 * is made of is not the collection at all — it is the coins the collector does **not** have — which is
 * why no order of the index's sheet could have held it.
 *
 * **The same casillas, drawn the same way.** The hole, the ghost of the design, the year sunk into the
 * cardboard and the name under it are the plate's, because a marked slot is a slot: what a row adds is
 * which lámina it came from, which is the one thing the plate never has to say, and «Quitar».
 *
 * It is «Explorar»'s first section, and today it is the whole of it: the shelf window of twenty plates
 * is not in this delivery, so the annex has one section, no shelf — seven rows have one order, the last
 * marked first — and it is named after the only thing behind its door.
 */
@Composable
fun ExploreScreen(
    subject: WishSubject,
    images: Map<Int, TypeImages>,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    onOpenSource: (String) -> Unit,
    onRemove: (WishKey) -> Unit,
    onMessage: (UiNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The same machine a lámina and a hoja use (#430), with a noun of its own: what leaves here is
    // «la lista», and one page of it is a PNG exactly as one page of a plate is.
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
        modifier = modifier,
    ) { export ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = WISH_MARGIN, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(PlateMetrics.gutter),
            // The plate's own row gap, because these are the plate's own casillas: what separates two
            // rows of holes is the sheet's proximity and not its air (see [PlateSpacing]).
            verticalArrangement = Arrangement.spacedBy(PlateSpacing.rowGap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // No eyebrow and no title: the masthead of this screen already says «Lo que
                    // busco», and printing it again one line below is the furniture ADR 0026 §5
                    // prices.
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
                    // What the marks cost every month, where the collector can see it without going
                    // into Ajustes: it is the first spend of the app that they decide (ADR 0029 §5).
                    subject.spend?.let { spend ->
                        Text(
                            spend,
                            style = MaterialTheme.typography.labelLarge,
                            color = Paper.muted,
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
                        PrimaryAction(
                            text = export.label,
                            onClick = export.onExport,
                            enabled = export.enabled,
                        )
                        export.options?.invoke()
                        export.progress?.invoke()
                    }
                }
            }
            items(subject.rows, key = DrawnWish::id) { row ->
                WishCell(
                    row = row,
                    images = images[row.typeId],
                    onOpenSource = onOpenSource,
                    onRemove = { onRemove(row.key) },
                )
            }
        }
    }
}

/** The page margin of the list, which is the plate's: the same casillas on the same paper. */
private val WISH_MARGIN = 20.dp

/**
 * One marked casilla as a row of the list.
 *
 * **No mark drawn inside the hole**, and that is the frequency rule of ADR 0026 §5 rather than an
 * omission: every casilla on this sheet is marked, so «lo busco» under each of them would print the
 * same two words seven times to distinguish nothing. On a plate the chip is the whole point, because
 * there it tells one hole from its fifty neighbours. What the chip still says here is the **price**,
 * which does differ per row.
 */
@Composable
private fun WishCell(
    row: DrawnWish,
    images: TypeImages?,
    onOpenSource: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AlbumHole(
                photo = images?.printedPhoto(row.printedSide),
                // Always a hole: what is on this sheet is by definition what the collector does not
                // have, so the ghost of the design is what every one of them shows.
                missing = true,
                otherSide = images?.printedPhoto(row.printedSide.other),
                modifier = Modifier.size(104.dp),
            )
            HoleStamp(cost = row.cost, wished = false)
        }
        // The year's own target, reserved whether or not there is a year to put in it, exactly as on
        // the plate: otherwise a row with none would pull its name up against the hole while its
        // neighbours' stayed down (#473).
        Box(
            modifier = Modifier.height(YearTagMetrics.target),
            contentAlignment = Alignment.Center,
        ) {
            row.year?.let { year ->
                RecessedYearTag(
                    year = year,
                    onOpen = { onOpenSource(numistaTypeUrl(row.typeId)) },
                )
            }
        }
        row.printedName?.let { name -> PlateCellName(name = name) }
        // Which lámina this casilla is a slot of. The list crosses plates, so it is the one thing a
        // row has that a casilla on its own plate never needs to say.
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
