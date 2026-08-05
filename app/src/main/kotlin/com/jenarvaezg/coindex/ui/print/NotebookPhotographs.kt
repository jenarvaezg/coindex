package com.jenarvaezg.coindex.ui.print

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import com.jenarvaezg.coindex.data.photos.warmPhotographs
import java.util.concurrent.atomic.AtomicInteger

/**
 * How many photographs of the warm-up are in flight at once.
 *
 * The same four as `CoinPhotoLoader`'s dispatcher, on purpose: asking for more would only pile
 * them up one layer higher, where the progress counter cannot see them and cancelling does not
 * reach them. The background prefetch takes two of the four for the opposite reason (#191) — the
 * export is what the collector is watching, and it is allowed to take the lot.
 */
private const val WARM_CONCURRENCY = 4

/**
 * Every photograph the notebook needs, once.
 *
 * **Deduplicated**, which is most of the point: the same type shows up on several pages — two rows
 * of the same Southern Cross, a type two catalogs share — and the drawing pass would ask for it
 * once per cell.
 *
 * Only the **first** candidate of each cell, which is the thumbnail. The original behind it is the
 * fallback for a thumbnail that is refused (#67), and warming both would double the requests to
 * pre-empt a failure that mostly does not happen; a page that does fall back still asks for it
 * itself.
 */
fun notebookPhotographs(pages: List<PrintPage>): List<String> = pages
    .asSequence()
    .flatMap { it.cells.asSequence() }
    .mapNotNull { cell -> cell.reverse?.candidates?.firstOrNull() }
    .distinct()
    .toList()

/**
 * Fetches every photograph of the notebook into the cache **before** any page is drawn.
 *
 * This is the fix for an export that came out with 64 photographs out of some 600 and took half an
 * hour doing it (#169, reported from the field). Asking page by page looked equivalent and is not:
 *
 * - **A photograph got one chance and it was a page's chance.** Whatever had not arrived when the
 *   page's budget ran out was frozen into the PDF as a hole, with no second try — unlike a single
 *   plate, which the collector simply exports again.
 * - **Seventy bursts compete with each other.** The photographs go through four slots on purpose,
 *   and a throttled one **blocks its slot** while it waits (`ThrottleRetryInterceptor`). Once
 *   Numista starts answering `503`, the slots are held by requests whose page has already moved on,
 *   so the next page starts already behind and never catches up.
 *
 * Warming first turns that into one queue with one progress counter: each photograph is asked for
 * exactly once, the pages that follow read from the cache in milliseconds, and a second export of
 * the same collection costs no network at all.
 *
 * A photograph that fails is **not** retried here and not reported as an error: the drawing pass is
 * what counts holes, because that is what ends up on the paper. This only makes them rare.
 */
suspend fun warmNotebookPhotographs(
    context: Context,
    urls: List<String>,
    onProgress: (done: Int) -> Unit,
) {
    if (urls.isEmpty()) return
    // Atomic because four coroutines report into it, and the counter is what the collector is
    // watching: a lost increment is a progress bar that never reaches the end.
    val done = AtomicInteger(0)
    // Whether each one landed is deliberately ignored here: success or failure, what matters is
    // that the cache now holds whatever this URL is ever going to give, and the drawing pass is
    // what counts holes because that is what ends up on the paper.
    warmPhotographs(
        context = context,
        loader = SingletonImageLoader.get(context),
        urls = urls,
        concurrency = WARM_CONCURRENCY,
        // Unchanged from #190: the page about to be drawn may want the same bitmap seconds later.
        memoryCache = CachePolicy.WRITE_ONLY,
    ) { _, _ -> onProgress(done.incrementAndGet()) }
}
