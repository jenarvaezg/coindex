package com.jenarvaezg.coindex.ui

/**
 * Joins a figure to its unit so a wrapped curated name never ends a line on «25» and starts the
 * next with a lone «g», which reads as a typo (#511).
 */
private const val NBSP = '\u00A0'

/**
 * A figure and its unit of measure.
 *
 * Only the unit, not the whole «· datum» segment: a curated name's last segment can be longer than
 * any line, and a weld the text box can't honour overflows or shrinks the type. Denominations such
 * as «5 euros» still break like prose. Fractions are welded too: a lone «½» is the same defect.
 *
 * Applied when drawing, never to the curated files: the shelf search folds the raw name, so a
 * needle typed with an ordinary space must keep finding «25 g».
 */
private val FIGURE_AND_UNIT = Regex("""(\d[\d.,]*|[½¼¾⅓⅔])\h+(g|kg|mg|oz|mm|cm)\b""")

/** The curated string as the sheet prints it. */
fun String.weldUnits(): String = FIGURE_AND_UNIT.replace(this) { match ->
    "${match.groupValues[1]}$NBSP${match.groupValues[2]}"
}
