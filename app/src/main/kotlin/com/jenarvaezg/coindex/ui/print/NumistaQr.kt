package com.jenarvaezg.coindex.ui.print

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Error correction level L keeps the codes at version 2 (25 × 25 modules). A URL is a byte-mode
 * symbol (the lowercase host rules out alphanumeric mode), and Numista's short URLs run 25 to 29
 * characters: version 2 holds 32 bytes at L but 26 at M. The code's side on paper is fixed, so a
 * version 3 would mean smaller modules; 7 % recovery on a 0,364 mm module reads better than 15 % on
 * a 0,324 mm one from a fresh print.
 */
private val QR_CORRECTION = ErrorCorrectionLevel.L

/**
 * The blank frame around the code, in modules, as the standard fixes it; a QR flush against text
 * doesn't scan. Counted into [qrModulesWithQuietZone], so the space reserved on the page is what
 * the symbol needs.
 */
const val QR_QUIET_MODULES = 4

/**
 * The Numista page of one coin, as a code to point a phone at (#234). Returns the modules without
 * the quiet zone, which the drawing reserves around them. A 1 × 1 output with no margin makes
 * [QRCodeWriter] return its natural size, one pixel per module.
 *
 * The URL is the cached ficha's (`TypeMeta.numistaUrl`); null or blank gives no code. The modules
 * are drawn as rectangles on the canvas, so codes need no download.
 */
fun numistaQr(url: String?): BitMatrix? {
    val content = url?.takeIf(String::isNotBlank) ?: return null
    return runCatching {
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            1,
            1,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to QR_CORRECTION,
                EncodeHintType.MARGIN to 0,
            ),
        )
    }.getOrNull()
}

/** The side of [this] symbol once its quiet zone is counted, which is what the page reserves. */
val BitMatrix.qrModulesWithQuietZone: Int get() = width + QR_QUIET_MODULES * 2

/**
 * The dark modules of one row as runs of adjacent columns: `2..5` is four modules in a row. Runs
 * roughly halve the PDF's drawing commands compared with one rectangle per module. Exported codes
 * rasterised at 300 dpi decode, so hairline seams between rows don't matter.
 */
fun BitMatrix.qrRuns(row: Int): List<IntRange> = buildList {
    var start = -1
    for (x in 0..width) {
        val dark = x < width && get(x, row)
        if (dark && start < 0) {
            start = x
        } else if (!dark && start >= 0) {
            add(start until x)
            start = -1
        }
    }
}
