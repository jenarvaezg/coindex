package com.jenarvaezg.coindex.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.core.content.FileProvider
import java.io.File
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where every export lands before the share sheet takes it: plates, sheets and the notebook. */
internal const val EXPORT_DIR = "plates"

/**
 * Records the node's drawing commands into [picture] **without painting it on screen**.
 *
 * A `Picture` is replayed into a software bitmap, which sidesteps the maximum texture size a
 * GPU-backed layer would impose — the point of the whole export being that a long catalog is
 * taller than any screen.
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
    require(picture.width > 0 && picture.height > 0) { "la lámina aún no se ha dibujado" }
    val file = withContext(Dispatchers.IO) {
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
    handToShareSheet(context, file, "image/png", "Compartir lámina")
}

/**
 * Hands a written export to another app, whatever it is.
 *
 * One place because the grant is the delicate part: the file lives in the cache directory declared
 * in `file_paths.xml`, and only a [FileProvider] uri with the read flag on it survives the trip to
 * a mail client or a chat.
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

/** File-system safe name for an exported plate. */
fun plateFileName(catalogId: String): String = "coindex-$catalogId"

/**
 * What the collector is told once the sheet has been handed to the share sheet.
 *
 * The exported plate **is** the product: it gets sent to whoever the collection is being shown
 * to, holes included. The old message called any sheet complete as long as every picture had
 * reported back, and a picture that failed reported back exactly like one that arrived — so a
 * sheet with twelve empty cells announced itself as «lámina completa» (issue #67). Counting the
 * ones that actually painted is what makes the sentence true.
 */
fun plateExportMessage(members: Int, expectedPhotos: Int, loadedPhotos: Int): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    return when (absent) {
        // «Casillas» and not «emisiones»: a plate can draw a slot the mint has not struck, and
        // the progress line right above it counts only what was.
        0 -> "Lámina completa exportada · $members casillas"
        1 -> "Lámina exportada, pero una foto no llegó a cargar"
        else -> "Lámina exportada, pero $absent fotos no llegaron a cargar"
    }
}

/**
 * The same, for a sheet of pieces, whose subject has no curated id to be named by.
 *
 * The title is prose the collector typed — accents, middle dots, double spaces — so it is
 * flattened here, which is the one place that prose meets a file system.
 */
fun piecesFileName(title: String): String {
    val slug = Normalizer.normalize(title, Normalizer.Form.NFD)
        .replace(NON_SPACING_MARKS, "")
        .lowercase()
        .replace(NOT_A_WORD, "-")
        .trim('-')
    return "coindex-${slug.ifEmpty { "piezas" }}"
}

private val NON_SPACING_MARKS = Regex("\\p{Mn}+")

/** Everything that is not a letter or a digit collapses to one separator, accents already gone. */
private val NOT_A_WORD = Regex("[^a-z0-9]+")
