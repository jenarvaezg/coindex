package com.jenarvaezg.coindex.ui.print

/**
 * The folio of a single lámina or hoja, cut down to what it actually draws (#431).
 *
 * A page shared into a chat is not a page anybody is going to file in a folder, so the bottom third
 * an A4 leaves under twelve casillas is nothing but blank the collector has to look past. What the
 * PNG keeps is the **width** of the notebook — the same rejilla, the same columns, the same coin at
 * 1:1 — and gives up only the height it was not using.
 *
 * **It is measured and never felt for.** Shortening the folio until the cells stop fitting reads a
 * rejilla that lies: [printGrid] floors its rows at one through `fitCount`, so a folio too short for
 * a single coin still reports a row, [printPages] places the casilla on it, and the drawing leaves
 * that casilla off the bottom edge. The plate of one casilla is where it shows — fifteen of the
 * father's are — and it came out as a masthead over an empty strip.
 *
 * [PrintGrid.blockHeightMm] is the answer instead, and its own KDoc says why it is the only one: the
 * packer subtracts it from the folio before anything is drawn and the block is drawn to it, so a
 * second spelling of that sum would be two page counts. The folio is that height plus the two things
 * around it that are not the rejilla — the margins and the foot strip with its ruler.
 *
 * **Only ever shorter.** A section that needs more than an A4 is one the panel has already measured
 * into two pages and sent down the PDF path; if one ever reaches here, it keeps the sheet of paper
 * it was counted on rather than growing a folio nobody measured.
 */
fun PrintGeometry.trimmedToContent(section: PrintSection): PrintGeometry {
    val needed = section.grid(this).blockHeightMm(section.cells.size)
    val folio = marginMm * 2 + footMm + needed
    return if (folio >= heightMm) this else copy(heightMm = folio)
}

/**
 * The same page, on the folio it needs (#431).
 *
 * Repacked and not resized: the cells are laid out again against the shorter folio, so what comes
 * back is the page the notebook would have printed had the paper been that size. Null where the trim
 * would not leave the whole thing on one page — which cannot happen for a section the panel measured
 * into one, and is the honest answer rather than half a lámina if it ever does.
 */
fun PrintPage.trimmedToContent(): PrintPage? {
    val section = blocks.singleOrNull()?.section ?: return null
    return printPages(listOf(section), geometry.trimmedToContent(section))
        .singleOrNull()
        ?.takeIf { trimmed -> trimmed.blocks.sumOf { it.cells.size } == section.cells.size }
}
