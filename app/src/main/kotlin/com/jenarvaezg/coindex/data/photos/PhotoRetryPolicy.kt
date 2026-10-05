package com.jenarvaezg.coindex.data.photos

/**
 * When a catalog photograph is worth asking for again, and how long to wait first. Numista's edge
 * answers bursts with `503` that succeed when asked again, and Coil treats the first failure as
 * final, leaving cells empty, in exports too (#67). Kept apart from OkHttp so it can be tested.
 */
object PhotoRetryPolicy {
    /** Attempts of one request, the first one included. */
    const val MAX_ATTEMPTS: Int = 3

    private const val FIRST_DELAY_MILLIS = 400L
    private const val BACKOFF_FACTOR = 3L

    /**
     * The longest a server's own `Retry-After` is honoured. A plate export waits for every
     * photograph, so a long wait means a failed export; past this the wait is capped.
     */
    private const val MAX_DELAY_MILLIS = 5_000L

    /**
     * Whether an HTTP status is worth a second try: `429` and `5xx` are throttling or a bad moment
     * at the edge, and `408` is a timeout. Other `4xx` are answers about the picture itself.
     */
    fun isRetryable(status: Int): Boolean =
        status == 408 || status == 429 || status in 500..599

    /**
     * Whether the picture is gone for good, so the prefetch (#191) remembers it instead of asking
     * on every launch. Not `403`: Cloudflare answers it to every photograph when the `User-Agent`
     * is missing (ADR 0017), and remembering it could switch off the whole catalog for good.
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
