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
}
