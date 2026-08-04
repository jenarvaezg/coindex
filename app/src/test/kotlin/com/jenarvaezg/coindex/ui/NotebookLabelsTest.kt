package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the notebook says about itself before, during and after an export that takes minutes. */
class NotebookLabelsTest {
    @Test
    fun `the button promises the size of what it is starting`() {
        assertEquals("Exportar 12 láminas", notebookExportLabel(12))
        assertEquals("Exportar 1 lámina", notebookExportLabel(1))
        assertEquals("Nada que exportar", notebookExportLabel(0))
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
}
