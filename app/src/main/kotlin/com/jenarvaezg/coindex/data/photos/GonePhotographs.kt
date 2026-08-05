package com.jenarvaezg.coindex.data.photos

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * How long a photograph Numista answered `404` for is left alone.
 *
 * Forgetting matters as much as remembering: a CDN having a bad minute would otherwise take that
 * picture out of the catalog on this phone **for ever**, invisibly, and not even clearing the cache
 * would bring it back. A month is long enough that a genuinely missing picture costs one request a
 * month, and short enough that a mistake repairs itself without anybody noticing it happened.
 */
private const val GONE_MEMORY_MILLIS = 30L * 24 * 60 * 60 * 1_000

/** The photographs Numista has answered `404` for, so they are not asked for on every launch. */
interface GonePhotographs {
    fun all(): Set<String>

    fun remember(url: String)
}

/**
 * Which of the remembered photographs are still to be left alone, at [now].
 *
 * Kept apart from the preferences so the forgetting can be read and tested as what it is:
 * arithmetic on a timestamp.
 */
fun stillGone(remembered: Map<String, Long>, now: Long): Set<String> = remembered
    .filterValues { refusedAt -> now - refusedAt < GONE_MEMORY_MILLIS }
    .keys

/**
 * Remembers them on the device, because the prefetch runs on every launch (#191).
 *
 * Coil's disk cache only ever holds answers that arrived, so a photograph that is **not** there
 * leaves no trace at all: without this list the prefetch would ask Numista for the same missing
 * picture every time the app opens, and the counter in settings would say «faltan 12» for ever over
 * twelve pictures that do not exist.
 *
 * One preference key per URL, holding when it was refused. There are a handful of these — a
 * catalog photograph that is missing is rare — and a key each is what makes them expire
 * independently.
 */
class StoredGonePhotographs(
    context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : GonePhotographs {
    private val appContext = context.applicationContext

    // Lazily, and never from the constructor: this object is built while the image loader is being
    // created, which happens on the main thread the first time anything draws a coin. Every reader
    // of it — the interceptor and the prefetch — is already off it.
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
 * Writes down the photographs Numista says are not there (#191).
 *
 * It listens to every photograph rather than only to the prefetch's, because a screen finding out
 * that a picture is gone is exactly as good a source. Installed outside `ThrottleRetryInterceptor`,
 * so what it writes down is the last word on the request rather than the first of three attempts.
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
