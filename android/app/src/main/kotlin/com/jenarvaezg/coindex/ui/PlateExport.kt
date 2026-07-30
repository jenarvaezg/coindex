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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val EXPORT_DIR = "plates"

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
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.plates", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, "Compartir lámina"))
}

/** File-system safe name for an exported plate. */
fun plateFileName(catalogId: String): String = "coindex-$catalogId"
