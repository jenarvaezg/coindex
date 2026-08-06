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
import com.jenarvaezg.coindex.ui.notebookExportMessage
import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.addNotebookPage
import com.jenarvaezg.coindex.ui.print.notebookFileName
import com.jenarvaezg.coindex.ui.print.notebookPhotographs
import com.jenarvaezg.coindex.ui.print.shareNotebookPdf
import com.jenarvaezg.coindex.ui.print.warmNotebookPhotographs
import com.jenarvaezg.coindex.ui.recordInto

/**
 * How long one page of the notebook waits for its pictures.
 *
 * Far below the thirty seconds a single plate gets, because by now every photograph has been asked
 * for and cached: what a page is waiting for is a decode from disk, and one that has not landed in
 * four seconds is not coming. The old ceiling was what turned a notebook of seventy pages into
 * thirty-five minutes of waiting for pictures that were never going to arrive.
 *
 * **Unchanged by «ambas caras» (#230), and checked rather than assumed.** A page of both faces asks
 * for twice as many decodes, but four seconds was never a budget of twelve: today's own pages run to
 * thirty cells of Venezuelan medios, and this is a **ceiling and not a cost** — what is cached
 * settles in a frame. The seven pages of a Kookaburra plate at both faces, seventy-four photographs,
 * were exported without one page reaching this ceiling and without a hole in the PDF.
 */
private const val PAGE_WAIT_MILLIS = 4_000L

/**
 * Draws the whole notebook into one PDF, one page at a time, and shares it.
 *
 * **Photographs first, pages second.** Every picture the notebook needs is fetched once, up front
 * (`warmNotebookPhotographs`), and only then is the first page composed. Asking page by page is what
 * the first version did and it produced 64 photographs out of 600 in half an hour: see that
 * function for why the two are not equivalent.
 *
 * **Or no photographs at all** (#231), and then the whole of that step is skipped rather than run
 * over an empty list: `warm` starts true, no [NotebookExportStep.Warming] is ever reported, no page
 * waits for a decode, and the closing message has a denominator of zero — the one export of the three
 * that cannot come out incomplete. It falls out of the arithmetic and is not a branch: a cell with no
 * faces asks for nothing, so there is nothing to warm and nothing to count.
 *
 * **One page in composition at any moment**, which is the only thing that makes the drawing
 * affordable: composing every page at once would hold seventy full-page recordings in memory. Each
 * page waits for its own pictures — cached by now, so in practice for a decode — is appended to the
 * document as drawing commands, and is then dropped.
 *
 * Cancelling is leaving composition: the parent stops drawing this, the effect is cancelled and
 * [DisposableEffect] closes the document. Nothing has to be cleaned up because nothing has been
 * written — the file appears only once the last page is in. That is also why the last step reports
 * [NotebookExportStep.Writing]: `writeTo` is a blocking native call that would not notice the
 * coroutine being cancelled, so the parent has to stop offering a cancel that would close the
 * document while it is being serialized.
 */
@Composable
fun NotebookPdfExport(
    pages: List<PrintPage>,
    onStep: (NotebookExportStep) -> Unit,
    onFinished: (String) -> Unit,
) {
    val context = LocalContext.current
    val document = remember { PdfDocument() }

    // Every photograph of the notebook, fetched once and before anything is drawn. Until it is
    // done no page is composed at all: composing one would put its cells in the same queue.
    val photographs = remember(pages) { notebookPhotographs(pages) }
    var warm by remember(pages) { mutableStateOf(photographs.isEmpty()) }

    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages.getOrNull(pageIndex).takeIf { warm }

    // A fresh recording per page, so the previous one is released as soon as the index moves on.
    val picture = remember(pageIndex) { Picture() }
    val settled = remember(pageIndex) { mutableIntStateOf(0) }

    // Counted across the whole notebook, because that is what the closing message is about: how
    // many of the photographs it asked for never arrived.
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
        // The plate at the top of the folio, which since #232 may not be the only one on it: the
        // progress line is what tells a stall from steady work, and it is the name the collector
        // recognises as the page goes by. Naming every plate on a shared folio would put three
        // titles in a line meant to be read at a glance.
        onStep(NotebookExportStep.Drawing(pageIndex, current.blocks.first().section.title))
        awaitSettledImages(current.photographs, settled, PAGE_WAIT_MILLIS)
        val appended = runCatching {
            addNotebookPage(document, picture, pageIndex + 1, current.geometry)
        }
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
        onStep(NotebookExportStep.Writing)
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
