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
 * How much disk the catalog's photographs are allowed to keep.
 *
 * Coil's own default is 2 % of the free space, which is a sensible rule for an app whose pictures
 * are a stream nobody expects to hold. This one's are a **finite set that is fetched on purpose**
 * (#191): measured on a device, the whole seeded catalog is 1.658 pictures and 29,8 MB. Left at
 * 2 %, a phone with a gigabyte and a half free would keep a cache smaller than the set being
 * prefetched — so every launch would fetch the tail, evict the head, and pay for the same
 * photographs over and over on the collector's data.
 *
 * 128 MB is that set with room to grow and for the originals that stand behind refused thumbnails.
 * It is a `cache/` directory, so the system is still free to reclaim all of it under real pressure.
 */
private const val PHOTO_DISK_CACHE_BYTES = 128L * 1024 * 1024

/**
 * The directory Coil would have chosen by itself, named here because the size is no longer the
 * default: pointing the cache anywhere else would abandon every photograph the phones already have.
 */
private const val PHOTO_DISK_CACHE_DIR = "coil3_disk_cache"

/**
 * The client every catalog photograph is fetched with.
 *
 * Three things, all of them consequences of issue #67:
 *
 * - **It says who it is.** See [coinPhotoUserAgent].
 * - **It asks for a few at a time.** A sheet composes every cell at once and OkHttp's own
 *   per-host default is five; four is this app's own number rather than an inherited one, and
 *   an export of thirty-eight photographs is a queue instead of a burst.
 * - **It tries again.** See [PhotoRetryPolicy].
 */
fun coinPhotoImageLoader(
    context: PlatformContext,
    userAgent: String,
    gonePhotographs: GonePhotographs,
): ImageLoader =
    ImageLoader.Builder(context)
        // Hardware bitmaps are disabled because exporting a plate replays it onto a software
        // canvas, which cannot draw them. The catalog pictures are small, so the cost is noise.
        .allowHardware(false)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(PHOTO_DISK_CACHE_DIR).toOkioPath())
                .maxSizeBytes(PHOTO_DISK_CACHE_BYTES)
                .build()
        }
        .components {
            // Coil holds this factory lazily and calls it once, so the four-slot dispatcher
            // below is shared by every photograph. A client per request would cap nothing.
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
    // Outside the retry interceptor, so what it writes down is the last word on the request
    // rather than the first of three attempts.
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
 * Retries a throttled photograph instead of giving the cell up for lost.
 *
 * An application interceptor rather than a network one: it must survive the redirect and the
 * connection retry OkHttp does underneath, and it is the last word before Coil sees a failure.
 * The refused response is closed before asking again, or its body leaks the connection.
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
            // A cell that has scrolled away must not hold one of the four slots for its wait.
            // Returned unclosed, like every other give-up path: the caller owns the body.
            if (chain.call().isCanceled()) return response
            response.close()
            // Blocking on purpose: the wait is the backpressure, and holding one of the four
            // slots is what stops the next photograph from adding to the pile-up.
            Thread.sleep(wait)
            attempt += 1
        }
    }
}
