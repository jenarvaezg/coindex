package com.jenarvaezg.coindex.data.photos

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.jenarvaezg.coindex.data.TypeImages
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * How many of `CoinPhotoLoader`'s four slots the prefetch is allowed to hold.
 *
 * **Fewer than there are**, which is the point: the dispatcher serves requests in the order they
 * arrive, so a prefetch running at four would park the plate the collector just opened behind six
 * hundred pictures nobody asked for. At two, a screen always finds a slot.
 */
private const val PREFETCH_CONCURRENCY = 2

/** How often the collector-visible count is updated while the prefetch runs. */
private const val PROGRESS_EVERY = 25

/**
 * What the phone holds of the catalog's photographs, and what it is still missing.
 *
 * @param wanted every photograph the index would draw, minus the ones Numista says are gone: this
 *   is the number that can actually be reached, so «faltan 0» means «faltan 0» for ever.
 * @param missing how many of those are not in the disk cache yet.
 * @param cacheBytes what Coil's disk cache weighs right now.
 */
data class PrefetchReport(val wanted: Int, val missing: Int, val cacheBytes: Long)

/**
 * Brings the catalog's photographs into the cache before anybody asks for them (#191).
 *
 * The export of the notebook already fetches every picture it needs up front (#190), and that made
 * it reliable; this makes it free. The same photographs sitting in the disk cache mean an export
 * starts drawing straight away and — the reason this is worth doing at all — **a plate opens with
 * its pictures already on it** instead of filling in before the collector's eyes.
 *
 * Four properties hold it to being an optimization rather than a feature:
 *
 * - **It only asks for what is missing.** The disk cache is consulted first, so a second launch
 *   costs no network at all and the count in settings is the truth rather than an estimate.
 * - **It never outranks the screen.** [PREFETCH_CONCURRENCY] of the loader's four slots.
 * - **It is resumable and idempotent.** Every photograph is independent and nothing is written but
 *   the cache itself, so being killed halfway leaves nothing half-done: whatever did not arrive
 *   today is asked for on the next launch. That is also why there is no `WorkManager` here.
 * - **It is silent.** No snackbar, no banner, nothing to dismiss. The one line it is allowed to say
 *   is in the settings screen, and only because «faltan 320, hay wifi» and «faltan 320, estás con
 *   datos» are different situations for the collector.
 *
 * There is deliberately **no ceiling per launch**: the collection is some 22 MB once in the life of
 * the phone, over wifi, and a ceiling would have meant four or five launches before the plates stop
 * filling in — which is most of the wait this is trying to remove.
 */
class PhotoPrefetch(
    context: Context,
    private val gone: GonePhotographs,
    private val imageLoader: () -> ImageLoader = { SingletonImageLoader.get(context) },
) {
    private val appContext = context.applicationContext

    /**
     * What is in the cache and what is not, without asking the network for anything.
     *
     * This is what the settings screen reads, and what the prefetch itself starts from.
     */
    suspend fun report(images: Collection<TypeImages>): PrefetchReport =
        withContext(Dispatchers.IO) {
            val wanted = photographsToPrefetch(images, gone.all())
            PrefetchReport(
                wanted = wanted.size,
                missing = wanted.count { !isCached(it) },
                cacheBytes = imageLoader().diskCache?.size ?: 0L,
            )
        }

    /**
     * Fetches whatever is missing, or reports why it did not.
     *
     * Cancelling is a first-class outcome: the sync cancels it, and leaving the app cancels it. The
     * cache keeps every photograph that had already landed.
     *
     * @param onProgress the missing count as it comes down, so a settings screen left open shows
     *   the work happening. Called every [PROGRESS_EVERY] photographs rather than on each one — six
     *   hundred state updates would be six hundred recompositions to move a number.
     */
    suspend fun run(
        images: Collection<TypeImages>,
        conditions: PrefetchConditions,
        onProgress: (missing: Int) -> Unit = {},
    ): PrefetchReport = withContext(Dispatchers.IO) {
        val wanted = photographsToPrefetch(images, gone.all())
        val missing = wanted.filterNot { isCached(it) }
        if (missing.isEmpty() || prefetchRefusal(conditions) != null) {
            return@withContext PrefetchReport(wanted.size, missing.size, cacheBytes())
        }
        val landed = AtomicInteger(0)
        warmPhotographs(appContext, imageLoader(), missing, PREFETCH_CONCURRENCY) { _, ok ->
            val done = landed.incrementAndGet()
            if (ok && done % PROGRESS_EVERY == 0) onProgress(missing.size - done)
        }
        // Counted again rather than derived from what landed: the interceptor may have learnt in
        // the meantime that some of these are gone, and those stop being missing — they stop being
        // wanted. Anything that merely failed is still missing, and is asked for on the next launch.
        val stillWanted = photographsToPrefetch(images, gone.all())
        PrefetchReport(stillWanted.size, stillWanted.count { !isCached(it) }, cacheBytes())
    }

    private fun cacheBytes(): Long = imageLoader().diskCache?.size ?: 0L

    /**
     * Whether this URL is already on disk.
     *
     * The disk cache is keyed by the request's data — the URL — so this is the same key the screens
     * will hit later. The snapshot has to be closed or the entry stays locked against eviction.
     */
    private fun isCached(url: String): Boolean {
        val cache = imageLoader().diskCache ?: return false
        return cache.openSnapshot(url)?.use { true } ?: false
    }
}
