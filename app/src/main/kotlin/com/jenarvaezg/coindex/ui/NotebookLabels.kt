package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookSwitch

/**
 * What the export button says it is about to do.
 *
 * The shelf tally already owns the number of visible collections, while the options sheet owns the
 * honest plate and page counts. Keeping this label count-free means it cannot promise one plate too
 * few when the persisted «Sin colección» option adds the loose-coin plate (#354).
 */
fun notebookExportLabel(): String = "Exportar láminas"

/** The temporary label while the notebook is being rendered. */
const val NOTEBOOK_EXPORTING_LABEL = "Exportando…"

const val NOTEBOOK_EXPORTING_EYEBROW: String = "Exportando el cuaderno"

/**
 * Why the wait is safe, said next to the «Cancelar» that would end it.
 *
 * The file is only written once the last page is drawn, so cancelling at page forty leaves no half
 * a notebook anywhere — which is the one thing a collector needs to know before pressing it.
 */
const val NOTEBOOK_EXPORT_PATIENCE: String =
    "Se comparte cuando esté entero. Puedes cancelar sin perder nada."

/** An export asked for with nothing on the shelf to print. */
const val NOTHING_TO_PRINT_MESSAGE: String = "No hay ninguna colección que llevar al papel."

const val NOTEBOOK_OPTIONS_EYEBROW: String = "Cómo se exporta"

/** What the cost above it is counted over, which is the index and not the whole collection (#228). */
const val NOTEBOOK_COST_SCOPE: String = "Es lo que hay en el índice ahora mismo, con los filtros puestos."

/**
 * What the cost under a single lámina or hoja is counted over (#401).
 *
 * The notebook's scope names the index and its filters; a sheet opened from its own screen has
 * neither, and repeating that sentence would claim pages came from a narrowing that is not there.
 */
fun sheetExportCostScope(sheet: SharedSheet): String =
    "La imagen es esta ${sheet.noun}; las páginas son cómo saldría en el cuaderno."

/**
 * What a single lámina or hoja is about to cost (#401).
 *
 * The notebook's line counts láminas because the filter chose many; a sheet opened from its own
 * screen is always one, and the noun has to be this sheet's — «1 lámina» under a hoja would rename
 * what the collector is looking at.
 */
fun sheetExportCostLabel(sheet: SharedSheet, pages: Int): String =
    "${plural(pages, "página", "páginas")} · 1 ${sheet.noun}"

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

/** What each of the six switches is called on the export sheet, in the collector's words. */
fun notebookSwitchLabel(switch: NotebookSwitch): String = when (switch) {
    NotebookSwitch.Photographs -> "Fotos"
    NotebookSwitch.BothFaces -> "Ambas caras"
    NotebookSwitch.ActualSize -> "Tamaño real"
    NotebookSwitch.SharePage -> "Compartir página"
    NotebookSwitch.NumistaQr -> "QR de Numista"
    NotebookSwitch.Unclaimed -> "Sin colección"
    NotebookSwitch.Money -> "El valor"
}

/**
 * Why a switch is greyed out, or null where it is a live question.
 *
 * **Two reasons, and each is about something the collector can undo** (#233, #275). With the
 * photographs off there is no face and no size to negotiate, and that resolves the moment they turn
 * the coins back on; with no loose coin left standing — because there is none, or because the filter
 * above has taken them all — «Sin colección» has no lámina to add, and that resolves when they clear
 * the filter. The reason that went for good was «Pendiente · #233», a switch drawn and remembered
 * before its ticket landed: a note that can never be printed is a branch nobody can read on the sheet.
 */
fun notebookSwitchNote(switch: NotebookSwitch, offered: Boolean): String? = when {
    offered -> null
    switch == NotebookSwitch.Unclaimed -> "No hay monedas sueltas que imprimir"
    else -> "Sin fotos no hay nada que ajustar"
}

/**
 * Why a switch on a single lámina or hoja is annotated (#401).
 *
 * **What fits in a sheet is a PNG** — Descargar and Compartir both leave a bitmap. Tamaño real,
 * the QR and the money only land on the notebook's paper, so the sheet says so under them rather
 * than letting this export look as if it will honour a switch the image cannot. Fotos and Ambas
 * caras stay a live question for the image until the PNG learns them; until then they still
 * remember how the next cuaderno will print.
 */
fun sheetExportSwitchNote(switch: NotebookSwitch, offered: Boolean): String? = when {
    !offered -> notebookSwitchNote(switch, offered)
    switch == NotebookSwitch.ActualSize ||
        switch == NotebookSwitch.NumistaQr ||
        switch == NotebookSwitch.Money -> "Sólo en el cuaderno"
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
 * The same criterion the shared sheets settled on ([sheetExportMessage]): a photograph that never
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
 * What the collector is told once a single lámina or hoja has been handed to the share sheet (#401).
 *
 * Same holes arithmetic as [notebookExportMessage], but the product is this sheet — saying
 * «cuaderno» about one PDF of one collection would rename what the collector just asked for.
 */
fun sheetPdfExportMessage(
    sheet: SharedSheet,
    pages: Int,
    expectedPhotos: Int,
    loadedPhotos: Int,
): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    val head = sheet.noun.replaceFirstChar(Char::uppercaseChar)
    val counted = plural(pages, "página", "páginas")
    return when (absent) {
        0 -> "$head exportada · $counted"
        1 -> "$head exportada · $counted, pero una foto no llegó a cargar"
        else -> "$head exportada · $counted, pero $absent fotos no llegaron a cargar"
    }
}

/**
 * What the collector is told once the notebook has landed in Descargas (#285).
 *
 * Same sentence as a sheet download: the notification names the PDF.
 */
fun notebookDownloadMessage(expectedPhotos: Int, loadedPhotos: Int): String =
    downloadMessage(expectedPhotos, loadedPhotos)

/** A notebook that never reached Descargas, with the cause when there is one to act on. */
fun notebookDownloadFailure(cause: String?): String {
    val sentence = "No se pudo descargar el cuaderno"
    return cause?.takeIf { it.isNotBlank() }?.let { "$sentence: $it" } ?: "$sentence."
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
