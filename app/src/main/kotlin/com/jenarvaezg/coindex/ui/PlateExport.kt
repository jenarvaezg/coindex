package com.jenarvaezg.coindex.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where every export lands before the share sheet takes it: plates, sheets and the notebook. */
internal const val EXPORT_DIR = "plates"

/** Where a download is announced, and tapped open, once it has landed in Descargas (#285). */
internal const val DOWNLOAD_CHANNEL_ID = "exports"

private val EXPORT_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

/** Where a drawn sheet goes: Descargas, or the share sheet (#285). */
enum class ExportDestination {
    Download,
    Share,
}

/**
 * Records the node's drawing commands into [picture] without painting it on screen. The picture
 * is replayed into a software bitmap, avoiding a GPU layer's maximum texture size: a long catalog
 * is taller than any screen.
 */
fun Modifier.recordInto(picture: Picture): Modifier = drawWithCache {
    val width = size.width.toInt()
    val height = size.height.toInt()
    onDrawWithContent {
        if (width <= 0 || height <= 0) return@onDrawWithContent
        val recordingCanvas = picture.beginRecording(width, height)
        draw(this, layoutDirection, Canvas(recordingCanvas), size) {
            this@onDrawWithContent.drawContent()
        }
        picture.endRecording()
    }
}

/** Writes a recorded sheet to a PNG and hands it to the Android share sheet. */
suspend fun sharePlateSheet(context: Context, picture: Picture, fileName: String) {
    val file = writePlatePng(context, picture, fileName)
    handToShareSheet(context, file, "image/png", "Compartir lámina")
}

/**
 * Writes a recorded one-page sheet to Descargas as a PNG (#401, #285); more pages go to PDF.
 * Returns the landed URI for the snackbar's Abrir (#403).
 */
suspend fun downloadPlateSheet(context: Context, picture: Picture, fileName: String): DownloadedExport {
    val displayName = datedExportFileName(fileName, "png")
    val file = writePlatePng(context, picture, fileName)
    val uri = handToDownloads(context, file, "image/png", displayName)
    return DownloadedExport(uri, "image/png")
}

private suspend fun writePlatePng(context: Context, picture: Picture, fileName: String): File {
    require(picture.width > 0 && picture.height > 0) { "la lámina aún no se ha dibujado" }
    return withContext(Dispatchers.IO) {
        // Replayed onto a software canvas on purpose: Bitmap.createBitmap(Picture, …) goes
        // through a hardware bitmap and its copy back to ARGB_8888 fails on some devices.
        val bitmap = Bitmap.createBitmap(
            picture.width,
            picture.height,
            Bitmap.Config.ARGB_8888,
        )
        AndroidCanvas(bitmap).drawPicture(picture)
        val directory = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        File(directory, "$fileName.png").also { target ->
            target.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }
}

/**
 * Hands a written export to another app. The file must live in the cache directory declared in
 * `file_paths.xml`: only a [FileProvider] uri with the read flag survives the trip.
 */
internal fun handToShareSheet(context: Context, file: File, mimeType: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, title))
}

/**
 * Copies a cache file into [MediaStore.Downloads] and announces it so a tap opens it (#285).
 * With `minSdk = 29` scoped storage applies and the insert needs no permission; the entry stays
 * pending until the bytes land. Returns the content URI for the snackbar's Abrir (#403).
 */
internal fun handToDownloads(
    context: Context,
    file: File,
    mimeType: String,
    displayName: String,
): Uri {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("no se pudo crear la entrada en Descargas")
    try {
        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } ?: error("no se pudo escribir en Descargas")
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } catch (failure: Exception) {
        resolver.delete(uri, null, null)
        throw failure
    }
    announceDownload(context, uri, displayName, mimeType)
    return uri
}

/** What [handToDownloads] left behind, enough for Abrir on the snackbar (#403). */
data class DownloadedExport(
    val uri: Uri,
    val mimeType: String,
)

/**
 * A notification that opens the downloaded file (#285). Best-effort: on API 33+ without
 * [android.Manifest.permission.POST_NOTIFICATIONS] it stays silent rather than asking. The
 * snackbar still names Descargas and offers Abrir when a viewer exists (#403, #436).
 */
internal fun announceDownload(
    context: Context,
    uri: Uri,
    displayName: String,
    mimeType: String,
) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(DOWNLOAD_CHANNEL_ID) == null) {
        manager.createNotificationChannel(
            NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = DOWNLOAD_CHANNEL_EXPLANATION },
        )
    }
    // A tap is only offered when something can open the file (#436).
    val intent = viewDownloadedFileIntent(uri, mimeType)
    val open = intent.takeIf { intent.resolveActivity(context.packageManager) != null }
        ?.let { view ->
            PendingIntent.getActivity(
                context,
                displayName.hashCode(),
                view,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle(DOWNLOAD_NOTIFICATION_TITLE)
        .setContentText(downloadNotificationText(displayName))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    manager.notify(displayName.hashCode(), notification)
}

/** ACTION_VIEW for a file in Descargas — snackbar Abrir and the notification share it (#403). */
fun viewDownloadedFileIntent(uri: Uri, mimeType: String): Intent =
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

/**
 * Whether anything on the phone can handle [viewDownloadedFileIntent] (#436); if not, Abrir and
 * the notification's tap are withheld.
 */
fun canViewDownloadedFile(context: Context, uri: Uri, mimeType: String): Boolean =
    viewDownloadedFileIntent(uri, mimeType).resolveActivity(context.packageManager) != null

/**
 * Opens a file from Descargas, returning false instead of crashing when no viewer is installed
 * (#436). Only [android.content.ActivityNotFoundException] counts as «no viewer».
 */
fun openDownloadedFile(context: Context, uri: Uri, mimeType: String): Boolean =
    try {
        context.startActivity(viewDownloadedFileIntent(uri, mimeType))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

/** File-system safe name for an exported plate. */
fun plateFileName(catalogId: String): String = "coindex-$catalogId"

/** The same for a sheet of pieces, which has no curated id: its title flattened to a slug. */
fun piecesFileName(title: String): String {
    val slug = Normalizer.normalize(title, Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS, "")
        .lowercase()
        .replace(NOT_A_WORD, "-")
        .trim('-')
    return "coindex-${slug.ifEmpty { "piezas" }}"
}

/**
 * The same for «Lo que busco», which has no subject to be named after (ADR 0029 §7).
 * [datedExportFileName] adds the date, as for every export.
 */
fun wishListFileName(): String = "coindex-lo-que-busco"

/**
 * The name that lands in Descargas: the base plus the local time of the tap (#285), so exporting
 * the same lámina twice doesn't collide.
 */
fun datedExportFileName(
    baseName: String,
    extension: String,
    at: LocalDateTime = LocalDateTime.now(),
): String = "$baseName-${EXPORT_STAMP.format(at)}.$extension"

private val NON_SPACING_MARKS = Regex("\\p{Mn}+")

/** Everything that is not a letter or a digit collapses to one separator, accents already gone. */
private val NOT_A_WORD = Regex("[^a-z0-9]+")
