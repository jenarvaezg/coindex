package com.jenarvaezg.coindex.data.photos

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
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
 *   is the number that can actually be reached, so «faltan 0» means «faltan 0».
 * @param missing how many of those are not in the disk cache yet.
 * @param bytes what the picture cache weighs right now.
 * @param held why they are not being brought at this moment, or null while they are.
 */
data class PhotoCacheStatus(
    val wanted: Int = 0,
    val missing: Int = 0,
    val bytes: Long = 0L,
    val held: PrefetchRefusal? = null,
)

/**
 * Whatever brings the catalog's photographs into the cache before anybody asks for them (#191).
 *
 * An interface because the real one needs Coil, a disk cache and a network: the *rules* around it —
 * when a pass is worth starting, what gives the network back to a sync — are [PhotoPrefetchLoop]'s,
 * and they are the part that had never been read by a test.
 */
interface PhotoPrefetch {
    /**
     * Fetches whatever is missing, or reports why it did not.
     *
     * @param held the reason not to ask for anything, already decided by [prefetchRefusal].
     * @param onStatus called with the counts before the first request and then as they land.
     */
    suspend fun run(
        images: Collection<TypeImages>,
        held: PrefetchRefusal?,
        onStatus: (PhotoCacheStatus) -> Unit = {},
    ): PhotoCacheStatus
}

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
 * There is deliberately **no ceiling per launch**: the collection is some 30 MB once in the life of
 * the phone, over wifi, and a ceiling would have meant four or five launches before the plates stop
 * filling in — which is most of the wait this is trying to remove.
 */
class CoilPhotoPrefetch(
    context: Context,
    private val gone: GonePhotographs,
    private val imageLoader: () -> ImageLoader = { SingletonImageLoader.get(context) },
) : PhotoPrefetch {
    private val appContext = context.applicationContext

    /**
     * Fetches whatever is missing, or reports why it did not.
     *
     * Cancelling is a first-class outcome: a sync cancels it, exporting the notebook cancels it, and
     * leaving the app cancels it. The cache keeps every photograph that had already landed.
     *
     * @param held the reason not to ask for anything, already decided by [prefetchRefusal]. Passed
     *   in rather than worked out here because the caller needs the same answer for its own state,
     *   and two readings of a phone that is changing underneath would not have to agree.
     * @param onStatus called with the counts **before** the first request — a settings screen
     *   opened during the first pass must show what is happening rather than «no hay fotos que
     *   traer» — and then every [PROGRESS_EVERY] photographs.
     */
    override suspend fun run(
        images: Collection<TypeImages>,
        held: PrefetchRefusal?,
        onStatus: (PhotoCacheStatus) -> Unit,
    ): PhotoCacheStatus = withContext(Dispatchers.IO) {
        val wanted = photographsToPrefetch(images, gone.all())
        val missing = wanted.filterNot { isCached(it) }
        val opening = PhotoCacheStatus(wanted.size, missing.size, cacheBytes(), held)
        onStatus(opening)
        if (missing.isEmpty() || held != null) return@withContext opening
        val landed = AtomicInteger(0)
        val asked = AtomicInteger(0)
        warmPhotographs(
            context = appContext,
            loader = imageLoader(),
            urls = missing,
            concurrency = PREFETCH_CONCURRENCY,
            memoryCache = CachePolicy.DISABLED,
        ) { _, ok ->
            if (ok) landed.incrementAndGet()
            // Counted apart from the ones that arrived: a photograph that failed is still missing,
            // and a progress line that counted it as brought would be a lie that settles itself
            // only at the end of the pass.
            if (asked.incrementAndGet() % PROGRESS_EVERY == 0) {
                // The size is read again each time: on a first run it starts at zero, and a line
                // that said «0,0 MB» while six hundred pictures landed would be the one number on
                // the card that is checkable and wrong.
                onStatus(opening.copy(missing = missing.size - landed.get(), bytes = cacheBytes()))
            }
        }
        // Counted again rather than derived from what landed: the interceptor may have learnt in
        // the meantime that some of these are gone, and those stop being missing — they stop being
        // wanted. Anything that merely failed is still missing, and is asked for on the next launch.
        val stillWanted = photographsToPrefetch(images, gone.all())
        PhotoCacheStatus(stillWanted.size, stillWanted.count { !isCached(it) }, cacheBytes(), held)
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
