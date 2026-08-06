package com.jenarvaezg.coindex.ui.print

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
     * The door is open and the room behind it is today's notebook.
     *
     * Each of the five switches becomes millimetres in its own ticket (#230-#234), so for now the
     * thirty-two combinations are one geometry. **A ticket landing deletes a line of this test**,
     * which is the point: until then a grey switch promises nothing, and the plumbing is provably
     * the only thing that changed.
     */
    @Test
    fun `no combination of the five moves a millimetre yet`() {
        val everyCombination = allCombinations()

        assertEquals(32, everyCombination.size, "faltan combinaciones de cinco interruptores")
        val moved = everyCombination.filter { printGeometry(it) != PrintGeometry() }
        assertEquals(emptyList(), moved, "un interruptor sin ticket ha movido la geometría")
    }

    @Test
    fun `the default configuration declares the geometry the notebook was measured with`() {
        val paper = printGeometry(NotebookOptions())

        assertEquals(210f, paper.widthMm)
        assertEquals(297f, paper.heightMm)
        assertEquals(40f, paper.headingMm)
        assertEquals(14f, paper.rulerMm)
        // 210 menos los dos márgenes; 297 menos los dos márgenes, la cabecera y la regla del pie.
        assertEquals(180f, paper.gridWidthMm)
        assertEquals(213f, paper.gridHeightMm)
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
     * The order of the enum is the order of the sheet, and every one still names its ticket.
     *
     * `pending` is what makes a grey switch honest: it is the issue that will make it work, and it
     * goes to null in that issue. When all five are null this property has no reason left to exist.
     */
    @Test
    fun `the five switches are in the order the sheet draws them and all still pending`() {
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
        assertEquals(listOf(231, 230, 233, 232, 234), NotebookSwitch.entries.map { it.pending })
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
