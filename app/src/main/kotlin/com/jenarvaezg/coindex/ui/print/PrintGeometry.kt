package com.jenarvaezg.coindex.ui.print

import kotlin.math.floor
import kotlin.math.max

/** The side the QR gets on paper, quiet zone included. See [PrintGeometry.qrMm]. */
private const val QR_SIDE_MM = 10f

/** Between the last line of a caption and the code under it. Less than this and they read as one. */
private const val QR_GAP_MM = 2f

/**
 * One member of the notebook with its photographs off: a tick box, a state, a name, a year and a
 * diameter, on one line (#231).
 *
 * Seven millimetres is the line and ten is its pitch, because the gutter that separates two columns
 * separates two rows. It is not a measure of the type —2,9 mm of serif is what a caption is set in
 * everywhere else— but of the pencil: the box is ticked at a fair, and a line a pen cannot land in
 * is a checklist that gets marked in the wrong row.
 */
private const val LIST_LINE_MM = 7f

/**
 * How many members a line-list page puts side by side.
 *
 * Two and not three, and the ticket already says why: three columns at a 6 mm line print the shelf
 * in twelve pages instead of nineteen, and what comes out is a list that is no longer read at a
 * glance. The width is what a name needs, and half a printable A4 is about thirty characters of it.
 */
private const val LIST_COLUMNS = 2

/**
 * The heading of a page with no photographs: what names the plate, and nothing that summarises it.
 *
 * The band of forty millimetres is the album's masthead, and over twenty-three lines of text it is a
 * quarter of the page spent on a title. What is kept is the eyebrow, the title over its two lines and
 * the subtitle — «reducida a la mínima que identifique la lámina» (#231), and *identify* is the word:
 * the specification block goes, because **the list is the specification**. A checklist that says
 * «Tengo» or «Me falta» on every one of its lines has already printed the coverage the band would
 * have summarised, and it is the one thing this page makes redundant.
 *
 * Twenty-eight and not less, **measured off the printed folio rather than assumed**: on the exported
 * notebook the rule under the heading lands at 13,1 mm of the band for a one-line title, at 18,4 with
 * a subtitle under it and at 21,2 for a title over two lines. The worst case is both at once —a
 * collection card with a long name— at 26,5, which leaves a millimetre and a half. A title clipped
 * half a line in is worse than a band with a millimetre spare, and it is exactly the qualification a
 * printed page cannot afford to lose.
 *
 * This is not «cabecera fina» arriving early. That one is derived from «compartir página» (#232) and
 * has to hold a plate's facts in a folio shared with another plate; this one holds none at all.
 */
private const val LIST_HEADING_MM = 28f

/** The foot of a page with no ruler: room for the source line and no more. See [PrintGeometry.footMm]. */
private const val LIST_FOOT_MM = 5f

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
    /**
     * The strip at the foot of the page. Zero is no strip at all.
     *
     * It carries the ruler on the left and the source on the right, and the two are not the same
     * decision: with the photographs off there is nothing at 1:1 to protect from a viewer's «ajustar a
     * la página», so [rulerBarMm] goes to zero — but the provenance of the page does not, because the
     * paper outlives the app. So the strip narrows to the line that says where the list came from.
     */
    val footMm: Float = 14f,
    /**
     * The ruler itself: a bar the collector can measure to catch a viewer's «fit to page». Zero is
     * none, and there is nothing to catch on a page that prints no coin (#231).
     */
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
     *
     * With the photographs off it is the **whole** cell (#231), because there is no coin above it:
     * seven millimetres of one line, and a cell that is a line is what turns a hundred pages into
     * twenty-three lines a column.
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
     * **Zero on a page of lines** (#231), and not because the code needs no air: a row already spaces
     * everything on it, and the ten millimetres of [qrMm] carry the symbol's own quiet zone. What this
     * buys on a page of coins is separation from a caption stacked directly above, which a line does
     * not have.
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
     * How many faces of a coin one cell holds side by side: none, the reverse, or the two of them.
     *
     * A count and not a length, and it belongs among the millimetres all the same: at 1:1 a second
     * face cannot be paid for by shrinking the coin, so it is paid for in **width**, and that is
     * arithmetic done before a cell is drawn. A plate of ounces goes from a 40,9 mm cell to an
     * 84,8 mm one and from twelve cells a page to six.
     *
     * It is «ambas caras» (#230), and it is where the notebook parts company with #169 on purpose:
     * an album page is the side you look at, and this is the collector documenting a piece by both.
     *
     * **Zero is «sin fotos»** (#231), and it is the same arithmetic run the other way: a cell with no
     * coin band is a line, its height is its caption alone and its width owes nothing to a diameter.
     * The two switches cannot both apply — the sheet greys «ambas caras» while the photographs are off
     * — so this stays one number and never a pair of them.
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
    /**
     * Whether this page draws coins at all, or is the list of #231.
     *
     * The two shapes of the printed notebook, named once so the four measures that follow from it —
     * the coin band, the height of a cell, where the code goes and what the renderer draws — all ask
     * the same question in the same words. It is [facesPerCell] and not a field of its own, because a
     * page that prints zero faces and a page that prints no coins are one fact, not two that could
     * ever disagree.
     */
    val printsCoins: Boolean get() = facesPerCell > 0

    val gridWidthMm: Float get() = widthMm - marginMm * 2

    val gridHeightMm: Float get() = heightMm - marginMm * 2 - headingMm - footMm

    /**
     * How wide the coins of one cell are: [facesPerCell] of them at [diameterMm], gutters between.
     *
     * The gutter that separates two cells is the one that separates the two faces of one, so a row
     * of pairs is evenly spaced and what tells a pair from its neighbour is the caption underneath —
     * one caption per coin, not one per face.
     *
     * Zero where the cell prints no coin at all (#231), and not a negative gutter: the arithmetic of
     * «una moneda y las calles entre ellas» has nothing to say about none of them.
     */
    fun coinBandWidthMm(diameterMm: Float): Float =
        if (!printsCoins) 0f else diameterMm * facesPerCell + gutterMm * (facesPerCell - 1)

    /**
     * How wide a cell of coins this big is: the band they take, or the floor a caption needs.
     *
     * Here rather than on [PrintGrid], because the grid needs it before it exists — the number of
     * columns is what this answer decides — and one place is what keeps the arithmetic of the page
     * count and the drawing of the cell measuring the same thing.
     */
    fun cellWidthMm(diameterMm: Float): Float = max(coinBandWidthMm(diameterMm), minCellWidthMm)

    /**
     * How tall a cell of coins this big is: the band they take, and the caption under it.
     *
     * Here for the same reason [cellWidthMm] is: the number of rows is what this answer decides, and
     * the drawing of the cell has to measure what the page count was computed from. With no coin band
     * (#231) a cell is its caption and nothing else — one line, whatever the coin measures.
     */
    fun cellHeightMm(diameterMm: Float): Float =
        (if (printsCoins) diameterMm else 0f) + captionMm

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
 * means, and «fotos» (#231), «ambas caras» (#230) and «QR de Numista» (#234) are the three that have
 * one.
 *
 * They compose without knowing about each other, which is what «cinco interruptores y no tres modelos
 * con nombre» buys: the code grows the caption, the second face grows the cell's width, and a
 * notebook with both on pays for both. «Fotos» is the one that changes the *shape* of the page rather
 * than a measure of it — there is a page of coins and a page of lines, and which one the code goes on
 * is what decides whether it costs height or only shares it.
 */
fun printGeometry(options: NotebookOptions): PrintGeometry {
    val paper = if (options.photographs) albumPage(options) else listPage()
    return if (options.numistaQr) paper.withNumistaCode() else paper
}

/** The notebook of #169: a coin at its real diameter, one face of it or two (#230). */
private fun albumPage(options: NotebookOptions): PrintGeometry = PrintGeometry(
    // One face or two, which is the width of the coin band and therefore of the whole cell (#230).
    facesPerCell = if (options.bothFaces) 2 else 1,
)

/**
 * The notebook with its photographs off: one line per member, two columns, no ruler (#231).
 *
 * It is the cheapest of the five switches and the one that goes furthest from the notebook of today,
 * and what it really buys is not paper: with no photograph on the page there is nothing to warm, so
 * the whole apparatus of #169 — the download up front, the four seconds a page waits, the count of
 * pictures that never arrived — has nothing to do. **This is the one export that cannot come out
 * incomplete.**
 *
 * The ruler goes with the coins: it is there to catch a viewer's «ajustar a la página», and a page
 * with nothing at 1:1 on it has nothing to protect. The diameter is still printed, as a number.
 */
private fun listPage(): PrintGeometry = PrintGeometry(
    facesPerCell = 0,
    headingMm = LIST_HEADING_MM,
    footMm = LIST_FOOT_MM,
    rulerBarMm = 0f,
    captionMm = LIST_LINE_MM,
).let { paper ->
    // The floor on a cell's width is the *whole* of its width here, since no coin is pushing it
    // wider: half the printable band, so exactly two columns fit and they fill the page rather than
    // leaving a ragged margin the centring would only spread over both sides.
    paper.copy(
        minCellWidthMm =
            (paper.gridWidthMm - paper.gutterMm * (LIST_COLUMNS - 1)) / LIST_COLUMNS,
    )
}

/**
 * The same page with a code on every cell (#234), which costs height on one shape and not the other.
 *
 * On a page of coins the code goes **under the name** and inside the cell's own width, which is the
 * whole of the decision: beside the name reads better and forces a cell of 44 mm, and that takes a
 * column away from almost every coin — the Russian 33 mm go from five to a row to four. Width is what
 * that grid is short of; height it can find, at 8 pages of the shipped plates. The caption grows by
 * the code's own band, so the words keep the sixteen millimetres they had.
 *
 * On a page of lines the code goes at the **end of the line**, where the width already runs left to
 * right and there is nothing to stack it under. So it costs no height of its own — the row is the
 * taller of the line and the code, and ten millimetres of it is what the list pays — and no air
 * either: the row spaces what is on it, and the ten millimetres carry the symbol's own quiet zone.
 */
private fun PrintGeometry.withNumistaCode(): PrintGeometry = copy(
    captionMm = if (printsCoins) captionMm + QR_GAP_MM + QR_SIDE_MM else max(captionMm, QR_SIDE_MM),
    qrMm = QR_SIDE_MM,
    qrGapMm = if (printsCoins) QR_GAP_MM else 0f,
)

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

    /** The coin band and the caption under it, or the caption alone where no coin is printed. */
    val cellHeightMm: Float get() = geometry.cellHeightMm(diameterMm)

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
        rows = fitCount(geometry.gridHeightMm, geometry.cellHeightMm(diameter), geometry.gutterMm),
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
