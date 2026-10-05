package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookSwitch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the notebook says about itself before, during and after an export that takes minutes. */
class NotebookLabelsTest {
    /** Pages first: the switches move them, while the filter already chose the láminas (#228). */
    @Test
    fun `the sheet says what the export is about to cost, in pages and in plates`() {
        assertEquals("104 páginas · 60 láminas", notebookCostLabel(104, 60))
        assertEquals("1 página · 1 lámina", notebookCostLabel(1, 1))
        // A checklist: fewer pages, the same láminas.
        assertEquals("19 páginas · 60 láminas", notebookCostLabel(19, 60))
    }

    @Test
    fun `every switch is named in the collector's own words`() {
        assertEquals(
            listOf(
                "Fotos",
                "Ambas caras",
                "Tamaño real",
                // Not «Compartir página», which clashed with the panel's Compartir button (#420).
                "Dos por página",
                "QR de Numista",
                "Sin colección",
                "El valor",
            ),
            NotebookSwitch.entries.map(::notebookSwitchLabel),
        )
    }

    /** Said on the spot; both reasons are something the collector can undo (#233, #275). */
    @Test
    fun `a greyed switch says which reason it is, and a live one says nothing`() {
        assertEquals(
            "Sin fotos no hay nada que ajustar",
            notebookSwitchNote(NotebookSwitch.ActualSize, offered = false),
        )
        assertEquals(
            "No hay monedas sueltas que imprimir",
            notebookSwitchNote(NotebookSwitch.Unclaimed, offered = false),
        )
        assertNull(notebookSwitchNote(NotebookSwitch.Unclaimed, offered = true))
    }

    @Test
    fun `progress counts the page being drawn and names the collection`() {
        assertEquals(
            "Página 1 de 84 · Personalidades destacadas de Rusia",
            notebookProgressLabel(0, 84, "Personalidades destacadas de Rusia"),
        )
        assertEquals(
            "Página 84 de 84 · Venezuela reales",
            notebookProgressLabel(83, 84, "Venezuela reales"),
        )
        // The last page reports done before the notebook is written; it never says «85 de 84».
        assertEquals("Página 84 de 84 · x", notebookProgressLabel(84, 84, "x"))
    }

    /** The steps differ because only drawing offers «Cancelar»; writing the file does not. */
    @Test
    fun `the writing step says so instead of freezing on the last page`() {
        // The long step, and it counts photographs because no page exists yet.
        assertEquals(
            "Descargando fotos · 320 de 623",
            notebookStepLabel(NotebookExportStep.Warming(320, 623), 84),
        )
        assertEquals(
            "Página 3 de 84 · Fuertes",
            notebookStepLabel(NotebookExportStep.Drawing(2, "Fuertes"), 84),
        )
        assertEquals(
            "Guardando el cuaderno · 84 páginas",
            notebookStepLabel(NotebookExportStep.Writing, 84),
        )
        assertEquals(
            "Guardando el cuaderno · 1 página",
            notebookStepLabel(NotebookExportStep.Writing, 1),
        )
    }

    /** As for a single plate (#67): a missing photo is said aloud and never fails the export. */
    @Test
    fun `the closing message counts the photographs that never arrived`() {
        assertEquals(
            "Cuaderno completo exportado · 84 páginas",
            notebookExportMessage(84, 1_044, 1_044),
        )
        assertEquals(
            "Cuaderno exportado en 84 páginas, pero una foto no llegó a cargar",
            notebookExportMessage(84, 1_044, 1_043),
        )
        assertEquals(
            "Cuaderno exportado en 84 páginas, pero 12 fotos no llegaron a cargar",
            notebookExportMessage(84, 1_044, 1_032),
        )
        // Never a negative shortfall, whatever order the callbacks landed in.
        assertEquals("Cuaderno completo exportado · 1 página", notebookExportMessage(1, 12, 13))
        // With «Fotos» off (#231) nothing was asked for, so nothing can be missing.
        assertEquals("Cuaderno completo exportado · 74 páginas", notebookExportMessage(74, 0, 0))
    }

    @Test
    fun `a cancelled export says that nothing was shared`() {
        val message = notebookCancelledMessage(12, 84)

        assertEquals(
            "Exportación cancelada en la página 13 de 84. No se ha compartido nada.",
            message,
        )
        assertTrue("No se ha compartido nada" in message)
    }

    @Test
    fun `cancelling the download says that what arrived is kept`() {
        assertEquals(
            "Exportación cancelada al descargar las fotos (320 de 623). " +
                "Las descargadas se guardan para la próxima.",
            notebookWarmCancelledMessage(320, 623),
        )
    }

    /**
     * No filter sits above a single lámina, so the notebook's scope sentence would lie (#401). One
     * page exports as a PNG, more as a PDF, and the line says which.
     */
    @Test
    fun `a single sheet says the cost is about this plate or this leaf`() {
        assertEquals(
            "Es esta lámina, con la configuración elegida.",
            sheetExportCostScope(SharedSheet.PLATE),
        )
        assertEquals(
            "Es esta hoja, con la configuración elegida.",
            sheetExportCostScope(SharedSheet.PIECES),
        )
        assertEquals("1 página · 1 lámina · PNG", sheetExportCostLabel(SharedSheet.PLATE, 1))
        assertEquals("2 páginas · 1 lámina · PDF", sheetExportCostLabel(SharedSheet.PLATE, 2))
        assertEquals("3 páginas · 1 hoja · PDF", sheetExportCostLabel(SharedSheet.PIECES, 3))
        assertTrue(sheetExportAsBitmap(1))
        assertTrue(!sheetExportAsBitmap(2))
    }

    /** Names the lámina, not the notebook (#401). */
    @Test
    fun `sharing a single sheet names the sheet and counts its pages`() {
        assertEquals(
            "Lámina exportada · 1 página",
            sheetPdfExportMessage(SharedSheet.PLATE, pages = 1, expectedPhotos = 19, loadedPhotos = 19),
        )
        assertEquals(
            "Hoja exportada · 3 páginas, pero una foto no llegó a cargar",
            sheetPdfExportMessage(SharedSheet.PIECES, pages = 3, expectedPhotos = 12, loadedPhotos = 11),
        )
        assertEquals(
            "Lámina exportada · 2 páginas, pero 4 fotos no llegaron a cargar",
            sheetPdfExportMessage(SharedSheet.PLATE, pages = 2, expectedPhotos = 40, loadedPhotos = 36),
        )
    }

    /** Since #431 the PNG is the printed page trimmed to its folio, so every switch reaches it. */
    @Test
    fun `no switch is greyed for landing in a PNG, because they all land in it`() {
        for (switch in listOf(
            NotebookSwitch.Photographs,
            NotebookSwitch.BothFaces,
            NotebookSwitch.ActualSize,
            NotebookSwitch.NumistaQr,
            NotebookSwitch.Money,
        )) {
            assertNull(sheetExportSwitchNote(switch, offered = true))
        }
        // The notebook's own two reasons still apply.
        assertEquals(
            "Sin fotos no hay nada que ajustar",
            sheetExportSwitchNote(NotebookSwitch.ActualSize, offered = false),
        )
        assertEquals(
            notebookSwitchNote(NotebookSwitch.Unclaimed, offered = false),
            sheetExportSwitchNote(NotebookSwitch.Unclaimed, offered = false),
        )
    }
}
