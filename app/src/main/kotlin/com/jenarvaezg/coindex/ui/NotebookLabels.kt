package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep

/**
 * What the export button says it is about to do.
 *
 * The count is the promise: pressing it can take minutes and produce eighty-four pages, so the
 * button names the size of what it is starting rather than saying «Exportar» and finding out.
 * «Láminas» and not «páginas», because what the collector chose is a set of collections — how many
 * pages each takes is the printer's business and the progress notice says it soon enough.
 */
fun notebookExportLabel(cards: Int): String = when (cards) {
    0 -> "Nada que exportar"
    else -> "Exportar ${plural(cards, "lámina", "láminas")}"
}

/**
 * Where the export has got to, said in pages and in the name of what is being drawn.
 *
 * Both halves earn their place: the pages are the only honest measure of how much is left, and the
 * name is what tells a stall — a collection whose photographs are not arriving — from steady work.
 */
fun notebookProgressLabel(pagesDone: Int, pages: Int, title: String): String =
    "Página ${(pagesDone + 1).coerceAtMost(pages)} de $pages · $title"

/**
 * The same, for whichever step the export is on.
 *
 * The writing step says so instead of freezing on the last page, because that is where a notebook
 * of eighty pages spends a visible pause and where the «Cancelar» next to it disappears.
 */
fun notebookStepLabel(step: NotebookExportStep, pages: Int): String = when (step) {
    is NotebookExportStep.Drawing ->
        notebookProgressLabel(step.pagesDone, pages, step.title)
    NotebookExportStep.Writing ->
        "Guardando el cuaderno · ${plural(pages, "página", "páginas")}"
}

/**
 * What the collector is told once the notebook has been handed to the share sheet.
 *
 * The same criterion the single plate settled on (`plateExportMessage`): a photograph that never
 * arrived is a hole in a page that is about to be sent to somebody, so it is counted and said out
 * loud, and it never fails the export — eighty-three good pages are not thrown away for one blank
 * cell.
 */
fun notebookExportMessage(pages: Int, expectedPhotos: Int, loadedPhotos: Int): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    val counted = plural(pages, "página", "páginas")
    return when (absent) {
        0 -> "Cuaderno completo exportado · $counted"
        1 -> "Cuaderno exportado en $counted, pero una foto no llegó a cargar"
        else -> "Cuaderno exportado en $counted, pero $absent fotos no llegaron a cargar"
    }
}

/**
 * What a cancelled export says.
 *
 * It has to say that nothing was shared. The file is only written once the last page is drawn, so a
 * cancelled export leaves no half a notebook anywhere — and a collector who cancelled at page forty
 * would otherwise go looking for it.
 */
fun notebookCancelledMessage(pagesDone: Int, pages: Int): String =
    "Exportación cancelada en la página ${(pagesDone + 1).coerceAtMost(pages)} de $pages. " +
        "No se ha compartido nada."
