package com.jenarvaezg.coindex.data.photos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ten of the thirty-eight photographs of a plate came back `503`, and the same URLs asked for
 * one at a time came back `200`: the pictures were never dead, they were throttled. Coil takes
 * the first failure as final, so each of those left a cell empty for good and the export froze
 * the hole into the sheet that gets shared (issue #67).
 */
class PhotoRetryPolicyTest {
    @Test
    fun `throttling and a bad moment at the edge are worth asking again`() {
        listOf(408, 429, 500, 502, 503, 504).forEach { status ->
            assertTrue(PhotoRetryPolicy.isRetryable(status), "$status debería reintentarse")
        }
    }

    @Test
    fun `an answer about the picture itself is not repeated`() {
        // 403 is what Cloudflare says with no User-Agent and 404 is a picture that is gone:
        // neither changes by asking twice, and both cost the collector's battery.
        listOf(400, 401, 403, 404, 410).forEach { status ->
            assertFalse(PhotoRetryPolicy.isRetryable(status), "$status no debería reintentarse")
        }
    }

    @Test
    fun `the wait grows between attempts and then runs out`() {
        val waits = (1..PhotoRetryPolicy.MAX_ATTEMPTS).map { PhotoRetryPolicy.delayMillis(it) }

        assertEquals(listOf(400L, 1_200L, null), waits)
    }

    @Test
    fun `the server's own Retry-After wins when it asks for longer`() {
        assertEquals(2_000L, PhotoRetryPolicy.delayMillis(attempt = 1, retryAfterSeconds = 2))
        // Shorter than the backoff, so the backoff stands: it is the pile-up being drained.
        assertEquals(1_200L, PhotoRetryPolicy.delayMillis(attempt = 2, retryAfterSeconds = 1))
    }

    @Test
    fun `an export is not parked for a minute because a header said so`() {
        assertEquals(5_000L, PhotoRetryPolicy.delayMillis(attempt = 1, retryAfterSeconds = 60))
    }

    @Test
    fun `a Retry-After that is not a number of seconds is ignored rather than guessed at`() {
        // The HTTP date form is legal and rare; reading it wrong would park the export.
        assertNull(PhotoRetryPolicy.retryAfterSeconds("Wed, 21 Oct 2026 07:28:00 GMT"))
        assertNull(PhotoRetryPolicy.retryAfterSeconds(null))
        assertEquals(3L, PhotoRetryPolicy.retryAfterSeconds(" 3 "))
    }

    @Test
    fun `the app's User-Agent says who it is and where to complain`() {
        // Without any User-Agent Cloudflare answers 403 to every photograph. Today the header
        // is whatever OkHttp writes underneath Coil, so the pictures work by inertia.
        assertEquals(
            "Coindex/0.8.0 (+https://github.com/jenarvaezg/coindex)",
            coinPhotoUserAgent("0.8.0"),
        )
        assertEquals(
            "Coindex/dev (+https://github.com/jenarvaezg/coindex)",
            coinPhotoUserAgent(""),
        )
    }
}
