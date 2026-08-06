package com.jenarvaezg.coindex.ui.print

import kotlin.math.floor
import kotlin.math.max

/** The side the QR gets on paper, quiet zone included. See [PrintGeometry.qrMm]. */
private const val QR_SIDE_MM = 10f

/** Between the last line of a caption and the code under it. Less than this and they read as one. */
private const val QR_GAP_MM = 2f

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
     *
     * With «QR de Numista» on it holds [qrMm] more of them (#234), and that is what the switch
     * costs: the caption is a constant of the layout, so the code is the same size in every cell of
     * every plate and the tallest one is what all of them pay for.
     */
    val captionMm: Float = 16f,
    /**
     * The square the QR of the coin gets at the foot of a caption, quiet zone included. Zero is
     * none.
     *
     * **Including the quiet zone**, because a code printed flush against a caption is a code that
     * does not scan: what is reserved here is what the symbol needs, not what its dark part measures.
     * Every URL of the cache is 33 modules across with its frame (`NumistaQr` says why), so ten
     * millimetres is a module of 0,303 mm.
     *
     * **Ten and not twelve, and a printed folio is why.** A calibration sheet —the same encoder, the
     * same ink on the same paper, a 50 mm ruler to prove the print was 1:1— carried the code at six
     * sizes from 9 to 14 mm, and the phone read **every one of them, the 9 mm included**. So the
     * floor is at or below 9 and the size is no longer a question of reading but of paper, and paper
     * answers in steps: a cell's height is quantised by how many rows fit a page, and every size from
     * 7 to 10,1 mm prints the sixty curated plates in the same 112 pages. Ten is the largest module
     * on the cheapest step — going down to the 9 mm that was measured buys nothing at all and only
     * spends margin, and twelve cost thirteen pages for a legibility nobody needed.
     */
    val qrMm: Float = 0f,
    /**
     * The air between the last line of a caption and the code under it. Less and they read as one.
     *
     * A field and not a constant of the renderer because it is part of the arithmetic: the caption is
     * the sixteen millimetres of #169 **plus** this and [qrMm], so what the words may take is unchanged
     * — a state, a title over two lines and a year still fill the caption exactly.
     */
    val qrGapMm: Float = 0f,
    /**
     * The floor on a cell's width, for the coins smaller than their own caption.
     *
     * The Venezuelan medios are 16 mm across: a cell that narrow would set its title in a column
     * three words wide. The coin is still printed at 16 mm — the cell is what grows.
     */
    val minCellWidthMm: Float = 28f,
    /**
     * How many faces of a coin one cell holds side by side: the reverse, or the two of them.
     *
     * A count and not a length, and it belongs among the millimetres all the same: at 1:1 a second
     * face cannot be paid for by shrinking the coin, so it is paid for in **width**, and that is
     * arithmetic done before a cell is drawn. A plate of ounces goes from a 40,9 mm cell to an
     * 84,8 mm one and from twelve cells a page to six.
     *
     * It is «ambas caras» (#230), and it is where the notebook parts company with #169 on purpose:
     * an album page is the side you look at, and this is the collector documenting a piece by both.
     */
    val facesPerCell: Int = 1,
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

    /**
     * How wide the coins of one cell are: [facesPerCell] of them at [diameterMm], gutters between.
     *
     * The gutter that separates two cells is the one that separates the two faces of one, so a row
     * of pairs is evenly spaced and what tells a pair from its neighbour is the caption underneath —
     * one caption per coin, not one per face.
     */
    fun coinBandWidthMm(diameterMm: Float): Float =
        diameterMm * facesPerCell + gutterMm * (facesPerCell - 1)

    /**
     * How wide a cell of coins this big is: the band they take, or the floor a caption needs.
     *
     * Here rather than on [PrintGrid], because the grid needs it before it exists — the number of
     * columns is what this answer decides — and one place is what keeps the arithmetic of the page
     * count and the drawing of the cell measuring the same thing.
     */
    fun cellWidthMm(diameterMm: Float): Float = max(coinBandWidthMm(diameterMm), minCellWidthMm)

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
 * The millimetres a configuration declares — the one place a switch becomes geometry.
 *
 * Every one of the five is a change to the arithmetic and not to the brush, so this is what the page
 * count is computed from and what the page is drawn with. The switches whose ticket has not landed
 * still return the notebook of #169: this function gaining a line is what «un interruptor funciona»
 * means, and «QR de Numista» (#234) and «ambas caras» (#230) are the two that have one.
 *
 * The two are independent and compose without knowing about each other, which is what «cinco
 * interruptores y no tres modelos con nombre» buys: the code grows the caption, the second face
 * grows the cell's width, and a notebook with both on pays for both.
 */
fun printGeometry(options: NotebookOptions): PrintGeometry = PrintGeometry(
    // One face or two, which is the width of the coin band and therefore of the whole cell (#230).
    facesPerCell = if (options.bothFaces) 2 else 1,
).let { paper ->
    if (!options.numistaQr) {
        paper
    } else {
        // Under the name and inside the cell's own width, which is the whole of the decision: beside
        // the name reads better and forces a cell of 44 mm, and that takes a column away from almost
        // every coin — the Russian 33 mm go from five to a row to four. Width is what this grid is
        // short of; height it can find, at 8 pages of the 60 curated plates.
        //
        // The caption grows by the code's own band, so the words keep the sixteen millimetres they had.
        paper.copy(
            captionMm = paper.captionMm + QR_GAP_MM + QR_SIDE_MM,
            qrMm = QR_SIDE_MM,
            qrGapMm = QR_GAP_MM,
        )
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
    /** The coins of the cell and their gutters, or the caption's floor where that is wider. */
    val cellWidthMm: Float get() = geometry.cellWidthMm(diameterMm)

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
    return PrintGrid(
        geometry = geometry,
        diameterMm = diameter,
        columns = fitCount(geometry.gridWidthMm, geometry.cellWidthMm(diameter), geometry.gutterMm),
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
