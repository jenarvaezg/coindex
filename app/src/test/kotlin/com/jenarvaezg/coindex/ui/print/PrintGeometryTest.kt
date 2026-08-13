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
        // 297 menos los dos márgenes y la regla del pie: el folio entero, que es de las láminas.
        assertEquals(253f, paper.contentHeightMm)
        // Y de ahí la cabecera de la lámina, que en un folio suyo es la única que sale.
        assertEquals(213f, paper.gridHeightMm)
    }

    /**
     * Cuántas filas caben en lo que queda de un folio, que es la pregunta del empaquetador (#232).
     *
     * Cero es una respuesta de verdad aquí y no lo es en [PrintGrid.rows]: una lámina a la que se le
     * da un folio entero se lleva una fila como mínimo, porque la alternativa es una lámina de cero
     * páginas; una lámina a la que se le ofrece la cola de un folio ajeno puede sencillamente no
     * caber, y entonces abre el siguiente.
     */
    @Test
    fun `a plate offered the tail of a folio takes the rows that fit and no more`() {
        val ounce = grid(40.9f)

        // El folio entero da las mismas tres filas que la rejilla, que es lo que la hace la misma
        // aritmética: una lámina sola cortada por el empaquetador se corta como siempre se cortó.
        assertEquals(ounce.rows, ounce.rowsIn(paper.contentHeightMm, paper.heading))
        // La cabecera sale de lo que queda, así que 40 mm de banda y 56,9 de fila son 96,9.
        assertEquals(0, ounce.rowsIn(96f, paper.heading))
        assertEquals(1, ounce.rowsIn(97f, paper.heading))
        assertEquals(2, ounce.rowsIn(156.8f, paper.heading))
        // Y una cola en la que no cabe ni la cabecera no vale para nada.
        assertEquals(0, ounce.rowsIn(20f, paper.heading))
        assertEquals(0, ounce.rowsIn(0f, paper.heading))
        // Qué banda se resta lo dice quien pregunta y no el papel (#480): con la fina, veintiséis
        // milímetros menos que restar son una fila más en el mismo hueco.
        assertEquals(0, ounce.rowsIn(70f, paper.continuationHeading))
        assertEquals(1, ounce.rowsIn(71f, paper.continuationHeading))
        assertEquals(2, ounce.rowsIn(131f, paper.continuationHeading))
        assertEquals(1, ounce.rowsIn(131f, paper.heading))
        assertEquals(3, ounce.rowsIn(191f, paper.continuationHeading))
        assertEquals(2, ounce.rowsIn(191f, paper.heading))
    }

    /**
     * La página que continúa una lámina se lleva la fila que la especificación repetida costaba (#480).
     *
     * Es la banda fina del #232 puesta a un segundo trabajo: en la página 2 el bloque de fichas no dice
     * nada que la página 1 no acabe de decir, y son veintiséis milímetros —los 40 del masthead menos los
     * 14 del nombre— que en una lámina de onzas valen exactamente una fila de cuatro monedas.
     *
     * La rejilla lleva las dos cuentas porque las dos existen en el mismo cuaderno, y la primera nunca
     * es mayor que la segunda: catorce milímetros es la más corta de las tres bandas.
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

        // Y ninguna lámina puede perder filas al continuar, sea cual sea su moneda.
        listOf(14.5f, 16f, 22f, 33f, 38.61f, 40.9f, 45.6f).forEach { diameter ->
            val grid = grid(diameter)
            assertTrue(
                grid.continuationRows >= grid.rows,
                "$diameter mm pierde filas al continuar: ${grid.rows} → ${grid.continuationRows}",
            )
            // Y la rejilla de continuación cabe en el folio que se midió contra ella.
            assertTrue(
                grid.heightOfMm(grid.continuationRows) <= paper.continuationGridHeightMm,
                "$diameter mm se sale del folio que continúa",
            )
        }
    }

    /**
     * Compartir folio no se mueve ni un milímetro (#232, #480).
     *
     * La banda que una continuación se lleva **es** la que ese interruptor ya imprime en todas las
     * páginas, así que allí no hay nada que ahorrar y no hay nada que cambiar: las dos cuentas de la
     * rejilla son la misma cuenta, y el cuaderno compartido sale cortado como el #232 lo dejó.
     */
    @Test
    fun `on shared folios the thin band was already every page's`() {
        val shared = printGeometry(NotebookOptions(sharePage = true))

        assertEquals(shared.heading, shared.continuationHeading)
        assertEquals(shared.gridHeightMm, shared.continuationGridHeightMm)
        val ounce = printGrid(40.9f, shared)
        assertEquals(ounce.rows, ounce.continuationRows)
        assertEquals(ounce.cellsPerPage, ounce.continuationCellsPerPage)
    }

    /** Y la altura de un bloque es la de sus filas y las calles entre ellas, nunca alrededor. */
    @Test
    fun `the height of a block is its rows and the gutters between them`() {
        val ounce = grid(40.9f)

        assertEquals(0f, ounce.heightOfMm(0))
        assertEquals(56.9f, ounce.heightOfMm(1), 0.01f)
        assertEquals(56.9f * 3 + 3f * 2, ounce.heightOfMm(3), 0.01f)
        // Las tres filas de la rejilla y su cabecera caben en el folio que se midió contra ellas.
        assertTrue(
            paper.headingMm + ounce.heightOfMm(ounce.rows) <= paper.contentHeightMm,
            "la rejilla de la onza se sale del folio",
        )
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

    /**
     * Y lo que cuesta es la primera página y después las continuaciones, no una división (#480).
     *
     * El Kookaburra son 37 emisiones: doce bajo el masthead y las veinticinco restantes a dieciséis por
     * folio, que son tres páginas donde antes eran cuatro.
     */
    @Test
    fun `a plate that does not fit continues on the next page`() {
        val ounce = grid(40.9f)

        assertEquals(3, pageCount(37, ounce))
        assertEquals(1, pageCount(12, ounce))
        assertEquals(2, pageCount(13, ounce))
        assertEquals(2, pageCount(28, ounce))
        assertEquals(3, pageCount(29, ounce))
        // Una lámina de una sola casilla sigue siendo una página, no cero.
        assertEquals(1, pageCount(1, ounce))
        assertEquals(1, pageCount(0, ounce))
    }

    /** La rejilla se mide contra un papel, y lo dice: es lo que el #228 hace enhebrable. */
    private fun grid(diameterMm: Float?) = printGrid(diameterMm, paper)
}
