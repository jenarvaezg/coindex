package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La geometría del papel a escala 1:1: la rejilla sale del diámetro mayor de la lámina y no del
 * número de casillas, porque una moneda impresa a tamaño real no puede encoger.
 */
class PrintGeometryTest {
    /** El papel de la configuración por omisión (#228). */
    private val paper = PrintGeometry()

    @Test
    fun `an A4 page keeps a printable band between its heading and its ruler`() {
        assertEquals(180f, paper.gridWidthMm)
        // 297 menos los dos márgenes y la regla del pie.
        assertEquals(253f, paper.contentHeightMm)
        // Menos la cabecera de la lámina, la única de un folio propio.
        assertEquals(213f, paper.gridHeightMm)
    }

    /**
     * Las filas que caben en la cola de un folio (#232). Aquí cero vale, a diferencia de
     * [PrintGrid.rows]: la lámina que no cabe abre el folio siguiente.
     */
    @Test
    fun `a plate offered the tail of a folio takes the rows that fit and no more`() {
        val ounce = grid(40.9f)

        // Con el folio entero, la misma cuenta que la rejilla.
        assertEquals(ounce.rows, ounce.rowsIn(paper.contentHeightMm, paper.heading))
        // La cabecera sale de lo que queda: 40 mm de banda y 56,9 de fila son 96,9.
        assertEquals(0, ounce.rowsIn(96f, paper.heading))
        assertEquals(1, ounce.rowsIn(97f, paper.heading))
        assertEquals(2, ounce.rowsIn(156.8f, paper.heading))
        // Una cola en la que no cabe ni la cabecera.
        assertEquals(0, ounce.rowsIn(20f, paper.heading))
        assertEquals(0, ounce.rowsIn(0f, paper.heading))
        // La banda la elige quien pregunta (#480): con la fina, 26 mm menos dan una fila más.
        assertEquals(0, ounce.rowsIn(70f, paper.continuationHeading))
        assertEquals(1, ounce.rowsIn(71f, paper.continuationHeading))
        assertEquals(2, ounce.rowsIn(131f, paper.continuationHeading))
        assertEquals(1, ounce.rowsIn(131f, paper.heading))
        assertEquals(3, ounce.rowsIn(191f, paper.continuationHeading))
        assertEquals(2, ounce.rowsIn(191f, paper.heading))
    }

    /**
     * La continuación no repite el bloque de fichas (#480): los 26 mm que ahorra (40 del masthead
     * menos 14 del nombre) son una fila de onzas. Nunca tiene menos filas que la primera página,
     * porque 14 mm es la banda más corta.
     */
    @Test
    fun `a page that continues a plate is measured against the thin band`() {
        assertEquals(PrintHeading.Slim, paper.continuationHeading)
        assertEquals(213f, paper.gridHeightMm)
        assertEquals(239f, paper.continuationGridHeightMm)

        // La onza: tres filas en su primera página y cuatro en cada una de las que la continúan.
        val ounce = grid(40.9f)
        assertEquals(3, ounce.rows)
        assertEquals(4, ounce.continuationRows)
        assertEquals(12, ounce.cellsPerPage)
        assertEquals(16, ounce.continuationCellsPerPage)

        listOf(14.5f, 16f, 22f, 33f, 38.61f, 40.9f, 45.6f).forEach { diameter ->
            val grid = grid(diameter)
            assertTrue(
                grid.continuationRows >= grid.rows,
                "$diameter mm pierde filas al continuar: ${grid.rows} → ${grid.continuationRows}",
            )
            assertTrue(
                grid.heightOfMm(grid.continuationRows) <= paper.continuationGridHeightMm,
                "$diameter mm se sale del folio que continúa",
            )
        }
    }

    /** Compartir folio no cambia: la banda fina ya está en todas las páginas (#232, #480). */
    @Test
    fun `on shared folios the thin band was already every page's`() {
        val shared = printGeometry(NotebookOptions(sharePage = true))

        assertEquals(shared.heading, shared.continuationHeading)
        assertEquals(shared.gridHeightMm, shared.continuationGridHeightMm)
        val ounce = printGrid(40.9f, shared)
        assertEquals(ounce.rows, ounce.continuationRows)
        assertEquals(ounce.cellsPerPage, ounce.continuationCellsPerPage)
    }

    /** Las calles van sólo entre filas, nunca alrededor. */
    @Test
    fun `the height of a block is its rows and the gutters between them`() {
        val ounce = grid(40.9f)

        assertEquals(0f, ounce.heightOfMm(0))
        assertEquals(56.9f, ounce.heightOfMm(1), 0.01f)
        assertEquals(56.9f * 3 + 3f * 2, ounce.heightOfMm(3), 0.01f)
        assertTrue(
            paper.headingMm + ounce.heightOfMm(ounce.rows) <= paper.contentHeightMm,
            "la rejilla de la onza se sale del folio",
        )
    }

    @Test
    fun `the grid comes from the diameter and not from the number of cells`() {
        // Las 2 rublos rusas de plata miden 33 mm.
        val roubles = grid(33f)
        assertEquals(5, roubles.columns)
        assertEquals(4, roubles.rows)
        assertEquals(20, roubles.cellsPerPage)

        // La onza australiana mide 40,9 mm.
        val ounce = grid(40.9f)
        assertEquals(4, ounce.columns)
        assertEquals(3, ounce.rows)
        assertEquals(12, ounce.cellsPerPage)

        // El Lunar II mide 45,6 mm.
        assertEquals(3, grid(45.6f).columns)
    }

    @Test
    fun `no cell is ever narrower than its own caption needs`() {
        // Los medios venezolanos miden 16 mm, pero su rótulo pide el ancho del de una onza.
        val tiny = grid(16f)
        assertEquals(paper.minCellWidthMm, tiny.cellWidthMm)
        assertTrue(tiny.columns in 5..6, "columnas para 16 mm: ${tiny.columns}")
    }

    /** A 1:1 la segunda cara se paga en ancho, no encogiendo la moneda (#230). */
    @Test
    fun `both faces make the cell two coins wide and leave its height alone`() {
        val doubled = PrintGeometry(facesPerCell = 2)
        val ounce = printGrid(40.9f, doubled)

        // 40,9 + 3 + 40,9 = 84,8 mm.
        assertEquals(84.8f, ounce.cellWidthMm, 0.01f)
        assertEquals(2, ounce.columns)
        assertEquals(3, ounce.rows)
        assertEquals(6, ounce.cellsPerPage)
        assertEquals(grid(40.9f).cellHeightMm, ounce.cellHeightMm)

        // Dos caras de 16 mm y la calle son 35 mm, que ya pasan del suelo de 28 del rótulo.
        assertEquals(35f, printGrid(16f, doubled).cellWidthMm, 0.01f)
        assertEquals(paper.minCellWidthMm, grid(16f).cellWidthMm)

        // El Lunar II cabe una sola vez por fila.
        val lunar = printGrid(45.6f, doubled)
        assertEquals(1, lunar.columns)
        assertTrue(lunar.blockWidthMm <= doubled.gridWidthMm, "94,2 mm no caben: $lunar")
    }

    @Test
    fun `a diameter nobody recorded falls back instead of printing nothing`() {
        assertEquals(grid(paper.fallbackDiameterMm), grid(null))
    }

    @Test
    fun `every cell fits inside the printable band it was measured against`() {
        listOf(14.5f, 16f, 22f, 33f, 38.61f, 40.9f, 45.6f).forEach { diameter ->
            val grid = grid(diameter)
            val height = grid.rows * grid.cellHeightMm + (grid.rows - 1) * paper.gutterMm
            assertTrue(
                grid.blockWidthMm <= paper.gridWidthMm,
                "$diameter mm se sale de ancho: ${grid.blockWidthMm}",
            )
            assertTrue(height <= paper.gridHeightMm, "$diameter mm se sale de alto: $height")
            assertTrue(grid.columns >= 1 && grid.rows >= 1, "rejilla vacía para $diameter mm")
        }
    }

    /** Con el Lunar II sobran 37 mm, y todos a un lado parecen una página impresa torcida. */
    @Test
    fun `the block is narrower than the page and what is left over is centred`() {
        val lunar = grid(45.6f)

        assertEquals(3, lunar.columns)
        assertEquals(142.8f, lunar.blockWidthMm, 0.01f)
        assertTrue(paper.gridWidthMm - lunar.blockWidthMm > 30f, "no sobraba tanto aire")
        // Una rejilla que casi llena el ancho apenas deja nada que centrar.
        val roubles = grid(33f)
        assertEquals(177f, roubles.blockWidthMm, 0.01f)
    }

    /** La primera página y luego continuaciones, no una división (#480): 37 son 12 + 16 + 9. */
    @Test
    fun `a plate that does not fit continues on the next page`() {
        val ounce = grid(40.9f)

        assertEquals(3, pageCount(37, ounce))
        assertEquals(1, pageCount(12, ounce))
        assertEquals(2, pageCount(13, ounce))
        assertEquals(2, pageCount(28, ounce))
        assertEquals(3, pageCount(29, ounce))
        // Siempre al menos una página, aunque la lámina tenga una casilla o ninguna.
        assertEquals(1, pageCount(1, ounce))
        assertEquals(1, pageCount(0, ounce))
    }

    private fun grid(diameterMm: Float?) = printGrid(diameterMm, paper)
}
