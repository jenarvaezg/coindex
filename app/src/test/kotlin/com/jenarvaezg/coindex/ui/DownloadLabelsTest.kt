package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the collector is told once a sheet has landed in Descargas (#285, #403).
 *
 * The snackbar names the folder — «Descargado en Descargas» — and carries Abrir so a tap opens
 * the file without digging through the phone. Without a viewer, Abrir is withheld and a failed
 * open says so aloud (#436). The notification still says the shorter «Descargado» with the file
 * name underneath; holes in the sheet are still counted.
 */
class DownloadLabelsTest {
    @Test
    fun `a complete download names Descargas`() {
        assertEquals(
            "Descargado en Descargas",
            sheetDownloadMessage(expectedPhotos = 38, loadedPhotos = 38),
        )
        assertEquals(
            "Descargado en Descargas",
            sheetDownloadMessage(expectedPhotos = 0, loadedPhotos = 0),
        )
        assertEquals(
            "Descargado en Descargas",
            notebookDownloadMessage(expectedPhotos = 120, loadedPhotos = 120),
        )
    }

    @Test
    fun `a download with holes still says how many photos never arrived`() {
        assertEquals(
            "Descargado en Descargas, pero 10 fotos no llegaron a cargar",
            sheetDownloadMessage(expectedPhotos = 38, loadedPhotos = 28),
        )
        assertEquals(
            "Descargado en Descargas, pero una foto no llegó a cargar",
            sheetDownloadMessage(expectedPhotos = 38, loadedPhotos = 37),
        )
        assertEquals(
            "Descargado en Descargas, pero 3 fotos no llegaron a cargar",
            notebookDownloadMessage(expectedPhotos = 120, loadedPhotos = 117),
        )
    }

    @Test
    fun `Abrir is the snackbar action that opens what landed`() {
        assertEquals("Abrir", DOWNLOAD_OPEN_ACTION)
    }

    @Test
    fun `without a viewer Abrir says so instead of crashing`() {
        // Phones without a PDF (or image) viewer exist; ACTION_VIEW must not take the app down (#436).
        assertEquals(
            "No hay ninguna aplicación que pueda abrirlo",
            DOWNLOAD_NO_VIEWER_MESSAGE,
        )
    }

    @Test
    fun `a landed download notice carries Abrir to the file`() {
        // URI as text: android.net.Uri is unavailable to JVM unit tests (same as routes).
        val notice = UiNotice(
            text = downloadMessage(expectedPhotos = 38, loadedPhotos = 38),
            openFile = OpenDownloadedFile(
                uri = "content://downloads/coindex-test.png",
                mimeType = "image/png",
            ),
        )
        assertEquals("Descargado en Descargas", notice.text)
        assertEquals("content://downloads/coindex-test.png", notice.openFile!!.uri)
        assertEquals("image/png", notice.openFile!!.mimeType)
        assertEquals("Abrir", DOWNLOAD_OPEN_ACTION)
    }

    @Test
    fun `a plain notice has no file to open`() {
        assertNull(UiNotice("Ajustes guardados.").openFile)
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
