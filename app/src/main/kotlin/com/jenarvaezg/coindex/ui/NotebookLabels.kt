package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookSwitch

/**
 * What the export button says it is about to do.
 *
 * The count is the promise: pressing it can take minutes and produce eighty-four pages, so the
 * button names the size of what it is starting rather than saying «Exportar» and finding out.
 * «Láminas» and not «páginas», because what the collector chose is a set of collections — how many
 * pages each takes is the printer's business, and since #228 the sheet the button opens says it
 * before a single one is drawn.
 */
fun notebookExportLabel(cards: Int): String = when (cards) {
    0 -> "Nada que exportar"
    else -> "Exportar ${plural(cards, "lámina", "láminas")}"
}

/**
 * What the export sheet is about to cost, recounted on every tap (#228).
 *
 * The pages first, because they are what the configuration moves and what the collector is deciding
 * about; the láminas after, because they are what the filter already chose and do not change here.
 * It is pure arithmetic over what the index is showing at that moment — filters and search included
 * — which is why the count can only live in the sheet: in Ajustes there is no index to speak of.
 */
fun notebookCostLabel(pages: Int, cards: Int): String =
    "${plural(pages, "página", "páginas")} · ${plural(cards, "lámina", "láminas")}"

/** What each of the five switches is called on the export sheet, in the collector's words. */
fun notebookSwitchLabel(switch: NotebookSwitch): String = when (switch) {
    NotebookSwitch.Photographs -> "Fotos"
    NotebookSwitch.BothFaces -> "Ambas caras"
    NotebookSwitch.ActualSize -> "Tamaño real"
    NotebookSwitch.SharePage -> "Compartir página"
    NotebookSwitch.NumistaQr -> "QR de Numista"
}

/**
 * Why a switch is greyed out, or null where it is a live question.
 *
 * Two reasons and they read differently on purpose. **Pending** is about the app: the switch is drawn
 * and remembered but its ticket has not landed, and saying which one is what keeps a grey control
 * from reading as a bug. **Derived** is about the configuration the collector has built: with the
 * photographs off there is no face and no size left to negotiate, and that one resolves itself the
 * moment they turn the coins back on.
 */
fun notebookSwitchNote(switch: NotebookSwitch, offered: Boolean): String? = when {
    !offered -> "Sin fotos no hay nada que ajustar"
    switch.pending != null -> "Pendiente · #${switch.pending}"
    else -> null
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
    // In photographs, not in pages: it is the step that takes the time, and no page exists yet.
    is NotebookExportStep.Warming ->
        "Descargando fotos · ${step.photographsDone} de ${step.photographs}"
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
 *
 * **With «fotos» off it cannot speak of photographs at all** (#231), and not because it checks: a
 * notebook of lines asks for none, so both counts are zero and the shortfall between them cannot be
 * anything but zero either. A message saying «pero 3 fotos no llegaron a cargar» about a notebook
 * with no photographs in it would be a lie, and the way to make a lie impossible is to leave nothing
 * that could tell it.
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

/**
 * The same, cancelled while the photographs were still being fetched.
 *
 * It says that what arrived is kept, because it is: the pictures live in the cache from now on, so
 * cancelling here is «not now» and not «start over» — the next export will not ask for them again.
 */
fun notebookWarmCancelledMessage(photographsDone: Int, photographs: Int): String =
    "Exportación cancelada al descargar las fotos ($photographsDone de $photographs). " +
        "Las descargadas se guardan para la próxima."
