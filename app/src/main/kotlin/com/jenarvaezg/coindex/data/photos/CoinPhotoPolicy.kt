package com.jenarvaezg.coindex.data.photos

/**
 * The app's own name, version and address, for the log of a catalogue that owes us nothing.
 *
 * Without any `User-Agent` Cloudflare answers `403` to every photograph, and until now the
 * header was whatever OkHttp writes underneath Coil: the pictures worked by inertia rather than
 * by decision, and a change of network engine would have turned all of them off at once.
 */
fun coinPhotoUserAgent(versionName: String): String {
    val version = versionName.ifBlank { "dev" }
    return "Coindex/$version (+https://github.com/jenarvaezg/coindex)"
}

/**
 * When a catalog photograph is worth asking for again, and how long to wait first.
 *
 * A sheet of nineteen issues asks Numista for thirty-eight photographs at once and its edge
 * answers some of them with `503`; the very same URLs, asked for one at a time, answer `200`.
 * Coil treats the first failure as final, so a throttled request left a cell empty for the rest
 * of the plate's life and the export froze that hole into the shared sheet (issue #67).
 *
 * Kept apart from OkHttp so the rule can be read and tested as what it is: arithmetic.
 */
object PhotoRetryPolicy {
    /** Attempts of one request, the first one included. */
    const val MAX_ATTEMPTS: Int = 3

    private const val FIRST_DELAY_MILLIS = 400L
    private const val BACKOFF_FACTOR = 3L

    /**
     * The longest a server's own `Retry-After` is honoured.
     *
     * A plate export waits for every photograph before it can capture, so a request parked for
     * a minute is a failed export, not a slow one: past this the wait is capped and the attempt
     * is spent anyway.
     */
    private const val MAX_DELAY_MILLIS = 5_000L

    /**
     * Whether an HTTP status is worth a second try.
     *
     * `429` and `503` are the throttle saying «not now»; the rest of `5xx` is the edge having a
     * bad moment. A `404` or a `403` is an answer about the picture itself and repeating it only
     * wastes the collector's battery — with one exception left alone on purpose: `408`, which is
     * the server timing the request out rather than judging it.
     */
    fun isRetryable(status: Int): Boolean =
        status == 408 || status == 429 || status in 500..599

    /**
     * Whether the answer means the picture is not there and is not coming back.
     *
     * Worth telling apart from «not retryable» because of the prefetch (#191), which runs on every
     * launch: a photograph that is merely refused is asked for again another day, and one that is
     * **gone** is remembered so it stops costing a request for ever.
     *
     * `403` is deliberately outside this. Without a `User-Agent` Cloudflare answers `403` to every
     * photograph (ADR 0017), so remembering it would let one bad afternoon at the edge switch the
     * whole catalog off on this phone, permanently and invisibly.
     */
    fun isGone(status: Int): Boolean = status == 404 || status == 410

    /**
     * How long to wait before [attempt] + 1, or null when the attempts are spent.
     *
     * [attempt] is 1-based. [retryAfterSeconds] is the server's own instruction, which wins over
     * the backoff whenever it is longer, up to the cap.
     */
    fun delayMillis(attempt: Int, retryAfterSeconds: Long? = null): Long? {
        if (attempt < 1 || attempt >= MAX_ATTEMPTS) return null
        var delay = FIRST_DELAY_MILLIS
        repeat(attempt - 1) { delay *= BACKOFF_FACTOR }
        val asked = retryAfterSeconds?.takeIf { it >= 0 }?.times(1_000L) ?: 0L
        return maxOf(delay, asked).coerceAtMost(MAX_DELAY_MILLIS)
    }

    /** Numista's `Retry-After`, when it sends one and it is a plain number of seconds. */
    fun retryAfterSeconds(header: String?): Long? = header?.trim()?.toLongOrNull()
}
