package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Las palabras que van en el papel, que no son las que la app dice sobre el papel. */
class PrintedLabelsTest {
    /**
     * The diameter as a number, for the page that has no ruler to hold a coin against (#231).
     *
     * One decimal and a comma, because that is how Numista records it and how the collector says it.
     * A whole number of millimetres drops the decimal instead of printing a zero it never claimed,
     * and a coin nobody measured prints a blank rather than «0 mm».
     */
    @Test
    fun `the diameter prints as a number where the page cannot print it at size`() {
        assertEquals("40,9 mm", printedDiameterLabel(40.9f))
        assertEquals("45,6 mm", printedDiameterLabel(45.6f))
        assertEquals("14,5 mm", printedDiameterLabel(14.5f))
        // Un número redondo no finge un decimal.
        assertEquals("40 mm", printedDiameterLabel(40f))
        assertEquals("33 mm", printedDiameterLabel(33.02f))
        // Y lo que nadie midió no es un cero.
        assertNull(printedDiameterLabel(null))
        assertNull(printedDiameterLabel(0f))
    }

    /**
     * The furniture of the four sections, which is where the notebook says what kind of page this is.
     *
     * Every one of these was a literal in the exporter or a twin in a copy file nobody read (#543):
     * the eyebrow of a curated catalog, the source of the two pages of owned pieces, and the labels of
     * the specification rows. What is pinned here is the wording, because the folio outlives the app
     * and a page that called a derived collection «CATÁLOGO CURADO» could not be corrected by an
     * update.
     *
     * The two eyebrows say which of the two hierarchies the page came out of (ADR 0021 §1), and the
     * two pages that are neither say so with their own: what no collection claims, and what the
     * collector does not own yet.
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
        // Y son cuatro y no tres: dos páginas diciéndose lo mismo serían una sola clase de página.
        assertEquals(eyebrows.size, eyebrows.distinct().size)

        // Los tres orígenes: lo que hay en casa, lo que nadie tiene todavía, y el catálogo de cada
        // lámina, que es el único que no es una constante porque lo pone el curador.
        assertEquals("tu colección en Numista", INVENTORY_SECTION_SOURCE)
        assertEquals("los catálogos curados de Coindex", WISH_SECTION_SOURCE)
    }

    /** Las filas de una especificación impresa, que son etiquetas y no frases. */
    @Test
    fun `the rows of a printed specification are named once each`() {
        assertEquals("País", COUNTRY_FACT_LABEL)
        assertEquals("Piezas", PIECES_FACT_LABEL)
        assertEquals("Valor", VALUE_FACT_LABEL)
        assertEquals("Casillas", WISH_SECTION_COUNT_LABEL)
    }

    /**
     * Where the folio says its coins came from, which is a plural since a folio can hold two plates.
     *
     * The strip at the foot is one per page and the heading is one per plate (#232), so a page that
     * named only the first catalog would attribute the second plate's coins to it. Each source named
     * once — a folio of five plates of the same catalog says it once — and in the order they print.
     */
    @Test
    fun `the foot names every catalog on the folio, once each`() {
        assertEquals("Fuente: Numista", notebookSourceLabel(listOf("Numista")))
        assertEquals(
            "Fuentes: Numista · tu colección en Numista",
            notebookSourceLabel(listOf("Numista", "tu colección en Numista")),
        )
        // Y el mismo catálogo dos veces se dice una: cinco láminas de una serie en un folio no son
        // cinco fuentes, y es esta función la que sostiene lo que promete la palabra «Fuentes».
        assertEquals("Fuente: Numista", notebookSourceLabel(listOf("Numista", "Numista")))
        assertEquals(
            "Fuentes: Numista · tu colección en Numista",
            notebookSourceLabel(listOf("Numista", "tu colección en Numista", "Numista")),
        )
        // Y un folio sin láminas no existe, así que esto es una frase que nadie llega a leer.
        assertEquals("", notebookSourceLabel(emptyList()))
    }

    /**
     * La sección partida en dos folios lo dice; la que cabe en uno, no.
     *
     * «PÁGINA 1 DE 1» es ruido en papel, así que la condición está en quien la llama y no aquí: esta
     * frase existe porque una lámina cortada entre dos folios es indistinguible de dos láminas.
     */
    @Test
    fun `un folio de una sección partida dice cuál es`() {
        assertEquals("PÁGINA 2 DE 3", printedPageOfSection(2, 3))
    }

    /** La regla dice la escala además de la medida: una barra de milímetros que no está a 1:1 es peor que ninguna. */
    @Test
    fun `la regla promete la escala que dibuja`() {
        assertEquals("40 MM · ESCALA 1:1", printedRulerLabel(40))
    }
}
