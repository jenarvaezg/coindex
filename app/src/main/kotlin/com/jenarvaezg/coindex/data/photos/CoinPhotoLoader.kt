package com.jenarvaezg.coindex.data.photos

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Path.Companion.toOkioPath

/** How many photographs are asked of Numista at the same time. */
private const val MAX_CONCURRENT_PHOTOS = 4

/**
 * How much disk the catalog's photographs may keep. Coil's default, 2 % of free space, can be
 * smaller than the finite set the prefetch fetches on purpose (#191), and then every launch would
 * evict and re-download the same photographs. 128 MB leaves room for growth and for the originals
 * behind refused thumbnails; it is under `cache/`, so the system can still reclaim it.
 */
private const val PHOTO_DISK_CACHE_BYTES = 128L * 1024 * 1024

/**
 * Coil's default directory, named because the cache is configured by hand; a different one would
 * abandon every photograph already cached.
 */
private const val PHOTO_DISK_CACHE_DIR = "coil3_disk_cache"

/**
 * The client every catalog photograph is fetched with (#67): it identifies itself
 * ([coinPhotoUserAgent]), asks for [MAX_CONCURRENT_PHOTOS] at a time so a sheet composing every
 * cell at once queues instead of bursting, and retries throttling ([PhotoRetryPolicy]).
 */
fun coinPhotoImageLoader(
    context: PlatformContext,
    userAgent: String,
    gonePhotographs: GonePhotographs,
): ImageLoader =
    ImageLoader.Builder(context)
        // Exporting a plate replays it onto a software canvas, which can't draw hardware bitmaps.
        .allowHardware(false)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(PHOTO_DISK_CACHE_DIR).toOkioPath())
                .maxSizeBytes(PHOTO_DISK_CACHE_BYTES)
                .build()
        }
        .components {
            // Coil calls this factory once, so every photograph shares the dispatcher's slots.
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { coinPhotoHttpClient(userAgent, gonePhotographs) },
                ),
            )
        }
        .build()

private fun coinPhotoHttpClient(
    userAgent: String,
    gonePhotographs: GonePhotographs,
): OkHttpClient = OkHttpClient.Builder()
    .dispatcher(
        Dispatcher().apply {
            maxRequests = MAX_CONCURRENT_PHOTOS
            maxRequestsPerHost = MAX_CONCURRENT_PHOTOS
        },
    )
    .addInterceptor(UserAgentInterceptor(userAgent))
    // Outside the retry interceptor, so it records the final answer, not the first attempt.
    .addInterceptor(GonePhotographInterceptor(gonePhotographs))
    .addInterceptor(ThrottleRetryInterceptor())
    .build()

private class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder().header("User-Agent", userAgent).build(),
        )
}

/**
 * Retries a throttled photograph instead of giving the cell up. An application interceptor, so it
 * sits above OkHttp's redirects and connection retries. The refused response is closed before
 * asking again, or its body leaks the connection.
 */
private class ThrottleRetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 1
        while (true) {
            val response = chain.proceed(chain.request())
            if (response.isSuccessful || !PhotoRetryPolicy.isRetryable(response.code)) {
                return response
            }
            val wait = PhotoRetryPolicy.delayMillis(
                attempt = attempt,
                retryAfterSeconds = PhotoRetryPolicy.retryAfterSeconds(
                    response.header("Retry-After"),
                ),
            ) ?: return response
            // A cell that scrolled away must not hold a slot while waiting. Returned unclosed, like
            // every give-up path: the caller owns the body.
            if (chain.call().isCanceled()) return response
            response.close()
            // Blocking on purpose: holding the slot while waiting is the backpressure.
            Thread.sleep(wait)
            attempt += 1
        }
    }
}
