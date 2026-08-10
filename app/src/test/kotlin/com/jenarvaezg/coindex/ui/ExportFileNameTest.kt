package com.jenarvaezg.coindex.ui

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What a download is called in Descargas when the same lámina leaves twice (#285).
 *
 * The base name alone would collide; the date of the tap is what makes each file its own, and
 * readable from the Downloads list without opening it.
 */
class ExportFileNameTest {
    @Test
    fun `a download carries the moment it was saved, so a second tap never overwrites`() {
        val at = LocalDateTime.of(2026, 8, 10, 11, 48, 7)
        assertEquals(
            "coindex-venezuela-1-bolivar-2026-08-10-114807.png",
            datedExportFileName("coindex-venezuela-1-bolivar", "png", at),
        )
        assertEquals(
            "coindex-cuaderno-2026-08-10-114807.pdf",
            datedExportFileName("coindex-cuaderno", "pdf", at),
        )
    }

    @Test
    fun `hours and minutes under ten keep their leading zero`() {
        val at = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        assertEquals(
            "coindex-piezas-2026-01-02-030405.png",
            datedExportFileName("coindex-piezas", "png", at),
        )
    }
}
