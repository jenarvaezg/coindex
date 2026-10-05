package com.jenarvaezg.coindex.ui.print

/**
 * The folio of a single lámina or hoja for the PNG, cut to the height it draws (#431); width,
 * rejilla and scale stay the notebook's. The height is [PrintGrid.blockHeightMm] plus margins and
 * foot strip. Shrinking until cells stop fitting would not work: [printGrid] always reports at
 * least one row, so a too-short folio would draw its casilla off the bottom edge.
 *
 * Only ever shorter: a section taller than an A4 keeps the A4.
 */
fun PrintGeometry.trimmedToContent(section: PrintSection): PrintGeometry {
    // A trimmed folio is always the plate's first and only page, so it gets the first-page band
    // (#480).
    val needed = section.grid(this).blockHeightMm(section.cells.size, heading)
    val folio = marginMm * 2 + footMm + needed
    return if (folio >= heightMm) this else copy(heightMm = folio)
}

/**
 * The same page repacked on the folio it needs (#431). Null if the section wouldn't fit on one
 * page, rather than returning half a lámina.
 */
fun PrintPage.trimmedToContent(): PrintPage? {
    val section = blocks.singleOrNull()?.section ?: return null
    return printPages(listOf(section), geometry.trimmedToContent(section))
        .singleOrNull()
        ?.takeIf { trimmed -> trimmed.blocks.sumOf { it.cells.size } == section.cells.size }
}
