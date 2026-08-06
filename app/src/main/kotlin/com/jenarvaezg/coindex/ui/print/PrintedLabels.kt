package com.jenarvaezg.coindex.ui.print

import kotlin.math.roundToInt

/**
 * What a printed cell says, as opposed to what the app says *about* printing.
 *
 * `NotebookLabels` is the copy of the export — the button, the sheet, the progress line, the message
 * that closes it — and none of it reaches the paper. This is the other half: words the PDF carries,
 * which outlive the app and are read with a coin in the other hand.
 */

/**
 * The diameter of a coin the page is not printing at its real size, as a number (#231).
 *
 * A notebook of lines has no ruler and nothing at 1:1 to hold one against, so the measure that was
 * the whole point of #169 has to be **said** instead of shown — the collector at a fair still wants
 * to know whether the piece in the tray is the 38,6 mm one or the 40,9.
 *
 * One decimal and a comma, because that is how Numista records it and how the collector says it. A
 * whole number of millimetres drops the decimal rather than printing «40,0 mm»: the zero would read
 * as a precision the ficha never claimed. Null where nobody recorded a size — an announced member, an
 * unlisted one — because that is a blank and not a «0 mm».
 */
fun printedDiameterLabel(millimetres: Float?): String? {
    val tenths = millimetres?.takeIf { it > 0f }?.times(10f)?.roundToInt() ?: return null
    val tenth = tenths % 10
    return if (tenth == 0) "${tenths / 10} mm" else "${tenths / 10},$tenth mm"
}
