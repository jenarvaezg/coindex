package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the collector is told once a sheet has landed in Descargas (#285).
 *
 * The snackbar is short on purpose — «Descargado» — because the openable notification already
 * names the file. Holes in the sheet still get said: eighty-three painted cells are not a
 * complete lámina, and silence would be a lie.
 */
class DownloadLabelsTest {
    @Test
    fun `a complete download is only the word Descargado`() {
        assertEquals("Descargado", sheetDownloadMessage(expectedPhotos = 38, loadedPhotos = 38))
        assertEquals("Descargado", sheetDownloadMessage(expectedPhotos = 0, loadedPhotos = 0))
        assertEquals(
            "Descargado",
            notebookDownloadMessage(expectedPhotos = 120, loadedPhotos = 120),
        )
    }

    @Test
    fun `a download with holes still says how many photos never arrived`() {
        assertEquals(
            "Descargado, pero 10 fotos no llegaron a cargar",
            sheetDownloadMessage(expectedPhotos = 38, loadedPhotos = 28),
        )
        assertEquals(
            "Descargado, pero una foto no llegó a cargar",
            sheetDownloadMessage(expectedPhotos = 38, loadedPhotos = 37),
        )
        assertEquals(
            "Descargado, pero 3 fotos no llegaron a cargar",
            notebookDownloadMessage(expectedPhotos = 120, loadedPhotos = 117),
        )
    }

    @Test
    fun `a failed download names what could not be written`() {
        assertEquals(
            "No se pudo descargar la lámina: ENOSPC",
            sheetDownloadFailure(SharedSheet.PLATE, "ENOSPC"),
        )
        assertEquals(
            "No se pudo descargar la hoja.",
            sheetDownloadFailure(SharedSheet.PIECES, null),
        )
        assertEquals(
            "No se pudo descargar el cuaderno: ENOSPC",
            notebookDownloadFailure("ENOSPC"),
        )
        assertEquals(
            "No se pudo descargar el cuaderno.",
            notebookDownloadFailure("  "),
        )
    }
}
