package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the collector is told once a sheet is exported. «Completa» only when every picture arrived
 * (#67); one set of sentences serves both sheets, with noun and count as arguments (#219).
 */
class SheetLabelsTest {
    @Test
    fun `an exported sheet is only called complete when every picture is on it`() {
        assertEquals(
            "Lámina completa exportada · 19 casillas",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(19), 38, 38),
        )
        assertEquals(
            "Hoja completa exportada · 10 monedas · 4 tipos",
            sheetExportMessage(SharedSheet.PIECES, countLabel(4, 10), 12, 12),
        )
    }

    @Test
    fun `a sheet with holes says how many, in the plural it needs`() {
        assertEquals(
            "Lámina exportada, pero 10 fotos no llegaron a cargar",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(19), 38, 28),
        )
        assertEquals(
            "Lámina exportada, pero una foto no llegó a cargar",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(19), 38, 37),
        )
        assertEquals(
            "Hoja exportada, pero 3 fotos no llegaron a cargar",
            sheetExportMessage(SharedSheet.PIECES, countLabel(4, 10), 12, 9),
        )
        assertEquals(
            "Hoja exportada, pero una foto no llegó a cargar",
            sheetExportMessage(SharedSheet.PIECES, countLabel(4, 10), 12, 11),
        )
    }

    /** A catalog whose types have no cached pictures exports a complete sheet of silhouettes. */
    @Test
    fun `a sheet that asked for no pictures is complete`() {
        assertEquals(
            "Lámina completa exportada · 3 casillas",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(3), 0, 0),
        )
    }

    @Test
    fun `more photos than expected never reads as a negative`() {
        assertEquals(
            "Hoja completa exportada · 2 monedas · 2 tipos",
            sheetExportMessage(SharedSheet.PIECES, countLabel(2, 2), 2, 4),
        )
    }

    @Test
    fun `a plate of one slot counts it in the singular`() {
        assertEquals(
            "Lámina completa exportada · 1 casilla",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(1), 2, 2),
        )
    }

    /** Both read [PiecesSubject.countSentence]: a ratio card is announced with its ratio (#226). */
    @Test
    fun `a shared sheet is announced in the words its collection counts itself with`() {
        val subject = PiecesSubject(
            title = "Las francesas",
            issuer = "Francia",
            variant = "Plata · 1 oz",
            coverage = CoverageRatio(0, 12),
            distinctTypes = 3,
            quantity = 4,
            pieces = emptyList(),
            boxId = null,
        )

        assertEquals(
            "Hoja completa exportada · 0 de 12 · te faltan 12",
            sheetExportMessage(SharedSheet.PIECES, subject.countSentence, 12, 12),
        )
    }

    /** Keeps the cause: no disk space or no app to share to are things the collector can fix. */
    @Test
    fun `a failed export says which sheet it could not share`() {
        assertEquals(
            "No se pudo exportar la lámina: la lámina aún no se ha dibujado",
            sheetExportFailure(SharedSheet.PLATE, "la lámina aún no se ha dibujado"),
        )
        assertEquals(
            "No se pudo exportar la hoja: ENOSPC",
            sheetExportFailure(SharedSheet.PIECES, "ENOSPC"),
        )
    }

    @Test
    fun `an exception with no message never says null`() {
        assertEquals(
            "No se pudo exportar la lámina.",
            sheetExportFailure(SharedSheet.PLATE, null),
        )
        assertEquals(
            "No se pudo exportar la hoja.",
            sheetExportFailure(SharedSheet.PIECES, "   "),
        )
    }

    /** One event, two jobs (#285, #403): the snackbar offers Abrir, the notification names it. */
    @Test
    fun `the snackbar names Descargas and the notification keeps Descargado`() {
        assertEquals("Descargado en Descargas", downloadMessage(38, 38))
        assertEquals("Descargado", DOWNLOAD_NOTIFICATION_TITLE)
        assertEquals("Descargas · coindex-venezuela.png", downloadNotificationText("coindex-venezuela.png"))
        assertEquals("Abrir", DOWNLOAD_OPEN_ACTION)
    }

    // A sheet's origin and hierarchy are printed words (#431) pinned in `PrintedLabelsTest` (#543).

    /**
     * Both nouns are feminine, so one pair of sentences serves both (#219). «Exportar», not
     * «Descargar» (#434): the button opens «Cómo se exporta», where the destination is chosen.
     */
    @Test
    fun `each sheet exports under its own noun`() {
        assertEquals("Exportar lámina", sheetExportLabel(SharedSheet.PLATE, exporting = false))
        assertEquals(
            "Preparando la lámina…",
            sheetExportLabel(SharedSheet.PLATE, exporting = true),
        )
        assertEquals("Exportar hoja", sheetExportLabel(SharedSheet.PIECES, exporting = false))
        assertEquals("Preparando la hoja…", sheetExportLabel(SharedSheet.PIECES, exporting = true))
    }

    /** The destination is asked once, at the end (#434). */
    @Test
    fun `Descargar and Compartir belong to the panel and not to the way in`() {
        assertEquals("Descargar", DOWNLOAD_ACTION)
        assertEquals("Compartir", SHARE_ACTION)
        assertTrue(SHARE_ACTION !in sheetExportLabel(SharedSheet.PLATE, exporting = false))
        assertTrue(DOWNLOAD_ACTION !in sheetExportLabel(SharedSheet.PIECES, exporting = false))
    }
}
