package com.jenarvaezg.coindex.data.photos

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * The size a warm-up decodes at: that of Numista's thumbnails (ADR 0017). A warm-up is for the disk
 * cache, keyed by URL alone, so the decode size only needs to be small.
 */
private const val WARM_SIZE_PX = 180

/**
 * Fetches photographs into the cache, [concurrency] at a time, reporting each one as it lands. The
 * notebook export takes all four of `CoinPhotoLoader`'s slots, since the collector is waiting on
 * it; the background prefetch (#191) takes two, so a plate opened meanwhile always finds a slot.
 * Failures are not retried here: `ThrottleRetryInterceptor` already did.
 *
 * @param memoryCache the export writes to the memory cache, since the page it draws next wants
 *   the same bitmaps; the prefetch leaves it alone, so it doesn't evict what the screen is using.
 * @param onDone called once per URL, on the coroutine that fetched it, with whether it landed.
 */
suspend fun warmPhotographs(
    context: Context,
    loader: ImageLoader,
    urls: List<String>,
    concurrency: Int,
    memoryCache: CachePolicy,
    onDone: (url: String, landed: Boolean) -> Unit,
) {
    if (urls.isEmpty()) return
    val gate = Semaphore(concurrency)
    coroutineScope {
        urls.forEach { url ->
            launch {
                val landed = gate.withPermit {
                    runCatching {
                        loader.execute(
                            ImageRequest.Builder(context)
                                .data(url)
                                .size(WARM_SIZE_PX, WARM_SIZE_PX)
                                .memoryCachePolicy(memoryCache)
                                .build(),
                        )
                    }.getOrNull() is SuccessResult
                }
                onDone(url, landed)
            }
        }
    }
}
