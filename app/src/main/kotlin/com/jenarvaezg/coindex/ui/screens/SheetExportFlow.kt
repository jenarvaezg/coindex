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

    data class Bitmap(override val destination: ExportDestination) : SheetExportJob()

    data class Pdf(
        val pages: List<PrintPage>,
        override val destination: ExportDestination,
    ) : SheetExportJob()
}

/**
 * The one export door a screen draws, and the two cards it opens.
 *
 * A screen is handed this instead of four pieces of state, which is the whole point: [label] and
 * [enabled] already know whether an export is in flight, and [options] and [progress] are null
 * exactly when there is nothing to draw in their slot. Where they go is still the screen's business
 * — the plate hangs them off its heading item, the hoja gives them rows of their own.
 */
class SheetExportSurface(
    /** «Exportar lámina» / «Exportar hoja», or «Preparando la lámina…» while one is on its way. */
    val label: String,
    /** False while the panel is open or an export is running: one conversation at a time. */
    val enabled: Boolean,
    val onExport: () -> Unit,
    /** «Cómo se exporta», while the collector is answering it. */
    val options: (@Composable () -> Unit)?,
    /** Where the PDF has got to, and the way out of it. A one-page PNG has nothing to report. */
    val progress: (@Composable () -> Unit)?,
)

/**
 * Exporting a single lámina or hoja: the whole machine, owned once (#430).
 *
 * `PlateScreen` and `PiecesScreen` each carried a copy of it — two twin sealed classes, the same
 * four pieces of state, the same `begin()` measuring pages and picking a format, the same
 * `ExportProgress` with its cancellation by step, and the same final `when` dispatching to
 * [SheetExport] or [NotebookPdfExport]. Twins drift: #219 had already merged the drawing half of
 * this for the same reason, and the halves that were left apart went on to grow the second entrance
 * #434 removed. What genuinely differs between a plate and a leaf of pieces is what the bitmap
 * draws, and that is the [bitmap] slot.
 *
 * @param key what a fresh export is keyed on — the catalog's id, the collection's title — so that
 *   moving to another subject recounts its pages instead of showing the previous one's.
 * @param bitmap draws the one-page export, which is the only thing the two screens do not share.
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
    bitmap: @Composable (destination: ExportDestination, onFinished: (UiNotice) -> Unit) -> Unit,
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
                SheetExportJob.Bitmap(destination)
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
        label = sheetExportLabel(sheet, exporting = job != null),
        enabled = !configuring && job == null,
        onExport = {
            if (!configuring) draft = notebookOptions
            configuring = true
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
                    switchNote = { switch, offered ->
                        sheetExportSwitchNote(switch, offered, measured.size)
                    },
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
            is SheetExportJob.Bitmap -> bitmap(current.destination) { message ->
                job = null
                onMessage(message)
            }
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
