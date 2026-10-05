package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Los textos que se imprimen en el cuaderno. */
class PrintedLabelsTest {
    /** For pages not at 1:1 (#231): one decimal and a comma, as Numista records it. */
    @Test
    fun `the diameter prints as a number where the page cannot print it at size`() {
        assertEquals("40,9 mm", printedDiameterLabel(40.9f))
        assertEquals("45,6 mm", printedDiameterLabel(45.6f))
        assertEquals("14,5 mm", printedDiameterLabel(14.5f))
        // Un número redondo no finge un decimal.
        assertEquals("40 mm", printedDiameterLabel(40f))
        assertEquals("33 mm", printedDiameterLabel(33.02f))
        // Lo que nadie midió no se imprime.
        assertNull(printedDiameterLabel(null))
        assertNull(printedDiameterLabel(0f))
    }

    /**
     * The wording is pinned because a printed folio can't be fixed by an update (#543). The
     * eyebrows name the hierarchy the page came from (ADR 0021 §1), or say it is neither.
     */
    @Test
    fun `each kind of page says what it is and where its coins came from`() {
        val eyebrows = listOf(
            PLATE_SECTION_EYEBROW,
            PIECES_SECTION_EYEBROW,
            UNCLAIMED_SECTION_EYEBROW,
            WISH_SECTION_EYEBROW,
        )
        assertEquals(
            listOf(
                "COINDEX · CATÁLOGO CURADO",
                "COINDEX · COLECCIÓN",
                "COINDEX · SIN COLECCIÓN",
                "COINDEX · LO QUE BUSCO",
            ),
            eyebrows,
        )
        // Cuatro distintas: dos iguales serían una sola clase de página.
        assertEquals(eyebrows.size, eyebrows.distinct().size)

        // El tercer origen, el catálogo de cada lámina, no es constante: lo pone el curador.
        assertEquals("tu colección en Numista", INVENTORY_SECTION_SOURCE)
        assertEquals("los catálogos curados de Coindex", WISH_SECTION_SOURCE)
    }

    @Test
    fun `the rows of a printed specification are named once each`() {
        assertEquals("País", COUNTRY_FACT_LABEL)
        assertEquals("Piezas", PIECES_FACT_LABEL)
        assertEquals("Valor", VALUE_FACT_LABEL)
        assertEquals("Casillas", WISH_SECTION_COUNT_LABEL)
    }

    /**
     * The foot strip is per page and a folio can hold several plates (#232), so it names every
     * source, once each, in print order.
     */
    @Test
    fun `the foot names every catalog on the folio, once each`() {
        assertEquals("Fuente: Numista", notebookSourceLabel(listOf("Numista")))
        assertEquals(
            "Fuentes: Numista · tu colección en Numista",
            notebookSourceLabel(listOf("Numista", "tu colección en Numista")),
        )
        // Cinco láminas de una serie en un folio son una sola fuente.
        assertEquals("Fuente: Numista", notebookSourceLabel(listOf("Numista", "Numista")))
        assertEquals(
            "Fuentes: Numista · tu colección en Numista",
            notebookSourceLabel(listOf("Numista", "tu colección en Numista", "Numista")),
        )
        // Un folio sin láminas no existe: esta frase no se llega a leer.
        assertEquals("", notebookSourceLabel(emptyList()))
    }

    /** Sin ella, una lámina partida entre folios parecería dos; quien llama omite «1 DE 1». */
    @Test
    fun `un folio de una sección partida dice cuál es`() {
        assertEquals("PÁGINA 2 DE 3", printedPageOfSection(2, 3))
    }

    /** Una regla de milímetros que no está a 1:1 es peor que ninguna. */
    @Test
    fun `la regla promete la escala que dibuja`() {
        assertEquals("40 MM · ESCALA 1:1", printedRulerLabel(40))
    }
}
