package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the collector is told after a sheet of pieces is shared.
 *
 * It counts **monedas** and never «casillas»: a sheet of pieces has no slot the mint has not struck,
 * which is the whole difference between it and a plate (ADR 0021 §9).
 */
class PiecesLabelsTest {
    /**
     * The message counts what the sheet counts. It used to report the number of rows drawn while
     * the sheet printed `countLabel` under the title, so a collection of four types held in ten
     * pieces was told «6 piezas» about a sheet that said something else.
     */
    @Test
    fun `everything painted reports the sheet in the sheet's own words`() {
        assertEquals(
            "Hoja completa exportada · 10 monedas · 4 tipos",
            piecesExportMessage(
                distinctTypes = 4,
                quantity = 10,
                expectedPhotos = 12,
                loadedPhotos = 12,
            ),
        )
        assertEquals(
            "Hoja completa exportada · 1 moneda · 1 tipo",
            piecesExportMessage(
                distinctTypes = 1,
                quantity = 1,
                expectedPhotos = 2,
                loadedPhotos = 2,
            ),
        )
    }

    /** A picture that never arrived is a hole in the sheet that has just been shared. */
    @Test
    fun `a missing photo is admitted rather than counted as a complete sheet`() {
        assertEquals(
            "Hoja exportada, pero una foto no llegó a cargar",
            piecesExportMessage(
                distinctTypes = 6,
                quantity = 6,
                expectedPhotos = 12,
                loadedPhotos = 11,
            ),
        )
        assertEquals(
            "Hoja exportada, pero 3 fotos no llegaron a cargar",
            piecesExportMessage(
                distinctTypes = 6,
                quantity = 6,
                expectedPhotos = 12,
                loadedPhotos = 9,
            ),
        )
    }

    /** More painted than expected is not a negative complaint: the count floors at zero. */
    @Test
    fun `more photos than expected never reads as a negative`() {
        assertEquals(
            "Hoja completa exportada · 2 monedas · 2 tipos",
            piecesExportMessage(
                distinctTypes = 2,
                quantity = 2,
                expectedPhotos = 2,
                loadedPhotos = 4,
            ),
        )
    }
}
