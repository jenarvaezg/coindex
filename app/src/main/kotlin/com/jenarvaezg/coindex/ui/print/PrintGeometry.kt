package com.jenarvaezg.coindex.ui.print

import kotlin.math.floor
import kotlin.math.max

/**
 * The page the notebook is printed on, in millimetres.
 *
 * Millimetres and not dp, because the whole point of the printed notebook is that a coin comes out
 * of the printer at its real diameter (#169): the unit of the layout has to be the unit of the
 * ruler the collector holds against it. What converts millimetres into anything a canvas
 * understands is the renderer, once, and nothing else in here knows about pixels.
 *
 * **A value and no longer an `object` of constants** (#228). The band a heading gets, the strip the
 * ruler gets and the height of a caption are what the five switches of [NotebookOptions] move, and
 * they have to move *before* a cell is drawn: the page count is arithmetic done up front, so the
 * configuration enters the arithmetic and not the brush. The defaults are the notebook of today,
 * exactly, so a geometry nobody configured is the one #169 measured.
 */
data class PrintGeometry(
    /** A4 vertical. There is no landscape page: a plate is read the way a page is read. */
    val widthMm: Float = 210f,
    val heightMm: Float = 297f,
    /** Wide enough that a domestic printer's unprintable border never eats a coin. */
    val marginMm: Float = 15f,
    /**
     * The band the heading gets, repeated on every page of a plate that spills over.
     *
     * Fixed rather than measured, and that is the load-bearing decision of the whole layout: the
     * page count is arithmetic done before a single cell is drawn, so a heading that grew with its
     * catalog's specification would put the drawing and the arithmetic out of step. What does not
     * fit in the band is clipped by the renderer.
     *
     * It is the thin heading of «compartir página» (#232) that makes this a field rather than a
     * constant: two plates in one folio cannot each take forty millimetres of band.
     */
    val headingMm: Float = 40f,
    /** The strip at the foot of the page that carries the 50 mm ruler. Zero is no strip at all. */
    val rulerMm: Float = 14f,
    /** The ruler itself: a bar the collector can measure to catch a viewer's «fit to page». */
    val rulerBarMm: Float = 50f,
    /** Between two cells, and between two rows. */
    val gutterMm: Float = 3f,
    /**
     * What a cell holds under the coin: its state, its title over at most two lines, its year.
     *
     * It is fixed for the whole notebook and the diameter is not, which is what makes the cell
     * taller for an ounce than for a half real without the type changing size from one plate to the
     * next.
     */
    val captionMm: Float = 16f,
    /**
     * The floor on a cell's width, for the coins smaller than their own caption.
     *
     * The Venezuelan medios are 16 mm across: a cell that narrow would set its title in a column
     * three words wide. The coin is still printed at 16 mm — the cell is what grows.
     */
    val minCellWidthMm: Float = 28f,
    /**
     * The diameter for a cell nobody recorded a size for.
     *
     * `size` covers 100 % of the seeded type cache, so in practice this is for the members no
     * Numista type backs at all — an announced or unlisted one — and for those the cell is a hole,
     * which by #169 takes the diameter of the coin the collector does have. The ounce is the
     * commonest piece of this collection and the least surprising thing for a lone hole to be.
     */
    val fallbackDiameterMm: Float = 40f,
) {
    val gridWidthMm: Float get() = widthMm - marginMm * 2

    val gridHeightMm: Float get() = heightMm - marginMm * 2 - headingMm - rulerMm

    companion object {
        /**
         * Pixels per millimetre a page is recorded at.
         *
         * Not a field of the geometry, because it is not about the layout: it is the resolution the
         * page is recorded at, one per process, and no switch of the export has an opinion on it.
         *
         * The page reaches the PDF as drawing commands, so text, rules and circles come out
         * vector-sharp whatever this is; what it buys is the resolution of the **photographs**,
         * which are bitmaps decoded at the size the layout asks for.
         *
         * Six is not a resolution choice, and **measured** not to be one: whatever it is, Coil
         * decodes a Numista thumbnail at its own 180 pixels (ADR 0017) and never upscales it, so the
         * PDF ends up holding 180 × 180 images either way — verified identical at five and at six.
         * What the number really buys is sub-pixel precision in the layout, and it keeps one page's
         * recording small enough to hold in memory eighty-one times over.
         *
         * The weight of the file is therefore not here: a notebook of 623 photographs is 27 MB
         * because Skia stores each of them losslessly, and the 623 draws are only 319 distinct
         * pictures.
         */
        const val PX_PER_MM = 6f
    }
}

/**
 * The rejilla of one plate: fixed by its largest coin, never by a constant of the notebook.
 *
 * The screen's sheet squares its grid off and shrinks the density as the catalog grows
 * (`SheetLayout`), which is right for a bitmap and wrong for paper: at 1:1 the coin's size is
 * given, so what varies is how many of them fit. Two plates of different coins therefore get
 * different grids, and the same coin is the same size on every page of the notebook.
 */
data class PrintGrid(
    /** The page this grid was fitted to, which is what every measure below is taken from. */
    val geometry: PrintGeometry,
    /** The diameter the cells are measured against: the largest of the plate's own coins. */
    val diameterMm: Float,
    val columns: Int,
    val rows: Int,
) {
    val cellWidthMm: Float get() = max(diameterMm, geometry.minCellWidthMm)

    val cellHeightMm: Float get() = diameterMm + geometry.captionMm

    val cellsPerPage: Int get() = columns * rows

    /**
     * How wide a full row of this grid is, which is at most the printable width and usually less:
     * a 45,6 mm coin fits three to a row and leaves 37 mm over.
     */
    val blockWidthMm: Float get() = widthOfMm(columns)

    /** The width of [columns] cells of this grid, with the gutters between them and not around. */
    fun widthOfMm(columns: Int): Float =
        columns * cellWidthMm + (columns - 1).coerceAtLeast(0) * geometry.gutterMm
}

/** The grid a plate of coins this big gets on [geometry]. */
fun printGrid(diameterMm: Float?, geometry: PrintGeometry): PrintGrid {
    val diameter = diameterMm
        ?.takeIf { it > 0f }
        ?: geometry.fallbackDiameterMm
    val cellWidth = max(diameter, geometry.minCellWidthMm)
    return PrintGrid(
        geometry = geometry,
        diameterMm = diameter,
        columns = fitCount(geometry.gridWidthMm, cellWidth, geometry.gutterMm),
        rows = fitCount(geometry.gridHeightMm, diameter + geometry.captionMm, geometry.gutterMm),
    )
}

/**
 * How many pages a plate of [cellCount] cells takes on this grid.
 *
 * Never zero: a collection with nothing in it is still a page saying so, and a page count of zero
 * would drop the plate out of the notebook without telling anyone.
 */
fun pageCount(cellCount: Int, grid: PrintGrid): Int {
    val perPage = grid.cellsPerPage.coerceAtLeast(1)
    return ((cellCount + perPage - 1) / perPage).coerceAtLeast(1)
}

/** How many cells of [cellSizeMm] fit in [availableMm], gutters between them and not around. */
private fun fitCount(availableMm: Float, cellSizeMm: Float, gutterMm: Float): Int {
    if (cellSizeMm <= 0f) return 1
    val count = floor((availableMm + gutterMm) / (cellSizeMm + gutterMm))
    return count.toInt().coerceAtLeast(1)
}
