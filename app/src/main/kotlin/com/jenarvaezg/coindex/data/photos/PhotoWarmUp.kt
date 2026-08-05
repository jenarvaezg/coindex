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
 * The size a warm-up decodes at, which is the size Numista's thumbnails are (ADR 0017).
 *
 * What a warm-up is really for is the **disk** cache, which is keyed by URL alone, so whatever asks
 * for the picture afterwards hits it at whatever diameter it then wants. Decoding to something is
 * unavoidable, so it decodes to the smallest honest thing rather than to the original.
 */
private const val WARM_SIZE_PX = 180

/**
 * Fetches photographs into the cache, [concurrency] at a time, reporting each one as it lands.
 *
 * The concurrency is the caller's because it is the whole difference between the two warm-ups this
 * app has. The notebook export takes all four of `CoinPhotoLoader`'s slots, because nothing else is
 * happening and the collector is watching a progress bar; the background prefetch (#191) takes two
 * of the four, so a plate the collector opens meanwhile always finds a free slot and its pictures
 * overtake the ones nobody asked for.
 *
 * A photograph that fails is reported as such and never retried here: `ThrottleRetryInterceptor`
 * has already tried it three times, and whatever is left is either gone or not coming today.
 *
 * @param memoryCache what this warm-up does to the memory cache. The export writes to it — the page
 *   it is about to draw may want the same bitmap seconds later — and the prefetch leaves it alone
 *   entirely: sixteen hundred thumbnails written to memory would evict the ones the screen wants,
 *   and none of them is the size a 40 mm cell asks for anyway.
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
