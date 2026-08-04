package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What an exported sheet of pieces is called on disk.
 *
 * A plate has a curated id to take its name from; a box has only what the collector typed, which
 * is prose — accents, middle dots, whatever they felt like. The file name is the one place that
 * prose meets a file system, so it is flattened here rather than at the share sheet.
 */
class PiecesFileNameTest {
    @Test
    fun `the name the collector typed becomes the name of the file`() {
        assertEquals("coindex-las-francesas", piecesFileName("Las francesas"))
        assertEquals(
            "coindex-sistema-monetario-1969-1980",
            piecesFileName("Sistema monetario 1969-1980"),
        )
    }

    @Test
    fun `accents and punctuation are flattened rather than carried into a file name`() {
        assertEquals("coindex-paquillos-de-alfonso-xiii", piecesFileName("Paquillos de Alfonso XIII"))
        assertEquals("coindex-la-onza-troy-925", piecesFileName("La onza troy · .925"))
        assertEquals("coindex-monnaie-de-paris", piecesFileName("  Monnaie   de  París  "))
    }

    /** A title of nothing but punctuation still has to produce a file, not an empty name. */
    @Test
    fun `a title that flattens to nothing falls back to the word for what it is`() {
        assertEquals("coindex-piezas", piecesFileName("···"))
        assertEquals("coindex-piezas", piecesFileName(""))
    }
}
