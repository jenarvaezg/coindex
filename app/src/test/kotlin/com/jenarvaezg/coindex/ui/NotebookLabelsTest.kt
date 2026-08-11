package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookSwitch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the notebook says about itself before, during and after an export that takes minutes. */
class NotebookLabelsTest {
    /**
     * The line under the five switches, which is what the export sheet exists for (#228).
     *
     * Pages first, because they are what the configuration moves and what the collector is deciding
     * about; láminas after, because the filter already chose those and no switch changes them.
     */
    @Test
    fun `the sheet says what the export is about to cost, in pages and in plates`() {
        assertEquals("104 páginas · 60 láminas", notebookCostLabel(104, 60))
        assertEquals("1 página · 1 lámina", notebookCostLabel(1, 1))
        // A checklist of nineteen pages over the same sixty collections is the whole point of
        // recounting: the láminas do not move and the paper does.
        assertEquals("19 páginas · 60 láminas", notebookCostLabel(19, 60))
    }

    @Test
    fun `every switch is named in the collector's own words`() {
        assertEquals(
            listOf(
                "Fotos",
                "Ambas caras",
                "Tamaño real",
                "Compartir página",
                "QR de Numista",
                "Sin colección",
                "El valor",
            ),
            NotebookSwitch.entries.map(::notebookSwitchLabel),
        )
    }

    /**
     * Why a switch is grey, said on the spot rather than in a help screen.
     *
     * **Two reasons** (#233, #275), and each is about something the collector can undo: the
     * configuration they built made the question moot, or the narrowing they put on leaves no loose
     * coin for «Sin colección» to print. «Pendiente · #233» was the third — a switch drawn and
     * remembered before its ticket landed — and it went for good with the last of the five.
     */
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

    /**
     * The two steps say different things, because they offer different things: while pages are
     * being drawn there is a «Cancelar» beside the line, and once the file is being written there
     * is not.
     */
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

    /**
     * The same criterion the single plate settled on in #67: a photograph that never arrived is a
     * hole in a page somebody is about to be shown, so it is said out loud and it never fails the
     * export.
     */
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
        // Y con «fotos» apagada (#231) no queda nada de lo que hablar: cero pedidas, cero llegadas,
        // y ninguna forma de decir «pero 3 fotos no llegaron» sobre un cuaderno que no pidió una.
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

    /**
     * Cancelling the download is «not now» and not «start over»: what arrived stays in the cache, so
     * the next export does not ask Numista for it again.
     */
    @Test
    fun `cancelling the download says that what arrived is kept`() {
        assertEquals(
            "Exportación cancelada al descargar las fotos (320 de 623). " +
                "Las descargadas se guardan para la próxima.",
            notebookWarmCancelledMessage(320, 623),
        )
    }

    /**
     * The cost under a single lámina is not the index: there is no filter above it, and the
     * notebook's scope sentence would lie about where the pages come from (#401). The format is
     * measured by pages — one is a PNG, more is a PDF — and the line announces it.
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

    /**
     * Sharing one PDF page of a lámina still names the lámina, not the whole notebook (#401).
     */
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

    @Test
    fun `paper switches are annotated only when the result is a PNG`() {
        for (switch in listOf(
            NotebookSwitch.Photographs,
            NotebookSwitch.BothFaces,
            NotebookSwitch.ActualSize,
            NotebookSwitch.NumistaQr,
            NotebookSwitch.Money,
        )) {
            assertEquals(
                "Sólo en el cuaderno",
                sheetExportSwitchNote(switch, offered = true, pages = 1),
            )
            assertNull(sheetExportSwitchNote(switch, offered = true, pages = 2))
        }
        assertEquals(
            "Sin fotos no hay nada que ajustar",
            sheetExportSwitchNote(NotebookSwitch.ActualSize, offered = false, pages = 1),
        )
    }
}
