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
     * The door is open and only the switch whose ticket landed has moved anything.
     *
     * Each of the five becomes millimetres in its own ticket (#230-#234), and «QR de Numista» is the
     * first: the caption grows to make room for the code (#234). **A ticket landing moves a switch
     * from one half of this test to the other**, which is the point — until then a grey switch
     * promises nothing, and what has and has not changed behind the door is written down.
     */
    @Test
    fun `only the switch whose ticket landed moves a millimetre`() {
        val everyCombination = allCombinations()

        assertEquals(32, everyCombination.size, "faltan combinaciones de cinco interruptores")
        val moved = everyCombination.filter { printGeometry(it) != PrintGeometry() }
        assertEquals(
            everyCombination.filter { it.numistaQr },
            moved,
            "un interruptor sin ticket ha movido la geometría, o el del #234 no la mueve",
        )
    }

    /**
     * The same again for the cells, which is the half a geometry check cannot see.
     *
     * The configuration reaches `notebookSections` too, because what a cell *is* depends on it —
     * both faces gives it an obverse (#230), no photographs gives it neither (#231). Until those
     * land, none of the four may change a cell: a notebook of 104 pages of empty circles is exactly
     * the half-landed switch #228 refuses to ship. «QR de Numista» is left out because changing a
     * cell is precisely what it does, and `NotebookPagesTest` is where that is measured.
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

        val moved = allCombinations().filterNot { it.numistaQr }.filter { options ->
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
        assertEquals(14f, paper.rulerMm)
        assertEquals(16f, paper.captionMm)
        // Y ningún código: sin él, el rótulo es el pie de foto entero.
        assertEquals(0f, paper.qrMm)
        assertEquals(0f, paper.qrGapMm)
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
     * The order of the enum is the order of the sheet, and the four that do nothing yet say so.
     *
     * `pending` is what makes a grey switch honest: it is the issue that will make it work, and it
     * goes to null in that issue. «QR de Numista» is the first one at null. When all five are, this
     * property has no reason left to exist.
     */
    @Test
    fun `the five switches are in the order the sheet draws them and four are still pending`() {
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
        assertEquals(listOf(231, 230, 233, 232, null), NotebookSwitch.entries.map { it.pending })
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
