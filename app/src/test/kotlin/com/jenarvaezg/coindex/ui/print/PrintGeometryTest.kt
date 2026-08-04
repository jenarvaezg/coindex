package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La geometría del papel, que es la única parte de la exportación que se puede medir sin un
 * teléfono delante.
 *
 * Escala 1:1: la rejilla no la elige el número de casillas —eso es lo que hace la lámina en
 * pantalla— sino el diámetro mayor de la lámina, porque una moneda impresa a su tamaño real no
 * puede encogerse para que quepan más.
 */
class PrintGeometryTest {
    @Test
    fun `an A4 page keeps a printable band between its heading and its ruler`() {
        assertEquals(180f, PrintPaper.gridWidthMm)
        // 297 menos los dos márgenes, la cabecera y la regla del pie.
        assertEquals(213f, PrintPaper.gridHeightMm)
    }

    @Test
    fun `the grid comes from the diameter and not from the number of cells`() {
        // Las 2 rublos rusas de plata miden 33 mm: cinco columnas de cuatro filas.
        val roubles = printGrid(33f)
        assertEquals(5, roubles.columns)
        assertEquals(4, roubles.rows)
        assertEquals(20, roubles.cellsPerPage)

        // La onza australiana mide 40,9 mm y sólo caben cuatro por tres.
        val ounce = printGrid(40.9f)
        assertEquals(4, ounce.columns)
        assertEquals(3, ounce.rows)
        assertEquals(12, ounce.cellsPerPage)

        // Y la moneda más grande de la colección, el Lunar II de 45,6 mm, baja a tres columnas.
        assertEquals(3, printGrid(45.6f).columns)
    }

    @Test
    fun `no cell is ever narrower than its own caption needs`() {
        // Los medios venezolanos miden 16 mm: la casilla no encoge con ellos, porque debajo de la
        // moneda hay un rótulo que se lee igual de lejos que el de una onza.
        val tiny = printGrid(16f)
        assertEquals(PrintPaper.MIN_CELL_WIDTH_MM, tiny.cellWidthMm)
        assertTrue(tiny.columns in 5..6, "columnas para 16 mm: ${tiny.columns}")
    }

    @Test
    fun `a diameter nobody recorded falls back instead of printing nothing`() {
        assertEquals(printGrid(PrintPaper.FALLBACK_DIAMETER_MM), printGrid(null))
    }

    @Test
    fun `every cell fits inside the printable band it was measured against`() {
        listOf(14.5f, 16f, 22f, 33f, 38.61f, 40.9f, 45.6f).forEach { diameter ->
            val grid = printGrid(diameter)
            val height = grid.rows * grid.cellHeightMm + (grid.rows - 1) * PrintPaper.GUTTER_MM
            assertTrue(
                grid.blockWidthMm <= PrintPaper.gridWidthMm,
                "$diameter mm se sale de ancho: ${grid.blockWidthMm}",
            )
            assertTrue(height <= PrintPaper.gridHeightMm, "$diameter mm se sale de alto: $height")
            assertTrue(grid.columns >= 1 && grid.rows >= 1, "rejilla vacía para $diameter mm")
        }
    }

    /**
     * What the grid leaves over is margin on both sides, because the block is centred.
     *
     * The 45,6 mm Lunar II is the case that made this visible: three coins to a row leave 37 mm,
     * and all of it against the right edge read as a page printed askew.
     */
    @Test
    fun `the block is narrower than the page and what is left over is centred`() {
        val lunar = printGrid(45.6f)

        assertEquals(3, lunar.columns)
        assertEquals(142.8f, lunar.blockWidthMm, 0.01f)
        assertTrue(PrintPaper.gridWidthMm - lunar.blockWidthMm > 30f, "no sobraba tanto aire")
        // A grid that nearly fills the width has almost nothing left to centre.
        val roubles = printGrid(33f)
        assertEquals(177f, roubles.blockWidthMm, 0.01f)
    }

    @Test
    fun `a plate that does not fit continues on the next page`() {
        val ounce = printGrid(40.9f)
        // El Kookaburra: 37 emisiones a doce por página.
        assertEquals(4, pageCount(37, ounce))
        assertEquals(1, pageCount(12, ounce))
        assertEquals(2, pageCount(13, ounce))
        // Una lámina de una sola casilla sigue siendo una página, no cero.
        assertEquals(1, pageCount(1, ounce))
        assertEquals(1, pageCount(0, ounce))
    }
}
