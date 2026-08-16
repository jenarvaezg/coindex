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
 * «Lo que busco»: the casillas the collector marked, in one sheet (ADR 0029 §6).
 *
 * It lives inside the annex of ADR 0026 §8 and is not a hierarchy: it hangs off Colecciones, it is left
 * with «Volver» and it carries no cell in the bar. What it is made of is not the collection at all — it
 * is the coins the collector does **not** have — which is why no order of the index's sheet could have
 * held it.
 *
 * **The same casillas, drawn the same way.** The hole, the ghost of the design, the year sunk into the
 * cardboard and the name under it are the plate's, because a marked slot is a slot: what a row adds is
 * which lámina it came from, which is the one thing the plate never has to say, and «Quitar».
 *
 * **A screen of its own behind a door of «Explorar»** since the shelf window arrived (ADR 0030 §8): the
 * annex used to be this list and nothing else, and the list stays whole rather than folding into the
 * shelf because what it is *for* is `Exportar la lista` — the sheet taken to a fair — and a tile saying
 * «2 lo busco» cannot be taken anywhere. Seven rows have one order, the last marked first, so it carries
 * no shelf of its own.
 */
@Composable
fun WishesScreen(
    subject: WishSubject,
    images: Map<Int, TypeImages>,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    /**
     * The sheet the year of a marked casilla opens (#508).
     *
     * The lámina's own, from the same place: a row here **is** a casilla of a plate, so pressing its
     * year opens the sheet it opens over there.
     */
    sheet: CoinSheetSurface,
    onRemove: (WishKey) -> Unit,
    onMessage: (UiNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Which **row** is open — not which type — because the face a casilla rests on is its own plate's
    // declaration, and the list crosses plates: two marked casillas of one type in two catalogs can
    // declare different sides (ADR 0020, #227). This screen's own state exactly as it is the lámina's.
    var openRowId by rememberSaveable { mutableStateOf<String?>(null) }
    val openRow = subject.rows.firstOrNull { it.id == openRowId }
    Box(modifier = modifier) {
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
            modifier = Modifier.fillMaxSize(),
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
                        if (subject.rows.isEmpty()) {
                            FieldCard(dashed = true, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    WishLabels.EMPTY_EXPLANATION,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Paper.muted,
                                )
                            }
                        } else {
                            // Gone while the panel it opened is in its slot (#512).
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
            // The declaration of the row's own plate, read off the row that was pressed. Where the row
            // is already gone — a sync filled its casilla while the sheet was open — the album's
            // reverse-first rule answers instead of a plate's.
            faces = { typeId ->
                val side = openRow?.printedSide
                if (side == null) coinAlbumFaces(images[typeId]) else printedFaces(images[typeId], side)
            },
            onDismiss = { openRowId = null },
        )
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
 *
 * **«Quitar» *is* per row, and that is not the control ADR 0029 §5 refused.** What it refused was a
 * toggle on each of a plate's fifty-one casillas, where the mode's cost line would have been printed
 * fifty-one times over coins the collector was only browsing. Here the population is the marks
 * themselves — seven of them — undoing one is the only upkeep the screen has, and it is the shape a
 * box's upkeep already has beside each of its pieces (ADR 0021 §9). A mode would charge two taps to
 * undo one mistake on a list that exists to be pruned.
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
                RecessedYearTag(year = year, onOpen = onOpenCoin)
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
