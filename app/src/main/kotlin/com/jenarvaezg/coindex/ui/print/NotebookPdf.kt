package com.jenarvaezg.coindex.ui.print

import android.content.Context
import android.graphics.Picture
import android.graphics.pdf.PdfDocument
import com.jenarvaezg.coindex.ui.EXPORT_DIR
import com.jenarvaezg.coindex.ui.DownloadedExport
import com.jenarvaezg.coindex.ui.datedExportFileName
import com.jenarvaezg.coindex.ui.handToDownloads
import com.jenarvaezg.coindex.ui.handToShareSheet
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** PDF pages are sized in PostScript points, 1/72 inch. */
private const val POINTS_PER_INCH = 72f

private const val MM_PER_INCH = 25.4f

/** The uniform scale from the recorded page to the PDF page. */
private const val POINTS_PER_PIXEL = POINTS_PER_INCH / (MM_PER_INCH * PrintGeometry.PX_PER_MM)

/**
 * Appends one recorded page to [document] as drawing commands: a [Picture] replayed onto the page's
 * canvas reaches the PDF as vectors, so only the photographs are bitmaps.
 *
 * The scale is the same on both axes on purpose. A4 is 595,28 × 841,89 points but a page is sized
 * in whole points (595 × 842); fitting the recording to that would stretch one axis and break 1:1.
 * The leftover fraction of a point falls inside the margin.
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
 * overwriting the first (#285). Returns the landed URI so Abrir can open it (#403).
 */
suspend fun downloadNotebookPdf(
    context: Context,
    document: PdfDocument,
    fileName: String,
): DownloadedExport {
    val displayName = datedExportFileName(fileName, "pdf")
    val file = writeNotebookPdf(context, document, fileName)
    val uri = handToDownloads(context, file, "application/pdf", displayName)
    return DownloadedExport(uri, "application/pdf")
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
 * The exported notebook's file name. Undated, so a new export replaces the previous one in the
 * cache: the notebook has no versions (ADR 0021 §1).
 */
fun notebookFileName(): String = "coindex-cuaderno"

private fun millimetresToPoints(millimetres: Float): Int =
    (millimetres * POINTS_PER_INCH / MM_PER_INCH).roundToInt()
