package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.NOTHING_TO_PRINT_MESSAGE
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.notebookCancelledMessage
import com.jenarvaezg.coindex.ui.notebookWarmCancelledMessage
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.sheetExportSwitches
import com.jenarvaezg.coindex.ui.sheetExportAsBitmap
import com.jenarvaezg.coindex.ui.sheetExportCostLabel
import com.jenarvaezg.coindex.ui.sheetExportCostScope
import com.jenarvaezg.coindex.ui.sheetExportLabel
import com.jenarvaezg.coindex.ui.sheetExportSwitchNote

/**
 * What one lámina or one hoja is being exported as, once the panel has been answered.
 *
 * Measured and not chosen (#401): what fits in one page leaves as a PNG, what needs more takes the
 * PDF section of the notebook. The [destination] rides along because it was answered in the same
 * card, one question earlier.
 */
sealed class SheetExportJob {
    abstract val destination: ExportDestination

    /** One page: the printed folio, trimmed to what it draws and recorded as a PNG (#431). */
    data class Bitmap(
        val page: PrintPage,
        override val destination: ExportDestination,
    ) : SheetExportJob()

    data class Pdf(
        val pages: List<PrintPage>,
        override val destination: ExportDestination,
    ) : SheetExportJob()
}

/**
 * The button into «Cómo se exporta»: what it says, whether it can be pressed, and the tap.
 *
 * It is a thing of its own because it is **absent** for a whole state of the flow, and a label with
 * nothing to label is how the panel came to be opened by a grey button that stayed on screen (#512).
 */
class SheetExportDoor(
    /** «Exportar lámina» / «Exportar hoja», or «Preparando la lámina…» while one is on its way. */
    val label: String,
    /** False while an export is running: one conversation at a time. */
    val enabled: Boolean,
    val onExport: () -> Unit,
)

/**
 * The one export door a screen draws, and the two cards it opens.
 *
 * A screen is handed this instead of four pieces of state, which is the whole point: [door] already
 * knows whether an export is in flight, and the three slots are null exactly when there is nothing
 * to draw in them. Where they go is still the screen's business — the plate hangs them off its
 * heading item, the hoja gives them rows of their own.
 *
 * **[door] and [options] are never both there** (#512): the panel does not appear beside the button
 * that opened it, it takes its place. A disabled button repeating a question already on screen is
 * read as broken, and the way back to it is «Cancelar», which the panel owns.
 */
class SheetExportSurface(
    /** The way in, or null while the panel is standing in its place. */
    val door: SheetExportDoor?,
    /** «Cómo se exporta», while the collector is answering it. */
    val options: (@Composable () -> Unit)?,
    /** Where the PDF has got to, and the way out of it. A one-page PNG has nothing to report. */
    val progress: (@Composable () -> Unit)?,
)

/**
 * The door drawn as a button, and nothing at all while the panel has taken its place (#512).
 *
 * The `null` lives here and not in three screens: what each of them owns is **where** the button
 * goes, which is the modifier, and — for the hoja — a condition of its own. Unpacking the label,
 * the tap and the enabling three times over is how the two twin export machines of #430 started.
 *
 * @param enabled a second condition the screen adds to the flow's: a collection with no piece in it
 *   has no sheet to export, whatever the machine says about being free.
 */
@Composable
fun SheetExportDoorButton(
    door: SheetExportDoor?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    door?.let { open ->
        PrimaryAction(
            text = open.label,
            onClick = open.onExport,
            enabled = open.enabled && enabled,
            modifier = modifier,
        )
    }
}

/**
 * Exporting a single lámina or hoja: the whole machine, owned once (#430).
 *
 * `PlateScreen` and `PiecesScreen` each carried a copy of it — two twin sealed classes, the same
 * four pieces of state, the same `begin()` measuring pages and picking a format, the same
 * `ExportProgress` with its cancellation by step, and the same final `when` dispatching to
 * [SheetPngExport] or [NotebookPdfExport]. Twins drift: #219 had already merged the drawing half of
 * this for the same reason, and the halves that were left apart went on to grow the second entrance
 * #434 removed.
 *
 * **Nothing is left that the two screens do not share** (#431). What used to differ was the bitmap —
 * a plate drew `PlateSheet` and a leaf `PiecesSheet`, neither of which received the switches — and
 * since the PNG became the printed page there is one drawing for both, chosen by the same measure as
 * before: one page is a photo, more is the notebook's PDF.
 *
 * @param key what a fresh export is keyed on — the catalog's id, the collection's title — so that
 *   moving to another subject recounts its pages instead of showing the previous one's.
 * @param tally what the sheet says it holds, in its own words, for the closing message.
 * @param content the screen itself, handed the door and the two cards to place.
 */
@Composable
fun SheetExportFlow(
    sheet: SharedSheet,
    key: String,
    fileName: String,
    notebookOptions: NotebookOptions,
    onNotebookPrinted: (NotebookOptions) -> Unit,
    notebookPages: (NotebookOptions) -> List<PrintPage>,
    onExporting: (Boolean) -> Unit,
    onMessage: (UiNotice) -> Unit,
    tally: String,
    modifier: Modifier = Modifier,
    content: @Composable (SheetExportSurface) -> Unit,
) {
    var configuring by remember { mutableStateOf(false) }
    // A draft, discarded on «Cancelar»: playing with the switches and backing out has not changed
    // how this collector prints. Reset from the stored configuration each time the panel opens.
    var draft by remember { mutableStateOf(notebookOptions) }
    var job by remember { mutableStateOf<SheetExportJob?>(null) }
    var step by remember { mutableStateOf<NotebookExportStep>(NotebookExportStep.Drawing(0, "")) }
    // Announced from the state and not from the tap, so cancelling and failing say it too: every
    // way out of an export goes through `job` becoming null.
    LaunchedEffect(job != null) { onExporting(job != null) }
    // Recounted when a switch moves, and nothing is drawn to get it: the panel's cost line and the
    // format it announces are both arithmetic over these pages.
    val pages = remember(configuring, draft, key) {
        if (!configuring) null else notebookPages(draft)
    }

    fun begin(destination: ExportDestination, measured: List<PrintPage>) {
        if (measured.isEmpty()) {
            onMessage(UiNotice(NOTHING_TO_PRINT_MESSAGE))
        } else {
            onNotebookPrinted(draft)
            job = if (sheetExportAsBitmap(measured.size)) {
                SheetExportJob.Bitmap(measured.single(), destination)
            } else {
                step = NotebookExportStep.Drawing(
                    0,
                    // The plate at the top of the folio, which since #232 may not be the only
                    // one on it.
                    measured.first().blocks.first().section.title,
                )
                SheetExportJob.Pdf(measured, destination)
            }
        }
        configuring = false
    }

    val surface = SheetExportSurface(
        door = if (configuring) {
            null
        } else {
            SheetExportDoor(
                label = sheetExportLabel(sheet, exporting = job != null),
                enabled = job == null,
                onExport = {
                    draft = notebookOptions
                    configuring = true
                },
            )
        },
        options = pages?.let { measured ->
            {
                ExportOptions(
                    options = draft,
                    pages = measured.size,
                    cards = 1,
                    // A single sheet never asks «Sin colección»: there is no index under it to
                    // have loose coins in.
                    loose = 0,
                    onChange = { draft = it },
                    onDownload = { begin(ExportDestination.Download, measured) },
                    onShare = { begin(ExportDestination.Share, measured) },
                    onDismiss = { configuring = false },
                    switches = sheetExportSwitches(),
                    costScope = sheetExportCostScope(sheet),
                    costLabel = sheetExportCostLabel(sheet, measured.size),
                    switchNote = ::sheetExportSwitchNote,
                )
            }
        },
        progress = (job as? SheetExportJob.Pdf)?.let { pdf ->
            {
                ExportProgress(
                    step = step,
                    pages = pdf.pages.size,
                    // Every step but the write, which would close the document under the thread
                    // serializing it.
                    onCancel = when (val current = step) {
                        is NotebookExportStep.Warming -> {
                            {
                                job = null
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
                                job = null
                                onMessage(
                                    UiNotice(
                                        notebookCancelledMessage(
                                            current.pagesDone,
                                            pdf.pages.size,
                                        ),
                                    ),
                                )
                            }
                        }
                        NotebookExportStep.Writing -> null
                    },
                )
            }
        },
    )

    Box(modifier = modifier) {
        content(surface)
        when (val current = job) {
            is SheetExportJob.Bitmap -> SheetPngExport(
                page = current.page,
                destination = current.destination,
                fileName = fileName,
                sheet = sheet,
                tally = tally,
                onFinished = { message ->
                    job = null
                    onMessage(message)
                },
            )
            is SheetExportJob.Pdf -> NotebookPdfExport(
                pages = current.pages,
                destination = current.destination,
                onStep = { step = it },
                onFinished = { message ->
                    job = null
                    onMessage(message)
                },
                fileName = fileName,
                sheet = sheet,
            )
            null -> Unit
        }
    }
}
