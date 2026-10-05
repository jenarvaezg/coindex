package com.jenarvaezg.coindex.data.photos

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * How many of `CoinPhotoLoader`'s four slots the prefetch may hold. The dispatcher serves requests
 * in arrival order, so leaving slots free keeps an opened plate from queueing behind the prefetch.
 */
private const val PREFETCH_CONCURRENCY = 2

/**
 * What the phone holds of the catalog's photographs, and what it is still missing.
 *
 * @param wanted every photograph the index would draw, minus the ones Numista says are gone, so
 *   «faltan 0» is reachable.
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
 * An interface so the rules around it, in [PhotoPrefetchLoop], can be tested without Coil, a disk
 * cache or a network.
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
 * Brings the catalog's photographs into the cache before anybody asks for them (#191), so a plate
 * opens with its pictures and a notebook export (#190) starts drawing at once.
 *
 * - It only asks for what is missing: the disk cache is checked first, so a later launch costs no
 *   network and the count in settings is exact.
 * - It never outranks the screen ([PREFETCH_CONCURRENCY]).
 * - It is resumable and idempotent: each photograph is independent and only the cache is written,
 *   so what didn't arrive is asked for next launch. Hence no `WorkManager`.
 * - It is silent: one line in settings, to tell «missing, on wifi» from «missing, on mobile data».
 *
 * No per-launch ceiling: the download happens once in the phone's life, over wifi, and a ceiling
 * would spread the wait over several launches.
 */
class CoilPhotoPrefetch(
    context: Context,
    private val gone: GonePhotographs,
    private val imageLoader: () -> ImageLoader = { SingletonImageLoader.get(context) },
) : PhotoPrefetch {
    private val appContext = context.applicationContext

    /**
     * Fetches whatever is missing, or reports why it did not. A sync, a notebook export or leaving
     * the app cancels it; the cache keeps what had already landed.
     *
     * @param held the reason not to ask for anything, decided by [prefetchRefusal] in the caller so
     *   both read the same state of a changing phone.
     * @param onStatus called with the counts before the first request, so settings opened during
     *   the first pass shows progress, and then every [PREFETCH_PROGRESS_EVERY] photographs.
     */
    override suspend fun run(
        images: Collection<TypeImages>,
        held: PrefetchRefusal?,
        onStatus: (PhotoCacheStatus) -> Unit,
    ): PhotoCacheStatus = withContext(Dispatchers.IO) {
        val wanted = photographsToPrefetch(images, gone.all())
        val missing = wanted.filterNot { isCached(it) }
        val opening = photoCacheStatus(wanted, ::isCached, cacheBytes(), held)
        onStatus(opening)
        if (prefetchAlreadySettled(missing.size, held)) return@withContext opening
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
            // Counted apart from the ones that landed: a failed photograph is still missing.
            if (shouldReportPrefetchProgress(asked.incrementAndGet())) {
                // The size is read again each time; on a first run it starts at zero.
                onStatus(
                    opening.copy(
                        missing = prefetchMissingAfter(missing.size, landed.get()),
                        bytes = cacheBytes(),
                    ),
                )
            }
        }
        // Recounted rather than derived from what landed: photographs the interceptor found gone
        // meanwhile stop being wanted, and failed ones are still missing.
        photoCacheStatus(photographsToPrefetch(images, gone.all()), ::isCached, cacheBytes(), held)
    }

    private fun cacheBytes(): Long = imageLoader().diskCache?.size ?: 0L

    /**
     * Whether this URL is already on disk, under the same key the screens use. The snapshot must be
     * closed or the entry stays locked against eviction.
     */
    private fun isCached(url: String): Boolean {
        val cache = imageLoader().diskCache ?: return false
        return cache.openSnapshot(url)?.use { true } ?: false
    }
}
