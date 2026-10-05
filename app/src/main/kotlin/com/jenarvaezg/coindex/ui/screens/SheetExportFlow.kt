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
 * A running export of one lámina or hoja. The format is measured, not chosen (#401): one page goes
 * out as a PNG, more as a PDF.
 */
sealed class SheetExportJob {
    abstract val destination: ExportDestination

    /** One page: the printed folio, trimmed to its content and saved as a PNG (#431). */
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
 * The button into «Cómo se exporta». A separate type because it is absent while the panel is open
 * (#512).
 */
class SheetExportDoor(
    /** «Exportar lámina» / «Exportar hoja», or «Preparando la lámina…» while exporting. */
    val label: String,
    /** False while an export is running. */
    val enabled: Boolean,
    val onExport: () -> Unit,
)

/**
 * What a screen draws for exporting: the button and the two cards it opens, each null when there is
 * nothing to draw. The screen decides where they go.
 *
 * [door] and [options] are never both present (#512): the panel replaces the button, and
 * «Cancelar» in the panel brings it back.
 */
class SheetExportSurface(
    /** Null while the panel is open. */
    val door: SheetExportDoor?,
    /** The «Cómo se exporta» panel, while open. */
    val options: (@Composable () -> Unit)?,
    /** PDF progress and cancel; null for a one-page PNG. */
    val progress: (@Composable () -> Unit)?,
)

/**
 * Draws [door] as a button, or nothing while the panel replaces it (#512).
 *
 * @param enabled an extra condition from the screen, e.g. a collection with no pieces has nothing
 *   to export.
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
 * The export flow for a single lámina or hoja, shared by `PlateScreen` and `PiecesScreen`
 * (#430, #431). Both export the printed page, as a PNG when it fits one page and through
 * [NotebookPdfExport] otherwise.
 *
 * @param key the subject's id (catalog id, collection title), so switching subject recounts pages.
 * @param tally what the sheet holds, for the closing message.
 * @param content the screen, given the [SheetExportSurface] to place.
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
    // Discarded on «Cancelar»; reset from the stored options each time the panel opens.
    var draft by remember { mutableStateOf(notebookOptions) }
    var job by remember { mutableStateOf<SheetExportJob?>(null) }
    var step by remember { mutableStateOf<NotebookExportStep>(NotebookExportStep.Drawing(0, "")) }
    // Driven by the state, not the tap, so cancel and failure report it too.
    LaunchedEffect(job != null) { onExporting(job != null) }
    // Recounted on each switch change without drawing; the cost line and format come from these.
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
                    // The folio's first plate; it may share the folio (#232).
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
                    // A single sheet never offers «Sin colección».
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
                    // Cancellable except while writing, which would close the document under the
                    // thread serializing it.
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
