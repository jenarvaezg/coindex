package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CoinPhoto

/**
 * One cell of a printed page: one coin, at its own diameter, and what is written under it.
 *
 * The same shape for a plate's member and for a piece of a collection with no issue list, because
 * what a page draws is a coin and a caption either way (ADR 0021 §9): the difference is which of
 * them can be [filled] false, and a sheet of pieces simply never is.
 */
data class PrintCell(
    val label: String,
    /** The state on a plate — «Tengo», «Me falta» — and the piece's own line on a sheet. */
    val state: String?,
    /** What is left to tell this cell apart, usually the year. Null when nothing is. */
    val footnote: String?,
    /** The real diameter in millimetres, or null when nobody recorded one for this type. */
    val diameterMm: Float?,
    /**
     * The faces this cell prints, side by side, in the order they are laid out.
     *
     * **One and it is the reverse**, which is the notebook of #169: an album page is the side you
     * look at, and a second picture cannot be paid for by halving the diameter of a page measured
     * with a ruler. **Two and they are the obverse and then the reverse** (#230), which is the
     * collector documenting a piece rather than mounting an album — and the second face is paid for
     * in width, so the cell doubles and a plate of ounces prints six to a page instead of twelve.
     *
     * A face with **no picture** is still a face and keeps its slot: what a plate needs is that its
     * cells line up, and what an empty slot draws — a silhouette, a dashed mount — is the renderer's
     * business, exactly as it always was for a reverse nobody had.
     */
    val faces: List<CoinPhoto>,
    /** Whether the collector owns this cell. False is a hole, and only a plate has holes. */
    val filled: Boolean,
    /**
     * The Numista page this coin's QR points at, or null where the cell carries no code (#234).
     *
     * Null in two different situations that come out the same on paper: the switch is off, so no cell
     * in the notebook has one; or nothing backs this cell on Numista — an announced member, a ficha
     * with no URL — so this one alone goes without. Both the millimetres and the cells are read off the
     * one [NotebookOptions] the export was started under, which is what keeps them in step.
     */
    val numistaUrl: String? = null,
)

/**
 * One card of the index as it goes to paper: its heading, its cells and the grid they get.
 *
 * A section is **not** an entity. The notebook has no cover, no name and no second order — the
 * export is what the index is showing at that moment, in the index's own order (ADR 0021 §6), and a
 * section is one card's turn at the printer and nothing more.
 */
data class PrintSection(
    /** «COINDEX · CATÁLOGO CURADO» or «COINDEX · COLECCIÓN», as the two sheets already say. */
    val eyebrow: String,
    val title: String,
    val subtitle: String?,
    val facts: List<Pair<String, String>>,
    val source: String,
    val cells: List<PrintCell>,
)

/**
 * The rejilla this section gets on [geometry]: fixed by its largest coin, so no page rescales a
 * coin (#169).
 *
 * It hangs off the geometry rather than off the section, because since #228 the same section prints
 * on more than one page shape: the grid is what the *configuration* makes of these cells, and a
 * section is only the cells and their heading.
 */
fun PrintSection.grid(geometry: PrintGeometry): PrintGrid =
    printGrid(cells.mapNotNull { it.diameterMm }.maxOrNull(), geometry)

/** How many pages this section takes on [geometry]. */
fun PrintSection.pages(geometry: PrintGeometry): Int = pageCount(cells.size, grid(geometry))

/**
 * One printed page: a slice of one section's cells, under that section's heading.
 *
 * A section that does not fit continues on the next page **with its heading repeated**, which is
 * why the heading is carried by the page and not by the section: on paper there is no scrolling
 * back to find out which collection you are looking at.
 */
data class PrintPage(
    val section: PrintSection,
    val cells: List<PrintCell>,
    /** The rejilla this page's cells are laid out on, and the page shape they were fitted to. */
    val grid: PrintGrid,
    /** 1-based within its section, so a spilled plate can say «2 de 4». */
    val numberInSection: Int,
    val pagesInSection: Int,
) {
    /** The millimetres this page is drawn with, which is what its configuration declared (#228). */
    val geometry: PrintGeometry get() = grid.geometry

    /**
     * The photographs this page will ask for, which is what the export waits on before capturing.
     *
     * Counted per page and not per notebook: the notebook is drawn one page at a time, so this is
     * twelve pictures to wait for eighty-four times over and never a thousand at once.
     *
     * **Faces and not cells** (#230): with both faces on, a cell asks for two, and a type whose
     * obverse never arrives has to count as one photograph missing and not as a broken plate. That
     * is the same number the closing message divides by.
     */
    val photographs: Int get() = cells.sumOf { cell -> cell.faces.count { it.hasPicture } }

    /**
     * Columns this page is laid out on, which is the grid's except on a plate of one short row.
     *
     * A plate that spills keeps the grid's columns on every one of its pages, even the tail page
     * holding a single coin: the pages of one plate are read as a run, and a lone Kookaburra
     * centred on page four would not line up with the column it continues. A plate that fits on one
     * page has no run to line up with, so three coins in a four-column grid are laid out as three.
     */
    val columnsUsed: Int
        get() = if (pagesInSection > 1) {
            grid.columns
        } else {
            minOf(grid.columns, cells.size).coerceAtLeast(1)
        }

    /**
     * How wide the block of cells is, which is what gets centred on the page.
     *
     * The **block** and not each row: centring row by row would move the short last row of a plate
     * that does not fill it, and an album page is read down its columns.
     */
    val blockWidthMm: Float get() = grid.widthOfMm(columnsUsed)
}

/**
 * The whole notebook on [geometry], in the order the index handed its cards over.
 *
 * The geometry comes in rather than being read off a constant (#228), and it is what makes this
 * function the whole of the notebook's arithmetic: how many pages the export will produce is
 * `printPages(sections, geometry).size` and nothing else, which is what lets the export sheet recount
 * on every tap without drawing anything.
 */
fun printPages(
    sections: List<PrintSection>,
    geometry: PrintGeometry,
): List<PrintPage> = sections.flatMap { section ->
    val grid = section.grid(geometry)
    val perPage = grid.cellsPerPage.coerceAtLeast(1)
    val pages = pageCount(section.cells.size, grid)
    // `chunked` on an empty list is empty, and an empty collection still gets its page: the
    // heading saying there is nothing in it is the honest page, not a section silently dropped.
    val slices = section.cells.chunked(perPage).ifEmpty { listOf(emptyList()) }
    slices.mapIndexed { index, cells ->
        PrintPage(
            section = section,
            cells = cells,
            grid = grid,
            numberInSection = index + 1,
            pagesInSection = pages,
        )
    }
}
