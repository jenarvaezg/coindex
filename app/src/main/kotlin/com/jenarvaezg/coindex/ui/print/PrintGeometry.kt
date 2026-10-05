package com.jenarvaezg.coindex.ui.print

import kotlin.math.floor
import kotlin.math.max

/** The QR's side on paper, quiet zone included. See [PrintGeometry.qrMm]. */
private const val QR_SIDE_MM = 12f

/** Between the last line of a caption and the code under it; any less and they read as one. */
private const val QR_GAP_MM = 2f

/**
 * One line of the photo-less list (#231): tick box, state, name, year and diameter. Sized for a
 * pencil tick rather than for the type, so the box isn't marked in the wrong row.
 */
private const val LIST_LINE_MM = 7f

/**
 * Columns on a line-list page: half a printable A4 leaves about thirty characters per name (#231).
 */
private const val LIST_COLUMNS = 2

/**
 * The foot of a page with no ruler (the list of #231, the scaled album of #233): just the source
 * line. See [PrintGeometry.footMm].
 */
private const val BARE_FOOT_MM = 5f

/**
 * The fraction of its real diameter a coin is printed at with «tamaño real» off (#233). Fixed
 * rather than offered as a slider: the switch chooses an illustrated catalog over an album, not a
 * size. At 0,6 a 45 mm coin prints at 27 mm, still big enough to read its design.
 */
private const val SCALED_COIN_FRACTION = 0.6f

/**
 * A caption on a scaled page: two millimetres taller than at 1:1, because with no ruler the
 * diameter is printed on a line of its own (appended to the year line, it was the part that got
 * ellipsized). State, a two-line title and the year end at about 13,3 mm, so a fourth line ends
 * near 16,1.
 */
private const val SCALED_CAPTION_MM = 18f

/**
 * The floor on a cell's width with «tamaño real» off (#233). At 0,6 every coin is narrower than the
 * 28 mm floor of 1:1, which would then set every column and cancel most of the saving. The longest
 * state label («SIN EMITIR» over «TENGO · ×9») takes about 17 mm and must stay on one line.
 */
private const val SCALED_CELL_MM = 18f

/**
 * The air between two plates that share a folio (#232): two gutters, so the seam between plates
 * doesn't read as a badly spaced row. See [PrintGeometry.blockGapMm].
 */
private const val BLOCK_GAP_MM = 6f

/**
 * The band at the top of a plate: what it holds and how tall it is, as one value. The page count is
 * computed before drawing, and the renderer draws what this names and clips to its height, so the
 * two can't disagree. On an exported folio the rule under the heading lands at 13,1 mm for a
 * one-line title, 18,4 with a subtitle, 21,2 for a two-line title and 26,5 for both.
 */
enum class PrintHeading(
    /** The band, from the eyebrow down to the rule under the title. */
    val millimetres: Float,
    /** How many lines the title gets before it is ellipsized. */
    val titleLines: Int,
    /** Whether the variant line under the title is printed. */
    val subtitle: Boolean,
    /** Whether the plate's specification block is printed under the rule. */
    val facts: Boolean,
) {
    /** The album masthead of #169: everything a plate says about itself. */
    Masthead(millimetres = 40f, titleLines = 2, subtitle = true, facts = true),

    /**
     * For the photo-less list (#231): eyebrow, title and subtitle, without the specification block,
     * since the list's «Tengo» / «Me falta» already shows the coverage. 28 mm leaves 1,5 mm over
     * the 26,5 mm worst case.
     */
    Plain(millimetres = 28f, titleLines = 2, subtitle = true, facts = false),

    /**
     * The name band of «compartir página» (#232) and of every page that continues a plate (#480): a
     * one-line title and nothing else, 13,1 mm plus a margin. A continued plate repeats its name
     * because on paper there is no scrolling back to see whose page it is.
     */
    Slim(millimetres = 14f, titleLines = 1, subtitle = false, facts = false),
}

/**
 * The page the notebook is printed on, in millimetres: a coin has to come out of the printer at its
 * real diameter (#169), so the layout's unit is the ruler's. Only the renderer converts to pixels.
 *
 * A value rather than constants (#228) because the switches of [NotebookOptions] change these
 * measures, and the page count is computed from them before anything is drawn. The defaults are the
 * 1:1 album of #169.
 */
data class PrintGeometry(
    /** A4 portrait; there is no landscape layout. */
    val widthMm: Float = 210f,
    val heightMm: Float = 297f,
    /** Wide enough that a domestic printer's unprintable border never eats a coin. */
    val marginMm: Float = 15f,
    /**
     * The band of a plate's heading on the first page it is printed on; pages that continue it get
     * [continuationHeading]. A field because «compartir página» (#232) needs a thinner one.
     */
    val heading: PrintHeading = PrintHeading.Masthead,
    /**
     * Whether a folio may hold more than one plate (#232). Off by default because printed pages are
     * filed by collection, and two plates on one folio break that filing.
     */
    val sharesPage: Boolean = false,
    /**
     * The strip at the foot of the page: ruler on the left, source on the right. Zero is no strip.
     * Pages with no coin at 1:1 drop the ruler ([rulerBarMm]) but keep the source line, because the
     * paper outlives the app.
     */
    val footMm: Float = 14f,
    /**
     * The ruler: a bar the collector measures to catch a viewer's «ajustar a la página». Zero is
     * none, as on pages with no coin at 1:1 (#231, #233), which print the diameter as a number
     * instead ([printsDiameterLabel]).
     */
    val rulerBarMm: Float = 50f,
    /** Between two cells, and between two rows. */
    val gutterMm: Float = 3f,
    /**
     * What a cell holds under the coin: state, title over at most two lines, year. Fixed for the
     * whole notebook, so the type is the same size on every plate and only the coin band varies.
     * With the photographs off (#231) it is the whole cell, one line; with codes on as well, at
     * least [qrMm].
     */
    val captionMm: Float = 16f,
    /**
     * The square the coin's QR gets, quiet zone included: a code flush against text doesn't scan.
     * Zero is none. Every cached URL encodes to 33 modules with its frame (see `NumistaQr`), so
     * 12 mm gives 0,364 mm per module, conservative for ordinary printers and phone cameras.
     */
    val qrMm: Float = 0f,
    /**
     * The air between the last line of a caption and a code stacked under it. Zero on a page of
     * lines (#231), where the code sits at the end of the row and [qrMm] carries its quiet zone.
     */
    val qrGapMm: Float = 0f,
    /**
     * The floor on a cell's width, for coins narrower than their caption: a 16 mm medio would set
     * its title three words wide. The coin still prints at its size; the cell is what grows. Scaled
     * pages lower it ([SCALED_CELL_MM]).
     */
    val minCellWidthMm: Float = 28f,
    /**
     * Faces of a coin per cell, side by side: 0 for the photo-less list (#231), 1, or 2 for «ambas
     * caras» (#230). Which face a single one is belongs to the plate (#227) and costs no width. At
     * 1:1 a second face is paid for in width: an ounce's cell goes from 40,9 mm to 84,8 mm. The
     * sheet disables «ambas caras» with the photographs off, so one count covers both switches.
     */
    val facesPerCell: Int = 1,
    /**
     * The fraction of its real diameter a coin is printed at; 1 is the 1:1 of #169 (#233). Every
     * coin shrinks by the same factor, so their relative sizes survive. Applied only through
     * [printedDiameterMm], which scales [fallbackDiameterMm] too. See [SCALED_COIN_FRACTION].
     */
    val coinScale: Float = 1f,
    /**
     * The diameter for a cell with no recorded size. Numista types carry one, so in practice this
     * is a member no type backs (announced or unlisted), drawn as a hole; an ounce is the least
     * surprising thing for a lone hole to be.
     */
    val fallbackDiameterMm: Float = 40f,
) {
    /** Whether this page draws coins, or is the list of #231. */
    val printsCoins: Boolean get() = facesPerCell > 0

    /**
     * Whether captions print the coin's diameter in words: on every page without a ruler (#233), so
     * that the size of a coin can always be checked one way. A member with no Numista type prints
     * no diameter rather than «0 mm» (`printedDiameterLabel`).
     */
    val printsDiameterLabel: Boolean get() = rulerBarMm <= 0f

    /**
     * The millimetres a coin of [realMm] is drawn at (#233). The only place [coinScale] is applied,
     * so the page count and the renderer agree.
     */
    fun printedDiameterMm(realMm: Float): Float = realMm * coinScale

    val headingMm: Float get() = heading.millimetres

    /**
     * The band of a page that continues a plate (#480): always the thin one, since the first page
     * has already printed the summary and a later page only needs the name. Never taller than
     * [heading], so a continued plate can only gain rows.
     */
    val continuationHeading: PrintHeading get() = PrintHeading.Slim

    /** The band for one page of a plate; both the packer and the renderer ask here (#480). */
    fun headingFor(continuation: Boolean): PrintHeading =
        if (continuation) continuationHeading else heading

    /** The air between two plates on one folio (#232); zero when folios aren't shared. */
    val blockGapMm: Float get() = if (sharesPage) BLOCK_GAP_MM else 0f

    val gridWidthMm: Float get() = widthMm - marginMm * 2

    /**
     * The folio less its margins and the foot strip. The foot is once per folio and the heading
     * once per plate (#232), so a shared folio takes one band per plate out of this.
     */
    val contentHeightMm: Float get() = heightMm - marginMm * 2 - footMm

    /** The rejilla's height when a plate has the folio to itself, under its first-page band. */
    val gridHeightMm: Float get() = contentHeightMm - headingMm

    /** The same on a page that continues a plate, under the thin band (#480). */
    val continuationGridHeightMm: Float get() = contentHeightMm - continuationHeading.millimetres

    /**
     * Width of a cell's coins: [facesPerCell] faces at [diameterMm], scaled, with a gutter between
     * faces. The faces of one coin share one caption. Zero on a page without coins (#231).
     */
    fun coinBandWidthMm(diameterMm: Float): Float =
        if (!printsCoins) {
            0f
        } else {
            printedDiameterMm(diameterMm) * facesPerCell + gutterMm * (facesPerCell - 1)
        }

    /**
     * A cell's width: its coin band or the caption's floor, whichever is wider. Here rather than on
     * [PrintGrid] because it decides the grid's column count.
     */
    fun cellWidthMm(diameterMm: Float): Float = max(coinBandWidthMm(diameterMm), minCellWidthMm)

    /**
     * A cell's height: coin band, caption, and the code when it has to be stacked under the caption
     * ([qrCostsHeight], #478). Without coins (#231) it is the caption alone.
     */
    fun cellHeightMm(diameterMm: Float): Float =
        (if (printsCoins) printedDiameterMm(diameterMm) else 0f) +
            captionMm +
            if (qrCostsHeight(diameterMm)) qrGapMm + qrMm else 0f

    /**
     * Whether a cell is wide enough to carry its code beside the caption rather than under it
     * (#478), as a cell with «ambas caras» is. The code's band is reserved on both sides so the
     * caption stays centred under its coin.
     */
    fun qrBesideCaption(diameterMm: Float): Boolean =
        printsCoins &&
            qrMm > 0f &&
            cellWidthMm(diameterMm) - 2 * (qrMm + qrGapMm) >= minCellWidthMm

    /** Whether this cell's code is stacked under the caption, adding to the cell's height. */
    fun qrCostsHeight(diameterMm: Float): Boolean =
        printsCoins && qrMm > 0f && !qrBesideCaption(diameterMm)

    companion object {
        /**
         * Pixels per millimetre a page is recorded at, one value per process. Text and vectors
         * reach the PDF sharp at any value, and photographs gain nothing from a higher one: Coil
         * decodes Numista thumbnails at 180 px and doesn't upscale (ADR 0017). Six gives sub-pixel
         * layout precision and keeps a page's recording small in memory. The PDF's size comes from
         * Skia storing each photograph losslessly, not from this.
         */
        const val PX_PER_MM = 6f
    }
}

/**
 * The geometry a set of [NotebookOptions] declares: the one place a switch becomes millimetres. The
 * switches compose in order (page shape, coin size, folio sharing, then the code), each step
 * changing what it inherits rather than replacing it, so a notebook with two switches on pays for
 * both.
 */
fun printGeometry(options: NotebookOptions): PrintGeometry {
    val paper = if (options.photographs) albumPage(options) else listPage()
    val folio = if (options.sharePage) paper.shared() else paper
    return if (options.numistaQr) folio.withNumistaCode() else folio
}

/**
 * The album page of #169: one face or both (#230), at 1:1 or scaled (#233). Scaling lives only
 * here, so a list page can't be scaled; the sheet also disables «tamaño real» with the photographs
 * off.
 */
private fun albumPage(options: NotebookOptions): PrintGeometry {
    val album = PrintGeometry(
        facesPerCell = if (options.bothFaces) 2 else 1,
    )
    return if (options.actualSize) album else album.scaled()
}

/**
 * The album page with coins at [SCALED_COIN_FRACTION] of their diameter (#233). Without the 1:1
 * promise the ruler goes too, since a ruler beside scaled coins would mislead, and every caption
 * prints the diameter as a number instead ([SCALED_CAPTION_MM]). The cell-width floor comes down
 * with the coins ([SCALED_CELL_MM]), or the smaller coins save no columns.
 */
private fun PrintGeometry.scaled(): PrintGeometry = copy(
    coinScale = SCALED_COIN_FRACTION,
    footMm = BARE_FOOT_MM,
    rulerBarMm = 0f,
    captionMm = SCALED_CAPTION_MM,
    minCellWidthMm = SCALED_CELL_MM,
)

/**
 * The photo-less notebook: one line per member, [LIST_COLUMNS] columns, no ruler, the diameter as a
 * number (#231). With no photographs to download it is the one export that can't come out with
 * pictures missing.
 */
private fun listPage(): PrintGeometry = PrintGeometry(
    facesPerCell = 0,
    heading = PrintHeading.Plain,
    footMm = BARE_FOOT_MM,
    rulerBarMm = 0f,
    captionMm = LIST_LINE_MM,
).let { paper ->
    // With no coin, the floor is the whole cell width: an exact share of the printable band, so the
    // columns fill it instead of leaving a margin.
    paper.copy(
        minCellWidthMm =
            (paper.gridWidthMm - paper.gutterMm * (LIST_COLUMNS - 1)) / LIST_COLUMNS,
    )
}

/**
 * Folios that may hold several plates (#232), with the thin [PrintHeading.Slim] band that comes
 * with the switch: two mastheads would spend 80 mm of one folio on titles. Nothing else changes.
 */
private fun PrintGeometry.shared(): PrintGeometry = copy(
    sharesPage = true,
    heading = PrintHeading.Slim,
)

/**
 * A code on every cell (#234). On a page of coins the cell decides where it goes
 * ([PrintGeometry.qrBesideCaption]): beside the caption at no cost when the cell is wide, stacked
 * under it otherwise ([PrintGeometry.cellHeightMm]). Adding it to the caption here, before the
 * diameter is known, wasted rows (#478). On a page of lines it goes at the end of the row, which
 * becomes at least as tall as the code, with no gap: the code carries its own quiet zone.
 */
private fun PrintGeometry.withNumistaCode(): PrintGeometry = copy(
    captionMm = if (printsCoins) captionMm else max(captionMm, QR_SIDE_MM),
    qrMm = QR_SIDE_MM,
    qrGapMm = if (printsCoins) QR_GAP_MM else 0f,
)

/**
 * The rejilla of one plate, fitted to its largest coin: at 1:1 the coin's size is given, so what
 * varies between plates is how many fit, and the same coin is the same size on every page. Both
 * exports use it (#431).
 */
data class PrintGrid(
    /** The page this grid was fitted to. */
    val geometry: PrintGeometry,
    /** The real diameter of the plate's largest coin, which the cells are fitted to. */
    val diameterMm: Float,
    val columns: Int,
    val rows: Int,
    /**
     * Rows on a page that continues the plate (#480): at least [rows], since its band is thinner.
     */
    val continuationRows: Int,
) {
    /**
     * What the coin is drawn at (#233). [diameterMm] stays the real one, which a scaled caption
     * prints: «33 mm» under a circle 20 mm across.
     */
    val printedDiameterMm: Float get() = geometry.printedDiameterMm(diameterMm)

    val cellWidthMm: Float get() = geometry.cellWidthMm(diameterMm)

    val cellHeightMm: Float get() = geometry.cellHeightMm(diameterMm)

    /** Cells on the plate's first page. */
    val cellsPerPage: Int get() = columns * rows

    /** Cells on each page that continues the plate (#480). */
    val continuationCellsPerPage: Int get() = columns * continuationRows

    /** Width of a full row, at most the printable width. */
    val blockWidthMm: Float get() = widthOfMm(columns)

    /** Width of [columns] cells, with gutters between them but not around. */
    fun widthOfMm(columns: Int): Float =
        columns * cellWidthMm + (columns - 1).coerceAtLeast(0) * geometry.gutterMm

    /** Height of [rows] rows, with gutters between them but not around. */
    fun heightOfMm(rows: Int): Float =
        if (rows <= 0) 0f else rows * cellHeightMm + (rows - 1) * geometry.gutterMm

    /** How many rows [cellCount] cells take; the last may be short. */
    fun rowsFor(cellCount: Int): Int = (cellCount + columns - 1) / columns

    /**
     * What a block of [cellCount] cells takes out of a folio: the [heading] band plus its rows. The
     * packer subtracts this before drawing and the block is drawn to it (#232), so this must stay
     * the only spelling of the sum. The caller passes the band because it depends on whether the
     * page continues the plate (#480).
     */
    fun blockHeightMm(cellCount: Int, heading: PrintHeading): Float =
        heading.millimetres + heightOfMm(rowsFor(cellCount))

    /**
     * Rows of this plate that fit in [availableMm] of a folio, [heading] included. Unlike [rows]
     * this can be zero: a plate offered the tail of a shared folio may not fit, and opens the next
     * (#232).
     */
    fun rowsIn(availableMm: Float, heading: PrintHeading): Int {
        val forCells = availableMm - heading.millimetres
        if (forCells < cellHeightMm) return 0
        return fitCount(forCells, cellHeightMm, geometry.gutterMm)
    }
}

/** The grid a plate of coins this big gets on [geometry]. */
fun printGrid(diameterMm: Float?, geometry: PrintGeometry): PrintGrid {
    val diameter = diameterMm
        ?.takeIf { it > 0f }
        ?: geometry.fallbackDiameterMm
    val cellHeightMm = geometry.cellHeightMm(diameter)
    return PrintGrid(
        geometry = geometry,
        diameterMm = diameter,
        columns = fitCount(geometry.gridWidthMm, geometry.cellWidthMm(diameter), geometry.gutterMm),
        rows = fitCount(geometry.gridHeightMm, cellHeightMm, geometry.gutterMm),
        continuationRows = fitCount(geometry.continuationGridHeightMm, cellHeightMm, geometry.gutterMm),
    )
}

/**
 * Pages a plate of [cellCount] cells takes: the first page, then as many continuation pages (under
 * the thinner band, #480) as the rest need. Never zero: an empty collection still prints a page
 * saying so instead of silently dropping out of the notebook.
 */
fun pageCount(cellCount: Int, grid: PrintGrid): Int {
    val onFirst = grid.cellsPerPage.coerceAtLeast(1)
    if (cellCount <= onFirst) return 1
    val perContinuation = grid.continuationCellsPerPage.coerceAtLeast(1)
    val left = cellCount - onFirst
    return 1 + (left + perContinuation - 1) / perContinuation
}

/** How many cells of [cellSizeMm] fit in [availableMm], gutters between them and not around. */
private fun fitCount(availableMm: Float, cellSizeMm: Float, gutterMm: Float): Int {
    if (cellSizeMm <= 0f) return 1
    val count = floor((availableMm + gutterMm) / (cellSizeMm + gutterMm))
    return count.toInt().coerceAtLeast(1)
}
