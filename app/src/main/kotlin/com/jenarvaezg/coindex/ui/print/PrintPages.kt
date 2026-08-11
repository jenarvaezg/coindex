package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.ui.CoinName

/**
 * One cell of a printed page: one coin, at its own diameter, and what is written under it.
 *
 * The same shape for a plate's member and for a piece of a collection with no issue list, because
 * what a page draws is a coin and a caption either way (ADR 0021 §9): the difference is which of
 * them can be [filled] false, and a sheet of pieces simply never is.
 */
data class PrintCell(
    /** Curator-authored whole label; mutually exclusive with [name]. */
    val curatedLabel: String? = null,
    /** Album-name ranges; mutually exclusive with [curatedLabel]. */
    val name: CoinName? = null,
    /** The state on a plate — «Tengo», «Me falta» — and the piece's own line on a sheet. */
    val state: String?,
    /** What is left to tell this cell apart, usually the year. Null when nothing is. */
    val footnote: String?,
    /** The real diameter in millimetres, or null when nobody recorded one for this type. */
    val diameterMm: Float?,
    /**
     * The faces this cell prints, side by side, in the order they are laid out.
     *
     * **One and it is the face the plate declared**, which is the notebook of #169 with the choice
     * #227 gave it: an album page is the side you look at, and a second picture cannot be paid for by
     * halving the diameter of a page measured with a ruler — but *which* side you look at is the
     * curator's, `printed_side`, and the reverse only where no catalog says otherwise.
     * **Two and they are the obverse and then the reverse** (#230), which is the
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
) {
    init {
        require((curatedLabel == null) != (name == null)) {
            "una celda lleva un nombre de moneda o un rótulo curado"
        }
    }

    val label: String get() = name?.text ?: requireNotNull(curatedLabel)
}

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
    /**
     * Where this plate came from, **bare**: «Numista» and not «Fuente: Numista».
     *
     * The wording belongs to `notebookSourceLabel` and not here, because since #232 a folio can hold
     * plates from two catalogs and the strip at its foot is one line: a prefix baked into each
     * section would print «Fuente: A · Fuente: B».
     */
    val source: String,
    val cells: List<PrintCell>,
    /**
     * The plate's already-counted `owned/issued` figure, or null for a page that is not a plate.
     *
     * [com.jenarvaezg.coindex.ui.PlateSubject] is the only place that measures coverage. Paper keeps
     * that answer so the completion stamp can celebrate the same ratio without counting cells again.
     */
    val ratio: String? = null,
    /**
     * Every issued member owned — the completion stamp of ADR 0026 §3, which §4 sends to the PDF
     * the same way it already travels to the PNG (#371).
     *
     * False for every page that is not a plate (pieces, box, unclaimed): there is no album to be
     * complete against. The Progress row stays in [facts] either way; [ratio] belongs to the
     * celebration inside the stamp and is not a substitute for the specification.
     */
    val complete: Boolean = false,
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

/**
 * How many pages this section takes on [geometry] **with a folio to itself**.
 *
 * «Alone» is in the name because since #232 it is a trap otherwise: a plate that starts halfway down
 * somebody else's folio can be cut into more pieces than this — the first of them is one short row
 * and the rest are whole folios — even though the notebook it belongs to comes out shorter. What the
 * notebook actually costs is `printPages(...).size` and always has been; this is the plate's own
 * length, which is what the field report ranks by.
 */
fun PrintSection.pagesAlone(geometry: PrintGeometry): Int = pageCount(cells.size, grid(geometry))

/**
 * One plate's turn on a folio: a slice of its cells, under its own heading.
 *
 * A section that does not fit continues on the next folio **with its heading repeated**, which is
 * why the heading is carried by the block and not by the section: on paper there is no scrolling
 * back to find out which collection you are looking at. Since #232 a folio can hold more than one of
 * these, and the heading is then also what tells the collector where one plate stops and the next
 * begins — so what repeats when the page turns is every heading on it, and not one.
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
    /** The millimetres this block is drawn with, which is what its configuration declared (#228). */
    val geometry: PrintGeometry get() = grid.geometry

    /**
     * The photographs this block will ask for, which is what the export waits on before capturing.
     *
     * **Faces and not cells** (#230): with both faces on, a cell asks for two, and a type whose
     * obverse never arrives has to count as one photograph missing and not as a broken plate. That
     * is the same number the closing message divides by.
     */
    val photographs: Int get() = cells.sumOf { cell -> cell.faces.count { it.hasPicture } }

    /** How many rows of its grid this block holds, which is what the packer gave it. */
    val rows: Int get() = grid.rowsFor(cells.size)

    /** The cells and the gutters between them, with no heading over them. */
    val cellsHeightMm: Float get() = grid.heightOfMm(rows)

    /** What this block takes out of a folio: its own band, and the rows under it. */
    val heightMm: Float get() = grid.blockHeightMm(cells.size)

    /**
     * Columns this block is laid out on, which is the grid's except on a plate of one short row.
     *
     * A plate that spills keeps the grid's columns on every one of its blocks, even the tail one
     * holding a single coin: the pages of one plate are read as a run, and a lone Kookaburra
     * centred on page four would not line up with the column it continues. A plate that fits in one
     * block has no run to line up with, so three coins in a four-column grid are laid out as three.
     *
     * **Unchanged by «compartir página», and it was re-read** (#232): the exception is about a
     * *plate* having no continuation, not about a folio having one plate on it. Two plates sharing a
     * folio each answer this for themselves, and each is centred on its own block — which is what
     * keeps a plate of three escudos from being dragged out of centre by the plate under it.
     */
    val columnsUsed: Int
        get() = if (pagesInSection > 1) {
            grid.columns
        } else {
            minOf(grid.columns, cells.size).coerceAtLeast(1)
        }

    /**
     * How wide the block of cells is, which is what gets centred on the folio.
     *
     * The **block** and not each row: centring row by row would move the short last row of a plate
     * that does not fill it, and an album page is read down its columns.
     */
    val blockWidthMm: Float get() = grid.widthOfMm(columnsUsed)
}

/**
 * One printed folio: the plates on it, in the order they were packed, and one strip at its foot.
 *
 * It is a **list** and not one plate since #232, and that is the whole of what the switch changes
 * about the shape of the notebook: everything that was a page's — its grid, its heading, its «2 de 4»
 * — belongs to a [PrintBlock] and is asked of it, and what is left here is the paper itself.
 */
data class PrintPage(val blocks: List<PrintBlock>) {
    init {
        // A folio with nothing on it is not a folio: an empty collection is a block with no cells,
        // which is the heading saying there is nothing in it, and never a page with no heading.
        require(blocks.isNotEmpty()) { "un folio sin ninguna lámina no es una página" }
    }

    /** The millimetres this folio is drawn with, which every block on it was fitted to. */
    val geometry: PrintGeometry get() = blocks.first().geometry

    /** Every cell on the folio, whichever plate it belongs to. */
    val cells: List<PrintCell> get() = blocks.flatMap { it.cells }

    /**
     * The photographs this folio will ask for, which is what the export waits on before capturing.
     *
     * Counted per folio and not per notebook: the notebook is drawn one page at a time, so this is
     * twelve pictures to wait for eighty-four times over and never a thousand at once.
     */
    val photographs: Int get() = blocks.sumOf { it.photographs }

    /**
     * Where the plates on this folio say they came from, in the order they are printed.
     *
     * Repeats and all: naming each catalog once is `notebookSourceLabel`'s job, so that the promise
     * the word «Fuentes» makes is kept by whoever writes the line and not by whoever gathers it.
     */
    val sources: List<String> get() = blocks.map { it.section.source }
}

/**
 * The whole notebook on [geometry], in the order the index handed its cards over.
 *
 * The geometry comes in rather than being read off a constant (#228), and it is what makes this
 * function the whole of the notebook's arithmetic: how many pages the export will produce is
 * `printPages(sections, geometry).size` and nothing else, which is what lets the export sheet recount
 * on every tap without drawing anything. **That constraint is what «compartir página» had to be built
 * inside** (#232): this is a packer now, and it packs by adding up millimetres rather than by drawing
 * a folio and seeing what came off the bottom.
 *
 * It is **one packer and not two paths**. «No compartir» is not a different algorithm — it is the
 * same one under a rule that every plate opens a folio of its own, which reproduces the `chunked` it
 * replaced cut for cut: a fresh folio always offers the whole of [PrintGrid.rows], so the slices are
 * the same slices. That is what keeps the notebook of today intact behind the switch.
 *
 * Greedy and in the index's order, and deliberately not a bin-packer: the notebook is what the index
 * is showing, in the index's own order (ADR 0021 §6), so a folio takes the next plate or the next
 * plate opens a folio. Reordering the shelf to fill paper better would print a notebook the
 * collector did not ask for.
 */
fun printPages(
    sections: List<PrintSection>,
    geometry: PrintGeometry,
): List<PrintPage> {
    val folios = mutableListOf<MutableList<Placement>>()
    var freeMm = 0f

    // What the folio already open can still take of a plate with [cellsLeft] cells to place: how
    // many of its rows fit, or null where it has to open one of its own — there is no folio yet, the
    // one open belongs to somebody else and sharing is off, or what is left of it is too little.
    //
    // Zero rows is a real answer and not «it does not fit»: a collection with nothing in it is its
    // heading and nothing else, and an emptied box (ADR 0021 §11) does not deserve a folio to itself
    // for the fourteen millimetres that say so.
    fun roomOnOpenFolio(grid: PrintGrid, cellsLeft: Int): Int? {
        if (folios.isEmpty() || !geometry.sharesPage) return null
        val freeForBlock = freeMm - geometry.blockGapMm
        val rows = grid.rowsIn(freeForBlock)
        if (rows > 0) return rows
        return if (cellsLeft == 0 && freeForBlock >= geometry.headingMm) 0 else null
    }

    sections.forEachIndexed { order, section ->
        val grid = section.grid(geometry)
        var rest = section.cells
        var placed = false
        // An empty collection still gets its block: the heading saying there is nothing in it is
        // the honest page, not a section silently dropped. Hence «not placed yet» and not «cells
        // left» as the condition to keep going.
        while (!placed || rest.isNotEmpty()) {
            val room = roomOnOpenFolio(grid, rest.size)
            if (room == null) {
                folios += mutableListOf<Placement>()
                freeMm = geometry.contentHeightMm
            }
            // A folio nobody has written on gives the plate its whole grid, even where a single row
            // of it would overflow the paper: a plate that opens a folio gets one page at least,
            // which is the floor `pageCount` has always had, and the overflow is clipped.
            val rows = room ?: grid.rows
            val slice = rest.take(rows * grid.columns)
            // The seam is only paid for by a plate landing under another one.
            val gapMm = if (room == null) 0f else geometry.blockGapMm
            freeMm -= gapMm + grid.blockHeightMm(slice.size)
            folios.last() += Placement(order, section, grid, slice)
            rest = rest.drop(slice.size)
            placed = true
        }
    }

    // «2 de 4» can only be said once the packing is done: how many pieces a plate is cut into
    // depends on how much room was left where it started, so the number and the total are read off
    // the finished notebook rather than computed per plate up front.
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
 *
 * [order] is the section's place in the index and not its title: two cards can be called the same
 * thing, and what «2 de 4» counts is the plate that was handed over and not the name it goes by.
 */
private data class Placement(
    val order: Int,
    val section: PrintSection,
    val grid: PrintGrid,
    val cells: List<PrintCell>,
)
