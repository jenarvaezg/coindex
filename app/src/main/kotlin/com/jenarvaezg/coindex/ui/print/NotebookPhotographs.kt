package com.jenarvaezg.coindex.ui.print

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import com.jenarvaezg.coindex.data.photos.warmPhotographs
import java.util.concurrent.atomic.AtomicInteger

/**
 * Photographs in flight at once during the warm-up: all four slots of `CoinPhotoLoader`'s
 * dispatcher. More would just queue there, out of reach of the progress counter and cancellation.
 * The background prefetch takes only two (#191); the export is what the collector is waiting on.
 */
private const val WARM_CONCURRENCY = 4

/**
 * Every photograph the notebook needs, deduplicated: a type can appear in many cells. Each face
 * counts (#230). Only the first candidate of each face, the thumbnail, is warmed; a page that falls
 * back to the original (#67) fetches it itself.
 */
fun notebookPhotographs(pages: List<PrintPage>): List<String> = pages
    .asSequence()
    .flatMap { it.cells.asSequence() }
    .flatMap { cell -> cell.faces.asSequence() }
    .mapNotNull { face -> face.candidates.firstOrNull() }
    .distinct()
    .toList()

/**
 * Fetches every photograph of the notebook into the cache before any page is drawn (#169). Fetching
 * page by page lost photographs: whatever missed a page's time budget became a hole in the PDF, and
 * once Numista answered `503`, throttled requests for pages already drawn held the four slots
 * (`ThrottleRetryInterceptor`). One queue asks for each photograph once, and the pages then read
 * from the cache.
 *
 * Failures aren't retried or reported here; the drawing pass counts the holes that reach paper.
 */
suspend fun warmNotebookPhotographs(
    context: Context,
    urls: List<String>,
    onProgress: (done: Int) -> Unit,
) {
    if (urls.isEmpty()) return
    // Atomic: four coroutines report into it, and a lost increment leaves the progress bar short.
    val done = AtomicInteger(0)
    // Success or failure is ignored: the drawing pass counts holes.
    warmPhotographs(
        context = context,
        loader = SingletonImageLoader.get(context),
        urls = urls,
        concurrency = WARM_CONCURRENCY,
        // As in #190: the page about to be drawn may want the same bitmap seconds later.
        memoryCache = CachePolicy.WRITE_ONLY,
    ) { _, _ -> onProgress(done.incrementAndGet()) }
}
