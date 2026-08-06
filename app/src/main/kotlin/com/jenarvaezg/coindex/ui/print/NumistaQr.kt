package com.jenarvaezg.coindex.ui.print

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * The error correction the notebook's codes are encoded with, and the reason they are version 2.
 *
 * A QR of a URL is a **byte-mode** symbol —the host is lowercase, so the compact alphanumeric mode is
 * out— and a version 2 holds 32 bytes at this level against 26 at the next one up. Numista's short
 * URL is `https://es.numista.com/` and a type id, which is 25 to 29 characters across the whole
 * seeded cache: at level M every one of them would spill into a version 3, and the ticket's arithmetic
 * (25 × 25 modules) with it.
 *
 * And version 3 would be the worse trade in the place it lands. The caption is a constant of the
 * layout, so the code's side in millimetres is fixed for the whole notebook: 29 modules in the same
 * square are **smaller** modules under the same phone camera. Seven per cent of recovery on a
 * 0,303 mm module beats fifteen on a 0,270 mm one when what is being read is a fresh laser print.
 */
private val QR_CORRECTION = ErrorCorrectionLevel.L

/**
 * The blank frame a reader needs around the code, in modules, as the standard fixes it at four.
 *
 * It is part of the code and not padding around it: a QR printed flush against a caption is a QR
 * that does not scan. It is counted into [qrModulesWithQuietZone] so that the millimetres reserved
 * on the page are the millimetres the symbol actually needs.
 */
const val QR_QUIET_MODULES = 4

/**
 * The Numista page of one coin, as a code to point a phone at (#234).
 *
 * Returns the symbol **without** its quiet zone — the modules and nothing else — because that is what
 * a decoder is given and what the drawing centres inside the frame it reserves. Asking for a one-pixel
 * output with no margin is how [QRCodeWriter] is told «at its natural size»: it never scales below one
 * pixel per module, so what comes back is 25 × 25 for every URL of the seeded cache.
 *
 * The URL comes from the cached ficha and is not built here (`TypeMeta.numistaUrl`). A member no
 * Numista type backs at all, or a ficha with no URL in it, gets no code: an empty square under the
 * caption is honest, and a code that leads nowhere is not.
 *
 * Nothing about this goes through Coil. The modules are drawn as rectangles on the page's canvas, so
 * a code cannot be a photograph that failed to arrive, cannot be a hole in the PDF, and costs nothing
 * in the warm-up of #169.
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
 * The dark modules of one row as runs of adjacent columns: `2..5` is four modules in a row.
 *
 * Drawn as runs and not one rectangle per module for the file's sake: a page of twelve codes is some
 * four thousand rectangles otherwise, every one of them a drawing command in the PDF, and a notebook
 * of a hundred-odd pages carries them all. Runs cut that by roughly half.
 *
 * It is **not** about seams. Two rectangles that share an edge still share one from row to row, and
 * whether a renderer leaves a hair of paper between them is not something this can decide — what says
 * it does not matter is the measurement: the codes of an exported notebook, rasterised at 300 dpi,
 * decode.
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
