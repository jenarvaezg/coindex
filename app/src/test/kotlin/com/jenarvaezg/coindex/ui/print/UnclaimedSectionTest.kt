package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.ui.notebookExportLabel
import com.jenarvaezg.coindex.ui.shelf.ShelfFixtures
import com.jenarvaezg.coindex.ui.shelf.unclaimedFacts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La lámina «Sin colección» del cuaderno (#275): dónde va y cuándo existe. Qué monedas lleva lo
 * decide el índice y lo prueba `UnclaimedRowsTest`.
 */
class UnclaimedSectionTest {
    private val curation = Curation(emptyList())
    private val state = ShelfFixtures.state
    private val cards = state.index
    private val loose: List<CollectedItem> = unclaimedFacts(state).map { it.piece }

    private fun sections(
        options: NotebookOptions,
        unclaimed: List<CollectedItem> = loose,
    ) = notebookSections(state, cards, unclaimed, curation, options)

    /** La promesa del #228, medida contra el cuaderno de antes y no contra un recuento a mano. */
    @Test
    fun `apagado no cambia nada, encendido añade una lámina y va la última`() {
        val comoAntes = sections(NotebookOptions())
        val entero = sections(NotebookOptions(unclaimed = true))

        assertEquals(cards.size, comoAntes.size)
        assertEquals(comoAntes, entero.dropLast(1))
        assertEquals("Sin colección", entero.last().title)
        assertEquals("COINDEX · SIN COLECCIÓN", entero.last().eyebrow)
    }

    @Test
    fun `la acción no promete un número cuando sin colección añade una lámina`() {
        val visibles = sections(NotebookOptions())
        val entero = sections(NotebookOptions(unclaimed = true))

        assertEquals(visibles.size + 1, entero.size)
        assertEquals("Exportar láminas", notebookExportLabel())
    }

    /** La hoja de exportación ya deshabilita el interruptor; esto es la última defensa. */
    @Test
    fun `sin monedas sueltas no se imprime una lámina vacía`() {
        val sections = sections(NotebookOptions(unclaimed = true), unclaimed = emptyList())

        assertEquals(cards.size, sections.size)
        assertTrue(sections.none { it.title == "Sin colección" })
    }

    /** Como cualquier lámina de piezas; el motivo va al informe de campo (ADR 0021 §12). */
    @Test
    fun `una casilla por fila, todas llenas y sin decir por qué están ahí`() {
        val lamina = sections(NotebookOptions(unclaimed = true)).last()

        assertEquals(loose.size, lamina.cells.size)
        assertEquals(listOf("Britannia", "Pieza 12"), lamina.cells.map { it.label })
        assertTrue(lamina.cells.all { it.filled })
        assertTrue(lamina.cells.all { it.state == null })
        assertEquals(
            listOf("Sin año · Numista 300", "Sin año · Numista 400"),
            lamina.cells.map { it.footnote },
        )
    }

    /** La misma frase que cuenta cualquier colección (#226). */
    @Test
    fun `la cabecera cuenta monedas y tipos, y no nombra un país que no tiene`() {
        val lamina = sections(NotebookOptions(unclaimed = true)).last()

        assertEquals(listOf("Piezas" to "2 monedas · 2 tipos"), lamina.facts)
        assertEquals("tu colección en Numista", lamina.source)
        assertNull(lamina.subtitle)
    }

    /**
     * Sin fotos no hay caras que pedir (#231); con «ambas caras» hay dos ranuras aunque falte la
     * ficha, para que las casillas cuadren (#230).
     */
    @Test
    fun `la lámina obedece los interruptores de siempre`() {
        val conFotos = sections(NotebookOptions(unclaimed = true)).last()
        val sinFotos = sections(NotebookOptions(unclaimed = true, photographs = false)).last()
        val dosCaras = sections(NotebookOptions(unclaimed = true, bothFaces = true)).last()

        assertEquals(listOf(1, 1), conFotos.cells.map { it.faces.size })
        assertEquals(listOf(0, 0), sinFotos.cells.map { it.faces.size })
        assertEquals(listOf(2, 2), dosCaras.cells.map { it.faces.size })
    }
}
