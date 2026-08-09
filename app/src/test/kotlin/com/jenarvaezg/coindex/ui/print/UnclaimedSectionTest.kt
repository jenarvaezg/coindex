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
 * La última lámina del cuaderno: las monedas que ninguna otra imprime (#275).
 *
 * Lo que se comprueba aquí es **dónde va y cuándo existe**; de qué monedas está hecha responde
 * `UnclaimedRowsTest`, porque la elección es del índice y no del impresor — esta función recibe la
 * lista ya hecha justo para no tener opinión sobre ella.
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

    /**
     * Apagado, el cuaderno sale como salía: es la promesa del #228, y aquí se mide contra el propio
     * cuaderno de antes en vez de contra un recuento escrito a mano.
     */
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

    /**
     * Sin sueltas no hay lámina, ni siquiera con el interruptor puesto.
     *
     * El interruptor se pone gris en la hoja de exportación antes de llegar aquí, pero eso es la
     * pantalla: una cabecera con nada debajo se gasta un folio, y la última defensa es que la
     * sección no llegue a existir.
     */
    @Test
    fun `sin monedas sueltas no se imprime una lámina vacía`() {
        val sections = sections(NotebookOptions(unclaimed = true), unclaimed = emptyList())

        assertEquals(cards.size, sections.size)
        assertTrue(sections.none { it.title == "Sin colección" })
    }

    /**
     * Una casilla por fila, que es lo que hace cualquier lámina de piezas del cuaderno.
     *
     * Y ninguna es un hueco: todas estas son monedas que el coleccionista tiene, así que la lámina
     * no puede decirle que le falta ninguna. El pie es el de siempre —lo que identifica a la fila—
     * sin línea de motivo, que el ADR 0021 §12 mandó al informe de campo.
     */
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

    /** Cuenta lo que hay, con la misma frase con la que se cuenta cualquier colección (#226). */
    @Test
    fun `la cabecera cuenta monedas y tipos, y no nombra un país que no tiene`() {
        val lamina = sections(NotebookOptions(unclaimed = true)).last()

        assertEquals(listOf("Piezas" to "2 monedas · 2 tipos"), lamina.facts)
        assertEquals("tu colección en Numista", lamina.source)
        assertNull(lamina.subtitle)
    }

    /**
     * Los cinco interruptores de antes llegan a estas casillas como a las demás.
     *
     * Sin fotos no hay ninguna cara que pedir, que es lo que hace del cuaderno sin fotos el único
     * que no puede salir incompleto (#231); con «ambas caras» son dos ranuras aunque la ficha no
     * haya llegado (#230), porque las casillas de una lámina tienen que cuadrar.
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
