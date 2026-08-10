package com.jenarvaezg.coindex.data.photos

import kotlin.test.Test
import kotlin.test.assertEquals

/** Who we say we are when asking Numista for a photograph (ADR 0017). */
class CoinPhotoUserAgentTest {
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
