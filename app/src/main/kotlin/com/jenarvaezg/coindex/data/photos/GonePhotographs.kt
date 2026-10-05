package com.jenarvaezg.coindex.data.photos

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * How long a photograph Numista answered `404` for is left alone. Forgotten after a month, so a CDN
 * glitch doesn't remove a picture for good, while a truly missing one costs one request a month.
 */
private const val GONE_MEMORY_MILLIS = 30L * 24 * 60 * 60 * 1_000

/** The photographs Numista has answered `404` for, so they are not asked for on every launch. */
interface GonePhotographs {
    fun all(): Set<String>

    fun remember(url: String)
}

/** Which of the remembered photographs are still to be left alone at [now]; testable arithmetic. */
fun stillGone(remembered: Map<String, Long>, now: Long): Set<String> = remembered
    .filterValues { refusedAt -> now - refusedAt < GONE_MEMORY_MILLIS }
    .keys

/**
 * Remembers them on the device, because the prefetch runs on every launch (#191) and Coil's disk
 * cache keeps no trace of a missing photograph; without this, settings would count them as missing
 * forever. One preference key per URL, holding when it was refused, so each expires on its own.
 */
class StoredGonePhotographs(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : GonePhotographs {
    private val appContext = context.applicationContext

    // Lazily: this is built with the image loader, on the main thread; its readers (the
    // interceptor and the prefetch) are already off it.
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    @Suppress("UNCHECKED_CAST")
    override fun all(): Set<String> =
        stillGone(prefs.all as Map<String, Long>, nowMillis())

    override fun remember(url: String) {
        prefs.edit().putLong(url, nowMillis()).apply()
    }

    private companion object {
        const val PREFS = "coindex-photos-gone"
    }
}

/**
 * Writes down the photographs Numista says are not there (#191), from any request, not just the
 * prefetch's. Installed outside `ThrottleRetryInterceptor`, so it records the final answer.
 */
internal class GonePhotographInterceptor(private val gone: GonePhotographs) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (PhotoRetryPolicy.isGone(response.code)) {
            gone.remember(chain.request().url.toString())
        }
        return response
    }
}
