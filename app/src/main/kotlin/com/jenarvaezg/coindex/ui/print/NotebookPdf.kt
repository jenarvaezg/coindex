package com.jenarvaezg.coindex.ui.print

import android.content.Context
import android.graphics.Picture
import android.graphics.pdf.PdfDocument
import com.jenarvaezg.coindex.ui.EXPORT_DIR
import com.jenarvaezg.coindex.ui.datedExportFileName
import com.jenarvaezg.coindex.ui.handToDownloads
import com.jenarvaezg.coindex.ui.handToShareSheet
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A PostScript point is a seventy-second of an inch, which is the unit a PDF page is sized in. */
private const val POINTS_PER_INCH = 72f

private const val MM_PER_INCH = 25.4f

/** How much the recorded page shrinks on its way into the PDF, uniformly on both axes. */
private const val POINTS_PER_PIXEL = POINTS_PER_INCH / (MM_PER_INCH * PrintGeometry.PX_PER_MM)

/**
 * Appends one recorded page to [document], as drawing commands and not as a bitmap.
 *
 * A [Picture] replayed onto the page's canvas reaches the PDF as vectors — text, rules and circles
 * stay sharp at any zoom, and the file is a fraction of what eighty-four full-page bitmaps would
 * weigh. The photographs are still bitmaps, at whatever resolution [PRINT_PX_PER_MM] decoded them.
 *
 * The scale is the **same on both axes** on purpose. A4 is 595,28 × 841,89 points and a page is
 * sized in whole ones (595 × 842), so fitting the recording to that rounded rectangle would stretch
 * it by six hundredths of a percent on one axis alone — invisible, and a lie at 1:1. Keeping the
 * scale uniform leaves the third of a point of difference inside the fifteen-millimetre margin,
 * where nothing is drawn.
 */
fun addNotebookPage(
    document: PdfDocument,
    picture: Picture,
    number: Int,
    geometry: PrintGeometry,
) {
    require(picture.width > 0 && picture.height > 0) { "la página aún no se ha dibujado" }
    val info = PdfDocument.PageInfo.Builder(
        millimetresToPoints(geometry.widthMm),
        millimetresToPoints(geometry.heightMm),
        number,
    ).create()
    val page = document.startPage(info)
    try {
        page.canvas.scale(POINTS_PER_PIXEL, POINTS_PER_PIXEL)
        page.canvas.drawPicture(picture)
    } finally {
        document.finishPage(page)
    }
}

/** Writes the finished notebook to the cache and hands it to the Android share sheet. */
suspend fun shareNotebookPdf(
    context: Context,
    document: PdfDocument,
    fileName: String,
    chooserTitle: String = "Compartir el cuaderno",
) {
    val file = writeNotebookPdf(context, document, fileName)
    handToShareSheet(context, file, "application/pdf", chooserTitle)
}

/**
 * Writes the finished notebook to Descargas, with the stamp that keeps a second tap from
 * overwriting the first (#285).
 */
suspend fun downloadNotebookPdf(context: Context, document: PdfDocument, fileName: String) {
    val displayName = datedExportFileName(fileName, "pdf")
    val file = writeNotebookPdf(context, document, fileName)
    handToDownloads(context, file, "application/pdf", displayName)
}

private suspend fun writeNotebookPdf(
    context: Context,
    document: PdfDocument,
    fileName: String,
): File = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
    File(directory, "$fileName.pdf").also { target ->
        target.outputStream().use { stream -> document.writeTo(stream) }
    }
}

/**
 * What the exported notebook is called.
 *
 * One name and no date: the notebook is not an entity with versions (ADR 0021 §1), it is what the
 * index was showing when the button was pressed, and a second export of the same collection
 * replacing the first in the cache is the honest outcome.
 */
fun notebookFileName(): String = "coindex-cuaderno"

private fun millimetresToPoints(millimetres: Float): Int =
    (millimetres * POINTS_PER_INCH / MM_PER_INCH).roundToInt()
