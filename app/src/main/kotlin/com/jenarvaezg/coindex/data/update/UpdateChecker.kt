package com.jenarvaezg.coindex.data.update

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Public GitHub repository the releases are published to. */
const val UPDATE_REPO: String = "jenarvaezg/coindex"

/** Name of the release asset describing the release. */
const val UPDATE_MANIFEST_ASSET: String = "update.json"

/**
 * What a release says about itself.
 *
 * Kept as a separate asset rather than parsed out of the tag name: the tag is for humans and
 * would be a fragile place to encode the version code an update decision depends on.
 */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    /** File name of the APK asset in the same release. */
    val apkAsset: String,
    val notes: String? = null,
)

sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus

    data class Available(val manifest: UpdateManifest, val apkUrl: String, val apkSize: Long) :
        UpdateStatus

    /** Checking failed. Never surfaced as an error: an update check must not nag. */
    data class Unavailable(val reason: String) : UpdateStatus
}

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

/**
 * Asks GitHub whether a newer APK has been published.
 *
 * These requests go to GitHub, not to Numista, so they are deliberately outside the API
 * budget gate: they cost nothing of the collector's monthly allowance.
 */
class UpdateChecker(
    private val httpClient: HttpClient,
    private val currentVersionCode: Int,
    private val repo: String = UPDATE_REPO,
    private val apiBaseUrl: String = "https://api.github.com",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(): UpdateStatus = try {
        val release = fetchLatestRelease() ?: return UpdateStatus.Unavailable(
            "no hay ninguna versión publicada todavía",
        )
        val manifestAsset = release.assets.firstOrNull { it.name == UPDATE_MANIFEST_ASSET }
            ?: return UpdateStatus.Unavailable("la última versión no publica $UPDATE_MANIFEST_ASSET")
        val manifest = json.decodeFromString<UpdateManifest>(fetchText(manifestAsset.downloadUrl))
        if (manifest.versionCode <= currentVersionCode) return UpdateStatus.UpToDate
        val apk = release.assets.firstOrNull { it.name == manifest.apkAsset }
            ?: return UpdateStatus.Unavailable(
                "la versión ${manifest.versionName} no adjunta ${manifest.apkAsset}",
            )
        UpdateStatus.Available(manifest, apk.downloadUrl, apk.size)
    } catch (error: Exception) {
        UpdateStatus.Unavailable(error.message ?: error.toString())
    }

    private suspend fun fetchLatestRelease(): GithubRelease? {
        val response = httpClient.get("$apiBaseUrl/repos/$repo/releases/latest") {
            header("Accept", "application/vnd.github+json")
        }
        if (!response.status.isSuccess()) return null
        return json.decodeFromString<GithubRelease>(response.bodyAsText())
    }

    private suspend fun fetchText(url: String): String {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("HTTP ${response.status.value} al leer $url")
        }
        return response.bodyAsText()
    }
}
