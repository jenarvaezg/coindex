package com.jenarvaezg.coindex.data.photos

import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Only a `404` or a `410` marks a photograph as gone (#191, #224). */
class GonePhotographInterceptorTest {
    @Test
    fun `a 404 is written down so the next launch does not ask again`() {
        val gone = RecordingGonePhotographs()
        val url = "https://en.numista.com/catalogue/photos/gone.jpg"

        GonePhotographInterceptor(gone).intercept(FixedChain(url, code = 404))

        assertEquals(listOf(url), gone.remembered)
    }

    @Test
    fun `a 410 is gone the same way a 404 is`() {
        val gone = RecordingGonePhotographs()
        val url = "https://en.numista.com/catalogue/photos/removed.jpg"

        GonePhotographInterceptor(gone).intercept(FixedChain(url, code = 410))

        assertEquals(listOf(url), gone.remembered)
    }

    @Test
    fun `a throttle or a refusal is not remembered as gone`() {
        val gone = RecordingGonePhotographs()
        val url = "https://en.numista.com/catalogue/photos/busy.jpg"

        listOf(403, 429, 500, 503).forEach { code ->
            GonePhotographInterceptor(gone).intercept(FixedChain(url, code = code))
        }

        assertEquals(emptyList(), gone.remembered)
    }
}

/** In-memory [GonePhotographs] that records every `remember` call. */
private class RecordingGonePhotographs : GonePhotographs {
    val remembered = mutableListOf<String>()
    private val known = mutableSetOf<String>()

    override fun all(): Set<String> = known.toSet()

    override fun remember(url: String) {
        remembered += url
        known += url
    }
}

/** An OkHttp chain whose [proceed] answers a fixed status without touching the network. */
private class FixedChain(
    url: String,
    private val code: Int,
) : Interceptor.Chain {
    private val request = Request.Builder().url(url).build()

    override fun request(): Request = request

    override fun proceed(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("test")
        .body(ByteArray(0).toResponseBody())
        .build()

    override fun connection(): Connection? = null

    override fun call(): Call = error("FixedChain has no Call")

    override fun connectTimeoutMillis(): Int = 0

    override fun readTimeoutMillis(): Int = 0

    override fun writeTimeoutMillis(): Int = 0

    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}
