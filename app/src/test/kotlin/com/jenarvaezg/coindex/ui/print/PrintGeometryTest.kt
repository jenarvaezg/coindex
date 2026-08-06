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
    /** El papel de hoy, que es el que la configuración por omisión declara (#228). */
    private val paper = PrintGeometry()

    @Test
    fun `an A4 page keeps a printable band between its heading and its ruler`() {
        assertEquals(180f, paper.gridWidthMm)
        // 297 menos los dos márgenes, la cabecera y la regla del pie.
        assertEquals(213f, paper.gridHeightMm)
    }

    @Test
    fun `the grid comes from the diameter and not from the number of cells`() {
        // Las 2 rublos rusas de plata miden 33 mm: cinco columnas de cuatro filas.
        val roubles = grid(33f)
        assertEquals(5, roubles.columns)
        assertEquals(4, roubles.rows)
        assertEquals(20, roubles.cellsPerPage)

        // La onza australiana mide 40,9 mm y sólo caben cuatro por tres.
        val ounce = grid(40.9f)
        assertEquals(4, ounce.columns)
        assertEquals(3, ounce.rows)
        assertEquals(12, ounce.cellsPerPage)

        // Y la moneda más grande de la colección, el Lunar II de 45,6 mm, baja a tres columnas.
        assertEquals(3, grid(45.6f).columns)
    }

    @Test
    fun `no cell is ever narrower than its own caption needs`() {
        // Los medios venezolanos miden 16 mm: la casilla no encoge con ellos, porque debajo de la
        // moneda hay un rótulo que se lee igual de lejos que el de una onza.
        val tiny = grid(16f)
        assertEquals(paper.minCellWidthMm, tiny.cellWidthMm)
        assertTrue(tiny.columns in 5..6, "columnas para 16 mm: ${tiny.columns}")
    }

    /**
     * Con «ambas caras» la casilla son dos monedas y la calle de en medio (#230).
     *
     * Es el interruptor más caro en papel y el más fácil de dibujar: a 1:1 la segunda cara no se
     * puede pagar encogiendo la moneda, así que se paga en ancho. La altura no se mueve.
     */
    @Test
    fun `both faces make the cell two coins wide and leave its height alone`() {
        val doubled = PrintGeometry(facesPerCell = 2)
        val ounce = printGrid(40.9f, doubled)

        // 40,9 + 3 + 40,9 = 84,8 mm, y sólo caben dos por fila en vez de cuatro.
        assertEquals(84.8f, ounce.cellWidthMm, 0.01f)
        assertEquals(2, ounce.columns)
        assertEquals(3, ounce.rows)
        assertEquals(6, ounce.cellsPerPage)
        assertEquals(grid(40.9f).cellHeightMm, ounce.cellHeightMm)

        // El suelo del rótulo sigue mandando donde la moneda es más estrecha que sus palabras: dos
        // medios venezolanos de 16 mm son 35 mm, que ya pasa de los 28 y deja de ser el suelo.
        assertEquals(35f, printGrid(16f, doubled).cellWidthMm, 0.01f)
        assertEquals(paper.minCellWidthMm, grid(16f).cellWidthMm)

        // Y la moneda más grande de la colección cabe una sola vez por fila, pero cabe.
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

    /**
     * What the grid leaves over is margin on both sides, because the block is centred.
     *
     * The 45,6 mm Lunar II is the case that made this visible: three coins to a row leave 37 mm,
     * and all of it against the right edge read as a page printed askew.
     */
    @Test
    fun `the block is narrower than the page and what is left over is centred`() {
        val lunar = grid(45.6f)

        assertEquals(3, lunar.columns)
        assertEquals(142.8f, lunar.blockWidthMm, 0.01f)
        assertTrue(paper.gridWidthMm - lunar.blockWidthMm > 30f, "no sobraba tanto aire")
        // A grid that nearly fills the width has almost nothing left to centre.
        val roubles = grid(33f)
        assertEquals(177f, roubles.blockWidthMm, 0.01f)
    }

    @Test
    fun `a plate that does not fit continues on the next page`() {
        val ounce = grid(40.9f)
        // El Kookaburra: 37 emisiones a doce por página.
        assertEquals(4, pageCount(37, ounce))
        assertEquals(1, pageCount(12, ounce))
        assertEquals(2, pageCount(13, ounce))
        // Una lámina de una sola casilla sigue siendo una página, no cero.
        assertEquals(1, pageCount(1, ounce))
        assertEquals(1, pageCount(0, ounce))
    }

    /** La rejilla se mide contra un papel, y lo dice: es lo que el #228 hace enhebrable. */
    private fun grid(diameterMm: Float?) = printGrid(diameterMm, paper)
}
