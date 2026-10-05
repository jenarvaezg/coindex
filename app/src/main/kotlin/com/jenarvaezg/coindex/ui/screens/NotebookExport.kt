package com.jenarvaezg.coindex.ui.screens

import android.graphics.Picture
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jenarvaezg.coindex.ui.ExportDestination
import com.jenarvaezg.coindex.ui.SharedSheet
import com.jenarvaezg.coindex.ui.UiNotice
import com.jenarvaezg.coindex.ui.downloadLandedNotice
import com.jenarvaezg.coindex.ui.notebookDownloadFailure
import com.jenarvaezg.coindex.ui.notebookExportMessage
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.addNotebookPage
import com.jenarvaezg.coindex.ui.print.downloadNotebookPdf
import com.jenarvaezg.coindex.ui.print.notebookFileName
import com.jenarvaezg.coindex.ui.print.notebookPhotographs
import com.jenarvaezg.coindex.ui.print.shareNotebookPdf
import com.jenarvaezg.coindex.ui.print.warmNotebookPhotographs
import com.jenarvaezg.coindex.ui.recordInto
import com.jenarvaezg.coindex.ui.sheetDownloadFailure
import com.jenarvaezg.coindex.ui.sheetExportFailure
import com.jenarvaezg.coindex.ui.sheetPdfExportMessage

/**
 * How long one notebook page waits for its pictures. Much shorter than a single plate's wait,
 * because every photograph has already been fetched and cached: a page only waits for disk decodes,
 * and one that hasn't landed in four seconds isn't coming.
 *
 * It is a ceiling, not a per-photo budget, so «ambas caras» (#230) doubling the decodes doesn't
 * need a longer one; this was checked on a both-faces export with no page reaching it.
 */
private const val PAGE_WAIT_MILLIS = 4_000L

/**
 * Draws the notebook into one PDF, one page at a time, and sends it to [destination] (Descargas or
 * the share sheet, #285).
 *
 * Photographs are fetched once, up front (`warmNotebookPhotographs`, which explains why asking page
 * by page is much slower), before the first page is composed. With photographs off (#231) there is
 * nothing to warm: `warm` starts true and no [NotebookExportStep.Warming] is reported.
 *
 * Only one page is in composition at a time, so memory holds one full-page recording. Each page
 * waits for its decodes, is appended to the document as drawing commands, and is dropped.
 *
 * Cancelling is leaving composition: the effect is cancelled and [DisposableEffect] closes the
 * document; no file exists until the last page is in. The final step reports
 * [NotebookExportStep.Writing] because `writeTo` is a blocking native call that ignores coroutine
 * cancellation, so the parent must stop offering cancel while it runs.
 *
 * [sheet] makes the closing copy and file stem name a single lámina or hoja (#401).
 */
@Composable
fun NotebookPdfExport(
    pages: List<PrintPage>,
    destination: ExportDestination = ExportDestination.Download,
    onStep: (NotebookExportStep) -> Unit,
    onFinished: (UiNotice) -> Unit,
    fileName: String = notebookFileName(),
    sheet: SharedSheet? = null,
) {
    val context = LocalContext.current
    val document = remember { PdfDocument() }

    // No page is composed until these are fetched: its cells would join the same queue.
    val photographs = remember(pages) { notebookPhotographs(pages) }
    var warm by remember(pages) { mutableStateOf(photographs.isEmpty()) }

    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages.getOrNull(pageIndex).takeIf { warm }

    // A fresh recording per page, so the previous one is released as soon as the index moves on.
    val picture = remember(pageIndex) { Picture() }
    val settled = remember(pageIndex) { mutableIntStateOf(0) }

    // Across the whole notebook, for the closing message's count of missing photographs.
    val expectedPhotographs = remember(pages) { pages.sumOf { it.photographs } }
    val loadedPhotographs = remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) { onDispose { document.close() } }

    LaunchedEffect(pages) {
        if (photographs.isEmpty()) return@LaunchedEffect
        onStep(NotebookExportStep.Warming(0, photographs.size))
        warmNotebookPhotographs(context, photographs) { done ->
            onStep(NotebookExportStep.Warming(done, photographs.size))
        }
        warm = true
    }

    LaunchedEffect(pageIndex, warm) {
        if (!warm) return@LaunchedEffect
        val current = pages.getOrNull(pageIndex) ?: return@LaunchedEffect
        // Only the folio's first plate, even when it shares the folio (#232): the progress line
        // must stay short.
        onStep(NotebookExportStep.Drawing(pageIndex, current.blocks.first().section.title))
        awaitSettledImages(current.photographs, settled, PAGE_WAIT_MILLIS)
        val appended = runCatching {
            addNotebookPage(document, picture, pageIndex + 1, current.geometry)
        }
        if (appended.isFailure) {
            val cause = appended.exceptionOrNull()?.message
            onFinished(
                UiNotice(
                    sheet?.let { sheetExportFailure(it, cause) }
                        ?: "No se pudo exportar el cuaderno: $cause",
                ),
            )
            return@LaunchedEffect
        }
        if (pageIndex + 1 < pages.size) {
            pageIndex += 1
            return@LaunchedEffect
        }
        onStep(NotebookExportStep.Writing)
        val written = runCatching {
            when (destination) {
                ExportDestination.Download ->
                    downloadNotebookPdf(context, document, fileName)
                ExportDestination.Share -> {
                    shareNotebookPdf(
                        context,
                        document,
                        fileName,
                        chooserTitle = sheet?.let { "Compartir la ${it.noun}" }
                            ?: "Compartir el cuaderno",
                    )
                    null
                }
            }
        }
        onFinished(
            if (written.isFailure) {
                val cause = written.exceptionOrNull()?.message
                UiNotice(
                    when (destination) {
                        ExportDestination.Download ->
                            sheet?.let { sheetDownloadFailure(it, cause) }
                                ?: notebookDownloadFailure(cause)
                        ExportDestination.Share ->
                            sheet?.let { sheetExportFailure(it, cause) }
                                ?: "No se pudo exportar el cuaderno: $cause"
                    },
                )
            } else {
                when (destination) {
                    ExportDestination.Download -> {
                        val landed = requireNotNull(written.getOrThrow())
                        downloadLandedNotice(
                            expectedPhotos = expectedPhotographs,
                            loadedPhotos = loadedPhotographs.intValue,
                            uri = landed.uri,
                            mimeType = landed.mimeType,
                        )
                    }
                    ExportDestination.Share ->
                        UiNotice(
                            sheet?.let {
                                sheetPdfExportMessage(
                                    sheet = it,
                                    pages = pages.size,
                                    expectedPhotos = expectedPhotographs,
                                    loadedPhotos = loadedPhotographs.intValue,
                                )
                            } ?: notebookExportMessage(
                                pages = pages.size,
                                expectedPhotos = expectedPhotographs,
                                loadedPhotos = loadedPhotographs.intValue,
                            ),
                        )
                }
            },
        )
    }

    if (page != null) {
        OffScreenSheet(printDensity) {
            NotebookPageSheet(
                page = page,
                onImageSettled = { painted ->
                    settled.intValue += 1
                    if (painted) loadedPhotographs.intValue += 1
                },
                // The page paints its own paper; recording it from the outside would drop it.
                modifier = Modifier.recordInto(picture),
            )
        }
    }
}
