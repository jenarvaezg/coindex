package com.jenarvaezg.coindex.data.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val API = "https://api.example"
private const val MANIFEST_URL = "$API/download/update.json"
private const val APK_URL = "$API/download/coindex-24.apk"

/**
 * Asking GitHub for a newer APK, and installing it (ADR 0011, #220).
 *
 * Two things had no way of being read before: **how often it is allowed to ask** — six hours of
 * arithmetic on a clock read in place, on a gesture that fires on every return to the front — and
 * the four refusals of installing, none of which can be provoked on a device on purpose.
 */
class UpdateFlowTest {
    private val asked = mutableListOf<String>()
    private val installer = FakeUpdateInstaller()

    private val checker = UpdateChecker(
        HttpClient(
            MockEngine { request ->
                asked += request.url.toString()
                val body = if (request.url.encodedPath.endsWith("releases/latest")) {
                    """
                    {"tag_name": "v0.16.0", "assets": [
                      {"name": "update.json", "browser_download_url": "$MANIFEST_URL", "size": 1},
                      {"name": "coindex-24.apk", "browser_download_url": "$APK_URL", "size": 29}
                    ]}
                    """
                } else {
                    """
                    {"versionCode": 24, "versionName": "0.16.0",
                     "apkAsset": "coindex-24.apk", "notes": "Costuras del ViewModel"}
                    """
                }
                respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            },
        ),
        currentVersionCode = 23,
        repo = "jenarvaezg/coindex",
        apiBaseUrl = API,
    )

    private fun flow(now: () -> Long) = UpdateFlow(checker, installer, now)

    private fun available() = UpdateStatus.Available(
        manifest = UpdateManifest(versionCode = 24, versionName = "0.16.0", apkAsset = "coindex-24.apk"),
        apkUrl = APK_URL,
        apkSize = 29L,
    )

    private fun releasesAsked() = asked.count { it.endsWith("releases/latest") }

    @Test
    fun `the first look always asks, and one a minute later does not`() = runTest {
        var now = 1_000_000L
        val flow = flow { now }

        val first = flow.check(force = true)
        now += 60_000
        val second = flow.check()

        assertIs<UpdateStatus.Available>(first)
        // Null and not «up to date»: nobody asked, so the banner stays exactly as it was.
        assertNull(second)
        assertEquals(1, releasesAsked())
    }

    @Test
    fun `once the interval has gone by it asks again`() = runTest {
        var now = 1_000_000L
        val flow = flow { now }

        flow.check(force = true)
        now += UPDATE_CHECK_INTERVAL_MILLIS + 1
        val again = flow.check()

        assertIs<UpdateStatus.Available>(again)
        assertEquals(2, releasesAsked())
    }

    @Test
    fun `a forced look asks whatever the clock says`() = runTest {
        val flow = flow { 1_000_000L }

        flow.check(force = true)
        flow.check(force = true)

        assertEquals(2, releasesAsked())
    }

    @Test
    fun `without the install permission it opens the screen and nothing is downloaded`() = runTest {
        installer.permitted = false
        var downloadStarted = false

        val outcome = flow { 0L }.install(available()) { downloadStarted = true }

        assertEquals(InstallOutcome.PermissionAsked, outcome)
        assertEquals(1, installer.permissionRequests)
        assertTrue(installer.downloads.isEmpty())
        // The button is never disabled for a branch that ends before anything is fetched.
        assertFalse(downloadStarted)
    }

    @Test
    fun `a device with nowhere to grant it says so instead of asking twice`() = runTest {
        installer.permitted = false
        installer.permissionScreenOpens = false

        val outcome = flow { 0L }.install(available())

        assertEquals(InstallOutcome.PermissionUnavailable, outcome)
    }

    @Test
    fun `the apk of the release on screen is the one downloaded and handed over`() = runTest {
        var downloadStarted = false

        val outcome = flow { 0L }.install(available()) { downloadStarted = true }

        assertEquals(InstallOutcome.Handed, outcome)
        assertEquals(listOf(APK_URL to 24), installer.downloads)
        assertEquals("coindex-24.apk", installer.handed?.name)
        assertTrue(downloadStarted)
    }

    @Test
    fun `a phone with no package installer is a refusal and not a crash`() = runTest {
        installer.installs = false

        assertEquals(InstallOutcome.NoInstaller, flow { 0L }.install(available()))
    }

    @Test
    fun `a download that dies comes back with its own reason`() = runTest {
        installer.downloadError = IllegalStateException("HTTP 503 al descargar el APK")

        val outcome = flow { 0L }.install(available())

        assertEquals(
            "HTTP 503 al descargar el APK",
            assertIs<InstallOutcome.Failed>(outcome).error.message,
        )
    }
}
