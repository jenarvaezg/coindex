package com.jenarvaezg.coindex.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import java.io.File

private const val EXPORT_DIR = "plates"

/**
 * Renders a captured plate to a PNG and hands it to the Android share sheet.
 *
 * Captures what is currently laid out on the plate: a catalog longer than the screen exports
 * the visible part, not the whole sheet (see README, "Exportar lámina").
 */
suspend fun sharePlateImage(context: Context, layer: GraphicsLayer, fileName: String) {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    val directory = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
    val file = File(directory, "$fileName.png")
    file.outputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
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
