package com.jenarvaezg.coindex.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val UPDATE_DIR = "updates"

/**
 * Whatever can put a published APK in front of the system installer.
 *
 * An interface because every branch of installing is a *refusal* — no permission, no Settings app,
 * no installer, a download that died — and each of those is a sentence the collector reads. They are
 * the reason [UpdateFlow] exists, and none of them can be provoked on a device on purpose.
 */
interface UpdateInstaller {
    /** Whether the user has granted Coindex the special "install unknown apps" permission. */
    fun canInstall(): Boolean

    /** Opens the system screen where that permission is granted. False if nothing handles it. */
    fun requestInstallPermission(): Boolean

    suspend fun download(url: String, versionCode: Int): File

    /** Hands the APK to the system installer. False if no installer is available. */
    fun install(apk: File): Boolean
}

/**
 * Downloads a published APK and hands it to the system installer.
 *
 * Sideloaded updates are always confirmed by the user: nothing here can install silently,
 * which is the correct trade-off for an app distributed outside any store. Android also
 * refuses an update signed with a different key than the installed app, so the signature is
 * verified by the platform rather than by us.
 */
class SystemUpdateInstaller(
    private val context: Context,
    private val httpClient: HttpClient,
) : UpdateInstaller {
    override fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Opens the system screen where that permission is granted.
     *
     * Returns false when no activity handles the intent — stripped-down builds exist and a
     * missing Settings app must not take the whole app down with it.
     */
    override fun requestInstallPermission(): Boolean = startSafely(
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ),
    )

    override suspend fun download(
        url: String,
        versionCode: Int,
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
        // One file per version, replaced on retry so a partial download is never installed.
        val target = File(directory, "coindex-$versionCode.apk")
        if (target.exists()) target.delete()
        val partial = File(directory, "coindex-$versionCode.apk.part")
        httpClient.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("HTTP ${response.status.value} al descargar el APK")
            }
            val channel = response.bodyAsChannel()
            partial.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                }
            }
        }
        partial.renameTo(target)
        target
    }

    override fun install(apk: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        return startSafely(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    private fun startSafely(intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    /** Drops previously downloaded APKs; they are only needed until the install is confirmed. */
    fun clearDownloads() {
        File(context.cacheDir, UPDATE_DIR).listFiles()?.forEach { it.delete() }
    }
}
