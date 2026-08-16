package com.jenarvaezg.coindex.ui

/**
 * The space a curated name is not allowed to break on: the one between a figure and its unit.
 *
 * «Fuertes · Venezuela · plata 25 g · 1876-1936» is one string wrapped by a text box, and where it
 * wraps is decided by the widest character that still fits. On a plate heading that came out as a
 * line ending in «25» and a line holding a single «g», which reads as a typo rather than as a
 * measurement (#511). The casillas do it too and in a narrower column: «750 escudos · 12,5 g» has
 * one row of the grid to fit in.
 */
private const val NBSP = '\u00A0'

/**
 * Figure and unit, welded so no line break can land between them.
 *
 * **Only the unit, and not every «· datum» pair.** Welding a whole segment was the other candidate
 * and it loses: `name` runs to 200 characters and its last segment can be «bullion anual desde 2013
 * (sin proof, BU de campos lisos, Oriental Border ni Coronation…)», which fits in no line of any
 * phone. A weld the box cannot honour is not a weld — the box overflows or shrinks the type — so
 * what is soldered here is the pair that is always short enough to keep together.
 *
 * **Units of measure only**, so «5 euros» and «100 pesetas» still break: a denomination is a name
 * and reads as one word of prose across a line end, while a lone «g» reads as a mistake. The
 * fractions carry their own weld because a `½` orphaned from its `oz` is the same defect written
 * smaller.
 *
 * The curated files are untouched: this is how the text is **printed**, not what it says. That
 * matters for the shelf's search box, which folds the raw name into its haystack — a needle typed
 * with an ordinary space has to keep finding «25 g».
 */
private val FIGURE_AND_UNIT = Regex("""(\d[\d.,]*|[½¼¾⅓⅔])\h+(g|kg|mg|oz|mm|cm)\b""")

/** The curated string as the sheet prints it. */
fun String.weldUnits(): String = FIGURE_AND_UNIT.replace(this) { match ->
    "${match.groupValues[1]}$NBSP${match.groupValues[2]}"
}
