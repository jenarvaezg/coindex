package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Los cinco interruptores por los que sale el cuaderno, y la puerta que el #228 abre.
 *
 * La configuración no es un parámetro del pincel: el número de páginas es aritmética hecha antes de
 * dibujar nada, así que lo que se comprueba aquí es que un interruptor se convierte en milímetros y
 * que los milímetros de hoy son los que nadie eligió.
 */
class NotebookOptionsTest {
    @Test
    fun `the notebook of today is what nobody chose`() {
        val untouched = NotebookOptions()

        // Fotos sí, una cara, tamaño real sí, sin compartir página, sin QR: nadie se encuentra su
        // cuaderno cambiado sin haberlo pedido.
        assertTrue(untouched.photographs)
        assertFalse(untouched.bothFaces)
        assertTrue(untouched.actualSize)
        assertFalse(untouched.sharePage)
        assertFalse(untouched.numistaQr)
    }

    /**
     * The door is open and only the switches whose ticket landed have moved anything.
     *
     * Each of the five becomes millimetres in its own ticket (#230-#234): «QR de Numista» grows the
     * caption to make room for the code (#234), «ambas caras» widens the cell to hold the second one
     * (#230), and «fotos» takes the coin band away altogether (#231). **A ticket landing moves a
     * switch from one half of this test to the other**, which is the point — until then a grey switch
     * promises nothing, and what has and has not changed behind the door is written down.
     */
    @Test
    fun `only the switches whose ticket landed move a millimetre`() {
        val everyCombination = allCombinations()

        assertEquals(32, everyCombination.size, "faltan combinaciones de cinco interruptores")
        val moved = everyCombination.filter { printGeometry(it) != PrintGeometry() }
        assertEquals(
            everyCombination.filter { it.numistaQr || it.bothFaces || !it.photographs },
            moved,
            "un interruptor sin ticket ha movido la geometría, o uno con ticket no la mueve",
        )
    }

    /**
     * The same again for the cells, which is the half a geometry check cannot see.
     *
     * The configuration reaches `notebookSections` too, because what a cell *is* depends on it —
     * both faces gives it an obverse (#230), no photographs gives it neither (#231). The two left,
     * «tamaño real» (#233) and «compartir página» (#232), may not change a cell until their own
     * ticket lands: a notebook of 113 pages of empty circles is exactly the half-landed switch #228
     * refuses to ship. The three that have landed are left out because changing a cell is precisely
     * what they do, and `NotebookPagesTest` is where that is measured.
     */
    @Test
    fun `no switch without a ticket changes a cell`() {
        val card = IndexCard.Box(
            name = "Bandeja del abuelo",
            issuer = null,
            box = OwnGroupingView(
                OwnGrouping(id = 1, name = "Bandeja del abuelo", typeIds = emptyList()),
                emptyList(),
            ),
        )
        val today = notebookSections(CollectionState(), listOf(card), Curation(emptyList()), NotebookOptions())

        val landed = { it: NotebookOptions -> it.numistaQr || it.bothFaces || !it.photographs }
        val moved = allCombinations().filterNot(landed).filter { options ->
            notebookSections(CollectionState(), listOf(card), Curation(emptyList()), options) != today
        }

        assertEquals(emptyList(), moved, "un interruptor sin ticket ha cambiado una casilla")
    }

    @Test
    fun `the default configuration declares the geometry the notebook was measured with`() {
        val paper = printGeometry(NotebookOptions())

        assertEquals(210f, paper.widthMm)
        assertEquals(297f, paper.heightMm)
        assertEquals(40f, paper.headingMm)
        assertTrue(paper.printsCoins)
        assertEquals(14f, paper.footMm)
        assertEquals(50f, paper.rulerBarMm)
        assertEquals(16f, paper.captionMm)
        // Y ningún código: sin él, el rótulo es el pie de foto entero.
        assertEquals(0f, paper.qrMm)
        assertEquals(0f, paper.qrGapMm)
        // Una cara por moneda, que es la del #169: el reverso, que es el lado que se mira.
        assertEquals(1, paper.facesPerCell)
        // 210 menos los dos márgenes; 297 menos los dos márgenes, la cabecera y la regla del pie.
        assertEquals(180f, paper.gridWidthMm)
        assertEquals(213f, paper.gridHeightMm)
    }

    /**
     * What the QR costs in millimetres, which is all it costs: only the caption moves.
     *
     * The page, the margins, the heading and the ruler are untouched, and so is the width of a cell:
     * beside the name would have forced a floor of 44 mm on every cell, and that is a column taken
     * away from almost every coin in the collection. The code goes under the name and grows the one
     * measure this grid has to spare.
     */
    @Test
    fun `the qr grows the caption and nothing else`() {
        val paper = printGeometry(NotebookOptions())
        val coded = printGeometry(NotebookOptions(numistaQr = true))

        // 16 mm de rótulo, 2 de aire y los 10 del código con su zona de silencio.
        assertEquals(28f, coded.captionMm)
        assertEquals(10f, coded.qrMm)
        assertEquals(2f, coded.qrGapMm)
        // Y las palabras siguen teniendo los 16 mm del #169: el código se **suma** al pie de foto, no
        // le quita sitio al rótulo. Un estado, un título de dos líneas y un año siguen cabiendo.
        assertEquals(paper.captionMm, coded.captionMm - coded.qrGapMm - coded.qrMm)
        assertEquals(
            paper,
            coded.copy(captionMm = paper.captionMm, qrMm = paper.qrMm, qrGapMm = paper.qrGapMm),
        )
        // Un módulo de 0,303 mm: 33 de ellos —25 de versión 2 y las dos zonas de silencio— en 10 mm.
        // El folio de calibración leyó hasta los 9 mm (módulo de 0,273), así que esto va sobrado.
        assertEquals(0.303f, coded.qrMm / 33f, 0.001f)
    }

    /**
     * What both faces cost in millimetres, which is all they cost: only the coin band moves (#230).
     *
     * The page, the margins, the heading, the ruler and the caption are untouched — the second face
     * is paid for in width, because at 1:1 the alternative is halving the diameter and that is the
     * one thing a page measured with a ruler cannot do.
     */
    @Test
    fun `both faces widen the coin band and nothing else`() {
        val paper = printGeometry(NotebookOptions())
        val doubled = printGeometry(NotebookOptions(bothFaces = true))

        assertEquals(2, doubled.facesPerCell)
        assertEquals(paper, doubled.copy(facesPerCell = paper.facesPerCell))
        // Dos onzas de 40,9 mm y la calle de 3 que las separa.
        assertEquals(40.9f, paper.coinBandWidthMm(40.9f))
        assertEquals(84.8f, doubled.coinBandWidthMm(40.9f), 0.01f)
    }

    /**
     * The two switches that have landed compose, which is what «cinco interruptores» buys.
     *
     * Neither knows about the other: the code grows the caption, the second face widens the cell,
     * and a notebook with both on pays for both. Models with a name were dropped precisely so the
     * collector can combine, and the live page count is what tells them what the combination costs.
     */
    @Test
    fun `the two that have landed compose without knowing about each other`() {
        val both = printGeometry(NotebookOptions(bothFaces = true, numistaQr = true))

        assertEquals(2, both.facesPerCell)
        assertEquals(28f, both.captionMm)
        assertEquals(10f, both.qrMm)
        assertEquals(
            printGeometry(NotebookOptions(numistaQr = true)),
            both.copy(facesPerCell = 1),
        )
    }

    /**
     * What «sin fotos» declares: a page of lines, and nothing on it measured against a ruler (#231).
     *
     * It is the only one of the five that changes the *shape* of the page rather than a measure of
     * it. No coin band, so a cell is its caption and a caption is one line of seven millimetres; no
     * diameter deciding the width, so the floor is the whole of it and exactly two columns fill the
     * printable band; no ruler, because there is nothing at 1:1 to catch a viewer's «ajustar a la
     * página» with; and a heading that names the plate without summarising it, since a list that says
     * «Tengo» or «Me falta» on every line has printed the coverage already.
     */
    @Test
    fun `with no photographs the page is lines, two to a row, and carries no ruler`() {
        val list = printGeometry(NotebookOptions(photographs = false))

        assertEquals(0, list.facesPerCell)
        assertFalse(list.printsCoins)
        assertEquals(7f, list.captionMm)
        assertEquals(0f, list.rulerBarMm)
        assertEquals(28f, list.headingMm)
        // La casilla es su línea, mida lo que mida la moneda: es lo que quita las cien páginas.
        assertEquals(7f, list.cellHeightMm(40.9f))
        assertEquals(7f, list.cellHeightMm(16f))
        assertEquals(0f, list.coinBandWidthMm(40.9f))
        // Media banda imprimible, así que caben dos exactas y la llenan.
        assertEquals(88.5f, list.cellWidthMm(40.9f), 0.01f)
        val grid = printGrid(40.9f, list)
        assertEquals(2, grid.columns)
        assertEquals(23, grid.rows)
        assertEquals(46, grid.cellsPerPage)
        assertEquals(list.gridWidthMm, grid.blockWidthMm, 0.01f)
        // Y la rejilla ya no la decide el diámetro: la lámina de medios y la de onzas son la misma,
        // y una casilla que nadie midió no necesita ningún diámetro de reserva para caber.
        val shape = { it: PrintGrid -> Triple(it.columns, it.rows, it.cellWidthMm) }
        assertEquals(shape(grid), shape(printGrid(16f, list)))
        assertEquals(shape(grid), shape(printGrid(null, list)))
    }

    /**
     * The code closes the line instead of sitting under it, so it costs the row three millimetres.
     *
     * On a page of coins the caption grows by the code's whole band, because the cell is read top to
     * bottom and there is a name to stack it under. A line is read left to right and has a right edge
     * going spare, so what the code costs is only the height it does not already have: seven
     * millimetres of line become the ten of the square.
     *
     * And no air of its own: [PrintGeometry.qrGapMm] is what separates a code from a caption stacked
     * over it, which is a thing a line does not have — the row already spaces what is on it, and the
     * ten millimetres of the square carry the symbol's own quiet zone. A gap the renderer never spends
     * would be a millimetre in the arithmetic that is nowhere on the paper.
     */
    @Test
    fun `on a page of lines the code costs the row its own height and no more`() {
        val coded = printGeometry(NotebookOptions(photographs = false, numistaQr = true))

        assertEquals(10f, coded.captionMm)
        assertEquals(10f, coded.qrMm)
        assertEquals(0f, coded.qrGapMm)
        assertEquals(10f, coded.cellHeightMm(40.9f))
        // Sólo el pie de foto se mueve: la cabecera, el pie de página y las columnas siguen igual.
        assertEquals(
            printGeometry(NotebookOptions(photographs = false)),
            coded.copy(captionMm = 7f, qrMm = 0f),
        )
        // 23 líneas por columna pasan a 18, que es lo que cuesta llevar el código encima.
        assertEquals(18, printGrid(40.9f, coded).rows)
    }

    /**
     * Two of the five stop being questions when the coins stop being drawn.
     *
     * With the photographs off no coin reaches the page at all, so «ambas caras» and «tamaño real»
     * have nothing to negotiate. The sheet greys them rather than leaving them ticked and inert.
     */
    @Test
    fun `with the photographs off there is no face and no size to negotiate`() {
        val bare = NotebookOptions(photographs = false)

        assertFalse(bare.offers(NotebookSwitch.BothFaces))
        assertFalse(bare.offers(NotebookSwitch.ActualSize))
        // Los otros tres siguen siendo preguntas: una lista sin fotos aún se puede compartir página
        // y llevar el QR, y las fotos se pueden volver a encender.
        assertTrue(bare.offers(NotebookSwitch.Photographs))
        assertTrue(bare.offers(NotebookSwitch.SharePage))
        assertTrue(bare.offers(NotebookSwitch.NumistaQr))

        assertTrue(NotebookSwitch.entries.all { NotebookOptions().offers(it) })
    }

    @Test
    fun `every switch reads and writes the one field it is about`() {
        NotebookSwitch.entries.forEach { switch ->
            val on = NotebookOptions().with(switch, on = true)
            val off = NotebookOptions().with(switch, on = false)

            assertTrue(on[switch], "$switch no se enciende")
            assertFalse(off[switch], "$switch no se apaga")
            NotebookSwitch.entries.filter { it != switch }.forEach { other ->
                assertEquals(NotebookOptions()[other], on[other], "$switch ha movido $other")
            }
        }
    }

    /**
     * The order of the enum is the order of the sheet, and the three that do nothing yet say so.
     *
     * `pending` is what makes a grey switch honest: it is the issue that will make it work, and it
     * goes to null in that issue. «QR de Numista» was the first one at null, «ambas caras» the second
     * and «fotos» the third. When all five are, this property has no reason left to exist.
     */
    @Test
    fun `the five switches are in the order the sheet draws them and two are still pending`() {
        assertEquals(
            listOf(
                NotebookSwitch.Photographs,
                NotebookSwitch.BothFaces,
                NotebookSwitch.ActualSize,
                NotebookSwitch.SharePage,
                NotebookSwitch.NumistaQr,
            ),
            NotebookSwitch.entries.toList(),
        )
        assertEquals(listOf(null, null, 233, 232, null), NotebookSwitch.entries.map { it.pending })
    }
}

/** The thirty-two configurations the five switches can be in. */
internal fun allCombinations(): List<NotebookOptions> = buildList {
    for (photographs in BOTH) {
        for (bothFaces in BOTH) {
            for (actualSize in BOTH) {
                for (sharePage in BOTH) {
                    for (numistaQr in BOTH) {
                        add(
                            NotebookOptions(
                                photographs = photographs,
                                bothFaces = bothFaces,
                                actualSize = actualSize,
                                sharePage = sharePage,
                                numistaQr = numistaQr,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private val BOTH = listOf(false, true)
