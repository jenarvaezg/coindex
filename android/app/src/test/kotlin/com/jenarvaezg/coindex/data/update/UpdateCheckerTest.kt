package com.jenarvaezg.coindex.data.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val API = "https://api.example"
private const val APK_URL = "$API/download/coindex-3.apk"
private const val MANIFEST_URL = "$API/download/update.json"

private fun release(assets: String) = """{"tag_name": "v0.3.0", "assets": [$assets]}"""

private fun asset(name: String, url: String, size: Long = 100) =
    """{"name": "$name", "browser_download_url": "$url", "size": $size}"""

private fun manifest(versionCode: Int, apkAsset: String = "coindex-3.apk") = """
    {
      "versionCode": $versionCode,
      "versionName": "0.3.0",
      "apkAsset": "$apkAsset",
      "notes": "Láminas exportables"
    }
"""

class UpdateCheckerTest {
    private fun checker(
        currentVersionCode: Int,
        releaseBody: String?,
        manifestBody: String? = null,
        releaseStatus: HttpStatusCode = HttpStatusCode.OK,
    ): Pair<UpdateChecker, MutableList<String>> {
        val requested = mutableListOf<String>()
        val engine = MockEngine { request ->
            requested += request.url.toString()
            when {
                request.url.encodedPath.endsWith("releases/latest") -> respond(
                    releaseBody ?: "",
                    releaseStatus,
                    headersOf("Content-Type", "application/json"),
                )
                request.url.toString() == MANIFEST_URL -> respond(
                    manifestBody ?: "",
                    if (manifestBody == null) HttpStatusCode.NotFound else HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json"),
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        return UpdateChecker(
            HttpClient(engine),
            currentVersionCode = currentVersionCode,
            repo = "jenarvaezg/coindex",
            apiBaseUrl = API,
        ) to requested
    }

    @Test
    fun `a higher version code offers the apk from the same release`() = runTest {
        val (checker, requested) = checker(
            currentVersionCode = 2,
            releaseBody = release(
                asset("update.json", MANIFEST_URL) + "," + asset("coindex-3.apk", APK_URL, 29L),
            ),
            manifestBody = manifest(versionCode = 3),
        )

        val status = checker.check()

        val available = status as UpdateStatus.Available
        assertEquals(3, available.manifest.versionCode)
        assertEquals("0.3.0", available.manifest.versionName)
        assertEquals(APK_URL, available.apkUrl)
        assertEquals(29L, available.apkSize)
        assertEquals("Láminas exportables", available.manifest.notes)
        assertTrue(requested.first().endsWith("/repos/jenarvaezg/coindex/releases/latest"))
    }

    @Test
    fun `the installed version and older releases are up to date`() = runTest {
        val assets = asset("update.json", MANIFEST_URL) + "," + asset("coindex-3.apk", APK_URL)
        val (same, _) = checker(3, release(assets), manifest(3))
        val (older, _) = checker(4, release(assets), manifest(3))

        assertEquals(UpdateStatus.UpToDate, same.check())
        assertEquals(UpdateStatus.UpToDate, older.check())
    }

    @Test
    fun `a release without a manifest is reported as unavailable, never as an update`() = runTest {
        val (checker, _) = checker(
            currentVersionCode = 2,
            releaseBody = release(asset("coindex-3.apk", APK_URL)),
        )

        val status = checker.check() as UpdateStatus.Unavailable

        assertTrue(status.reason.contains("update.json"), status.reason)
    }

    @Test
    fun `a manifest pointing at a missing apk never offers a broken download`() = runTest {
        val (checker, _) = checker(
            currentVersionCode = 2,
            releaseBody = release(asset("update.json", MANIFEST_URL)),
            manifestBody = manifest(versionCode = 3, apkAsset = "no-existe.apk"),
        )

        val status = checker.check() as UpdateStatus.Unavailable

        assertTrue(status.reason.contains("no-existe.apk"), status.reason)
    }

    @Test
    fun `no published release yet is not an error`() = runTest {
        val (checker, _) = checker(
            currentVersionCode = 2,
            releaseBody = null,
            releaseStatus = HttpStatusCode.NotFound,
        )

        val status = checker.check() as UpdateStatus.Unavailable

        assertTrue(status.reason.contains("ninguna versión"), status.reason)
    }

    @Test
    fun `an unreachable github never throws at the caller`() = runTest {
        val engine = MockEngine { throw java.io.IOException("sin red") }
        val checker = UpdateChecker(
            HttpClient(engine),
            currentVersionCode = 2,
            apiBaseUrl = API,
        )

        assertTrue(checker.check() is UpdateStatus.Unavailable)
    }

    @Test
    fun `malformed json is reported instead of crashing`() = runTest {
        val (checker, _) = checker(
            currentVersionCode = 2,
            releaseBody = release(asset("update.json", MANIFEST_URL)),
            manifestBody = "{esto no es json}",
        )

        assertTrue(checker.check() is UpdateStatus.Unavailable)
    }
}
