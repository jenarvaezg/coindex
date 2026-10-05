package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.CoinName

/**
 * One cell of a printed page: a coin at its own diameter and its caption. Plate members and the
 * pieces of a collection with no issue list share this shape (ADR 0021 §9); only a plate's cells
 * can be unfilled.
 */
data class PrintCell(
    /** Curator-authored whole label; mutually exclusive with [name]. */
    val curatedLabel: String? = null,
    /** Album-name ranges; mutually exclusive with [curatedLabel]. */
    val name: CoinName? = null,
    /** The state on a plate («Tengo», «Me falta»), or the piece's own line on a sheet. */
    val state: String?,
    /** What is left to tell this cell apart, usually the year. Null when nothing is. */
    val footnote: String?,
    /** The real diameter in millimetres, or null when nobody recorded one for this type. */
    val diameterMm: Float?,
    /**
     * The faces this cell prints, side by side, in layout order. One face is the side the plate
     * declares (`printed_side`, #227), the reverse by default; two are obverse then reverse (#230).
     * A face with no picture keeps its slot so the cells line up; the renderer decides what an
     * empty slot draws.
     */
    val faces: List<CoinPhoto>,
    /** Whether the collector owns this cell. False is a hole, and only a plate has holes. */
    val filled: Boolean,
    /**
     * The Numista page this coin's QR points at (#234). Null when the switch is off, or when
     * nothing on Numista backs this cell (an announced member, a ficha with no URL).
     */
    val numistaUrl: String? = null,
) {
    init {
        require((curatedLabel == null) != (name == null)) {
            "una celda lleva un nombre de moneda o un rótulo curado"
        }
    }

    val label: String get() = name?.text ?: requireNotNull(curatedLabel)
}

/**
 * One index card as it goes to paper: heading, cells and grid. Not an entity: the notebook is what
 * the index shows at that moment, in the index's order (ADR 0021 §6).
 */
data class PrintSection(
    /** «COINDEX · CATÁLOGO CURADO» or «COINDEX · COLECCIÓN», as the two sheets already say. */
    val eyebrow: String,
    val title: String,
    val subtitle: String?,
    val facts: List<Pair<String, String>>,
    /**
     * Where this plate came from, bare: «Numista», not «Fuente: Numista». `notebookSourceLabel`
     * words the foot line, which may name several plates' sources since #232.
     */
    val source: String,
    val cells: List<PrintCell>,
    /**
     * The plate's `owned/issued` figure as [com.jenarvaezg.coindex.ui.PlateSubject] counted it, or
     * null for a page that is not a plate. The completion stamp prints it without recounting cells.
     */
    val ratio: String? = null,
    /**
     * Every issued member owned: the completion stamp of ADR 0026 §3, which §4 sends to the PDF as
     * it does to the PNG (#371). Always false for pages that aren't plates. The Progress row stays
     * in [facts] either way; [ratio] is for the stamp.
     */
    val complete: Boolean = false,
)

/** The rejilla this section gets on [geometry], fitted to its largest coin (#169). */
fun PrintSection.grid(geometry: PrintGeometry): PrintGrid =
    printGrid(cells.mapNotNull { it.diameterMm }.maxOrNull(), geometry)

/**
 * Pages this section takes on [geometry] with folios to itself: the plate's own length, which the
 * field report ranks by. With shared folios (#232) a plate can be cut into more pieces; the
 * notebook's real page count is `printPages(...).size`.
 */
fun PrintSection.pagesAlone(geometry: PrintGeometry): Int = pageCount(cells.size, grid(geometry))

/**
 * One plate's turn on a folio: a slice of its cells under its own heading. A plate that spills
 * repeats its name on every folio, and on a shared folio (#232) each heading marks where a plate
 * starts, so the heading belongs to the block rather than the section.
 */
data class PrintBlock(
    val section: PrintSection,
    val cells: List<PrintCell>,
    /** The rejilla these cells are laid out on, and the page shape they were fitted to. */
    val grid: PrintGrid,
    /** 1-based within its section, so a spilled plate can say «2 de 4». */
    val numberInSection: Int,
    val pagesInSection: Int,
) {
    val geometry: PrintGeometry get() = grid.geometry

    /**
     * The band over this turn of the plate: the plate's own the first time, the thin one after
     * (#480). Derived from [numberInSection] so it can't disagree with the «2 de 4» beside it.
     */
    val heading: PrintHeading get() = geometry.headingFor(continuation = numberInSection > 1)

    /**
     * The photographs this block will ask for, which the export waits on before capturing. Counts
     * faces, not cells (#230), the same number the closing message divides by.
     */
    val photographs: Int get() = cells.sumOf { cell -> cell.faces.count { it.hasPicture } }

    val rows: Int get() = grid.rowsFor(cells.size)

    /** The cells and the gutters between them, without the heading. */
    val cellsHeightMm: Float get() = grid.heightOfMm(rows)

    /** What this block takes out of a folio: its band and the rows under it. */
    val heightMm: Float get() = grid.blockHeightMm(cells.size, heading)

    /**
     * Columns this block is laid out on. A plate that spills keeps the grid's columns on every
     * block, so a lone coin on its last page lines up with the column it continues; a plate that
     * fits in one block uses only as many columns as it has cells. Sharing folios (#232) doesn't
     * change this: each plate is centred on its own block.
     */
    val columnsUsed: Int
        get() = if (pagesInSection > 1) {
            grid.columns
        } else {
            minOf(grid.columns, cells.size).coerceAtLeast(1)
        }

    /**
     * The width that gets centred on the folio. The whole block is centred rather than each row, so
     * a short last row stays aligned with the columns above it.
     */
    val blockWidthMm: Float get() = grid.widthOfMm(columnsUsed)
}

/**
 * One printed folio: its plates in packing order (several since #232) and one strip at its foot.
 * Grid, heading and «2 de 4» belong to each [PrintBlock].
 */
data class PrintPage(val blocks: List<PrintBlock>) {
    init {
        // An empty collection is a block with no cells, never a page with no blocks.
        require(blocks.isNotEmpty()) { "un folio sin ninguna lámina no es una página" }
    }

    /** The millimetres this folio is drawn with, which every block on it was fitted to. */
    val geometry: PrintGeometry get() = blocks.first().geometry

    /** Every cell on the folio, whichever plate it belongs to. */
    val cells: List<PrintCell> get() = blocks.flatMap { it.cells }

    /**
     * The photographs this folio will ask for. The notebook is drawn one page at a time, so the
     * export waits for one folio's pictures at once, never the whole notebook's.
     */
    val photographs: Int get() = blocks.sumOf { it.photographs }

    /**
     * The sources of the plates on this folio, in print order and with repeats;
     * `notebookSourceLabel` names each one once.
     */
    val sources: List<String> get() = blocks.map { it.section.source }
}

/**
 * The whole notebook on [geometry], in the order the index handed its cards over. Its size is the
 * export's page count, computed by adding up millimetres without drawing anything, so the export
 * sheet can recount on every tap.
 *
 * One packer for both modes: without «compartir página» (#232) every plate simply opens a folio of
 * its own, which offers [PrintGrid.rows] under the plate's band or [PrintGrid.continuationRows]
 * under the thin one (#480). Greedy and in index order (ADR 0021 §6), not a bin-packer: reordering
 * plates to save paper would print a notebook the collector didn't ask for.
 */
fun printPages(
    sections: List<PrintSection>,
    geometry: PrintGeometry,
): List<PrintPage> {
    val folios = mutableListOf<MutableList<Placement>>()
    var freeMm = 0f

    // How many rows of a plate with [cellsLeft] cells the open folio can still take, or null when
    // the plate must open a new one: no folio yet, sharing off, or too little room left.
    //
    // Zero rows is a valid answer: an empty collection is just its heading, and an emptied box
    // (ADR 0021 §11) shouldn't take a folio to itself for that.
    fun roomOnOpenFolio(grid: PrintGrid, heading: PrintHeading, cellsLeft: Int): Int? {
        if (folios.isEmpty() || !geometry.sharesPage) return null
        val freeForBlock = freeMm - geometry.blockGapMm
        val rows = grid.rowsIn(freeForBlock, heading)
        if (rows > 0) return rows
        return if (cellsLeft == 0 && freeForBlock >= heading.millimetres) 0 else null
    }

    sections.forEachIndexed { order, section ->
        val grid = section.grid(geometry)
        var rest = section.cells
        var placed = false
        // Loop on «not placed yet» as well as «cells left», so an empty collection still gets a
        // block saying it is empty.
        while (!placed || rest.isNotEmpty()) {
            // The same question [PrintBlock.heading] asks of `numberInSection` (#480).
            val heading = geometry.headingFor(continuation = placed)
            val room = roomOnOpenFolio(grid, heading, rest.size)
            if (room == null) {
                folios += mutableListOf<Placement>()
                freeMm = geometry.contentHeightMm
            }
            // A fresh folio gives the plate its whole grid for the band it opens with, even if a
            // single row overflows the paper: every plate gets at least one page (as in
            // `pageCount`) and the overflow is clipped.
            val rows = room ?: if (placed) grid.continuationRows else grid.rows
            val slice = rest.take(rows * grid.columns)
            // Only a plate landing under another one pays for the seam.
            val gapMm = if (room == null) 0f else geometry.blockGapMm
            freeMm -= gapMm + grid.blockHeightMm(slice.size, heading)
            folios.last() += Placement(order, section, grid, slice)
            rest = rest.drop(slice.size)
            placed = true
        }
    }

    // «2 de 4» is numbered after packing: how many pieces a plate is cut into depends on the room
    // left where it started.
    val ofSection = folios.flatten().groupingBy { it.order }.eachCount()
    val numbered = mutableMapOf<Int, Int>()
    return folios.map { folio ->
        PrintPage(
            folio.map { placement ->
                val number = (numbered[placement.order] ?: 0) + 1
                numbered[placement.order] = number
                PrintBlock(
                    section = placement.section,
                    cells = placement.cells,
                    grid = placement.grid,
                    numberInSection = number,
                    pagesInSection = ofSection.getValue(placement.order),
                )
            },
        )
    }
}

/**
 * One plate's slice of cells on one folio, before the notebook knows how many slices it will be.
 * [order] is the section's place in the index rather than its title, since two cards can share a
 * name.
 */
private data class Placement(
    val order: Int,
    val section: PrintSection,
    val grid: PrintGrid,
    val cells: List<PrintCell>,
)
