package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the collector is told once the sheet has left for the share sheet.
 *
 * The sheet that gets shared is the product, so what it is called after exporting has to match
 * what is on it. It used to call any sheet «completa» as long as every picture had reported back,
 * and a picture that failed reported back exactly like one that arrived: twelve empty cells of the
 * 1000 escudos were announced as a complete plate (issue #67).
 */
class PlateExportMessageTest {
    @Test
    fun `an exported sheet is only called complete when every picture is on it`() {
        assertEquals(
            "Lámina completa exportada · 19 casillas",
            plateExportMessage(members = 19, expectedPhotos = 38, loadedPhotos = 38),
        )
    }

    @Test
    fun `a sheet with holes says how many, in the plural it needs`() {
        assertEquals(
            "Lámina exportada, pero 10 fotos no llegaron a cargar",
            plateExportMessage(members = 19, expectedPhotos = 38, loadedPhotos = 28),
        )
        assertEquals(
            "Lámina exportada, pero una foto no llegó a cargar",
            plateExportMessage(members = 19, expectedPhotos = 38, loadedPhotos = 37),
        )
    }

    /** A catalog whose types have no cached pictures exports a complete sheet of silhouettes. */
    @Test
    fun `a sheet that asked for no pictures is complete`() {
        assertEquals(
            "Lámina completa exportada · 3 casillas",
            plateExportMessage(members = 3, expectedPhotos = 0, loadedPhotos = 0),
        )
    }
}
