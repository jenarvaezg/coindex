package com.jenarvaezg.coindex.ui.screens

import android.graphics.Picture
import android.graphics.pdf.PdfDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jenarvaezg.coindex.ui.notebookExportMessage
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.addNotebookPage
import com.jenarvaezg.coindex.ui.print.notebookFileName
import com.jenarvaezg.coindex.ui.print.shareNotebookPdf
import com.jenarvaezg.coindex.ui.recordInto

/**
 * Draws the whole notebook into one PDF, one page at a time, and shares it.
 *
 * **One page in composition at any moment**, which is the only thing that makes this affordable: a
 * full notebook is of the order of a thousand photographs, and composing every page at once would
 * ask Coil for all of them and hold eighty-four full-page recordings in memory. Each page waits for
 * its own pictures with the same budget a single plate gets (`awaitSettledImages`), is appended to
 * the document as drawing commands, and is then dropped.
 *
 * Cancelling is leaving composition: the parent stops drawing this, the effect is cancelled and
 * [DisposableEffect] closes the document. Nothing has to be cleaned up because nothing has been
 * written — the file appears only once the last page is in.
 */
@Composable
fun NotebookPdfExport(
    pages: List<PrintPage>,
    onProgress: (pagesDone: Int, title: String) -> Unit,
    onFinished: (String) -> Unit,
) {
    val context = LocalContext.current
    val document = remember { PdfDocument() }
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages.getOrNull(pageIndex)

    // A fresh recording per page, so the previous one is released as soon as the index moves on.
    val picture = remember(pageIndex) { Picture() }
    val settled = remember(pageIndex) { mutableIntStateOf(0) }

    // Counted across the whole notebook, because that is what the closing message is about: how
    // many of the photographs it asked for never arrived.
    val expectedPhotographs = remember(pages) { pages.sumOf { it.photographs } }
    val loadedPhotographs = remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) { onDispose { document.close() } }

    LaunchedEffect(pageIndex) {
        val current = pages.getOrNull(pageIndex) ?: return@LaunchedEffect
        onProgress(pageIndex, current.section.title)
        awaitSettledImages(current.photographs, settled)
        val appended = runCatching { addNotebookPage(document, picture, pageIndex + 1) }
        if (appended.isFailure) {
            onFinished(
                "No se pudo exportar el cuaderno: ${appended.exceptionOrNull()?.message}",
            )
            return@LaunchedEffect
        }
        if (pageIndex + 1 < pages.size) {
            pageIndex += 1
            return@LaunchedEffect
        }
        val shared = runCatching {
            shareNotebookPdf(context, document, notebookFileName())
        }
        onFinished(
            if (shared.isFailure) {
                "No se pudo exportar el cuaderno: ${shared.exceptionOrNull()?.message}"
            } else {
                notebookExportMessage(
                    pages = pages.size,
                    expectedPhotos = expectedPhotographs,
                    loadedPhotos = loadedPhotographs.intValue,
                )
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
