package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.print.NotebookExportStep
import com.jenarvaezg.coindex.ui.print.NotebookSwitch

/**
 * The export button. No count: the shelf tally and the options sheet own those, and a count here
 * would miss the plate the «Sin colección» option adds (#354).
 */
fun notebookExportLabel(): String = "Exportar láminas"

/** The temporary label while the notebook is being rendered. */
const val NOTEBOOK_EXPORTING_LABEL = "Exportando…"

const val NOTEBOOK_EXPORTING_EYEBROW: String = "Exportando el cuaderno"

/**
 * Said next to «Cancelar»: the file is only written after the last page, so cancelling leaves no
 * half a notebook.
 */
const val NOTEBOOK_EXPORT_PATIENCE: String =
    "Se comparte cuando esté entero. Puedes cancelar sin perder nada."

/** An export asked for with nothing on the shelf to print. */
const val NOTHING_TO_PRINT_MESSAGE: String = "No hay ninguna colección que llevar al papel."

const val NOTEBOOK_OPTIONS_EYEBROW: String = "Cómo se exporta"

/** The cost above it counts the filtered index, not the whole collection (#228). */
const val NOTEBOOK_COST_SCOPE: String = "Es lo que hay en el índice ahora mismo, con los filtros puestos."

/** What the cost under a single lámina or hoja counts (#401): no index or filters to name. */
fun sheetExportCostScope(sheet: SharedSheet): String =
    "Es esta ${sheet.noun}, con la configuración elegida."

/** One page exports as a bitmap; more need a PDF (#401). */
fun sheetExportAsBitmap(pages: Int): Boolean = pages <= 1

/**
 * What a single lámina or hoja is about to cost (#401): its pages, its own noun, and the format the
 * page count implies.
 */
fun sheetExportCostLabel(sheet: SharedSheet, pages: Int): String {
    val format = if (sheetExportAsBitmap(pages)) "PNG" else "PDF"
    return "${plural(pages, "página", "páginas")} · 1 ${sheet.noun} · $format"
}

/**
 * What the export sheet is about to cost, recounted on every tap (#228). Pages first, since the
 * configuration moves them; láminas after, already chosen by the index's filters and search.
 */
fun notebookCostLabel(pages: Int, cards: Int): String =
    "${plural(pages, "página", "páginas")} · ${plural(cards, "lámina", "láminas")}"

/** What each switch is called on the export sheet. */
fun notebookSwitchLabel(switch: NotebookSwitch): String = when (switch) {
    NotebookSwitch.Photographs -> "Fotos"
    NotebookSwitch.BothFaces -> "Ambas caras"
    NotebookSwitch.ActualSize -> "Tamaño real"
    // Not «Compartir página» (#420): the panel's Compartir button is right below. Named for what
    // it does to the page count above it.
    NotebookSwitch.SharePage -> "Dos por página"
    NotebookSwitch.NumistaQr -> "QR de Numista"
    NotebookSwitch.Unclaimed -> "Sin colección"
    NotebookSwitch.Money -> "El valor"
}

/**
 * Why a switch is greyed out, or null when it is live (#233, #275). The collector can undo either
 * reason: turn the photographs back on, or clear the filter that left no loose coins.
 */
fun notebookSwitchNote(switch: NotebookSwitch, offered: Boolean): String? = when {
    offered -> null
    switch == NotebookSwitch.Unclaimed -> "No hay monedas sueltas que imprimir"
    else -> "Sin fotos no hay nada que ajustar"
}

/**
 * Why a switch on a single lámina or hoja is greyed: the notebook's reasons (#431), since the PNG
 * is the printed page and receives every option. Kept separate so a sheet-only reason has a place.
 */
fun sheetExportSwitchNote(switch: NotebookSwitch, offered: Boolean): String? =
    notebookSwitchNote(switch, offered)

/**
 * Export progress in pages, plus the name of what is being drawn, which tells a stall (photographs
 * not arriving) from steady work.
 */
fun notebookProgressLabel(pagesDone: Int, pages: Int, title: String): String =
    "Página ${(pagesDone + 1).coerceAtMost(pages)} de $pages · $title"

/**
 * The same, for whichever step the export is on. Writing has its own label because a long
 * notebook pauses visibly there, and «Cancelar» is gone by then.
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
 * What the collector is told once the notebook reaches the share sheet. As in
 * [sheetExportMessage], photographs that never arrived are counted and said but don't fail the
 * export. With «fotos» off none are expected, so the shortfall is always zero (#231).
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

/** [notebookExportMessage] for a single lámina or hoja, named as that sheet (#401). */
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

/** Once the notebook is in Descargas (#285). The notification names the PDF. */
fun notebookDownloadMessage(expectedPhotos: Int, loadedPhotos: Int): String =
    downloadMessage(expectedPhotos, loadedPhotos)

/** A notebook that never reached Descargas, with the cause when there is one to act on. */
fun notebookDownloadFailure(cause: String?): String {
    val sentence = "No se pudo descargar el cuaderno"
    return cause?.takeIf { it.isNotBlank() }?.let { "$sentence: $it" } ?: "$sentence."
}

/**
 * A cancelled export says nothing was shared: the file is only written after the last page, so
 * there is no half a notebook to look for.
 */
fun notebookCancelledMessage(pagesDone: Int, pages: Int): String =
    "Exportación cancelada en la página ${(pagesDone + 1).coerceAtMost(pages)} de $pages. " +
        "No se ha compartido nada."

/**
 * The same, cancelled while fetching the photographs: what arrived stays in the cache, so the next
 * export won't fetch it again.
 */
fun notebookWarmCancelledMessage(photographsDone: Int, photographs: Int): String =
    "Exportación cancelada al descargar las fotos ($photographsDone de $photographs). " +
        "Las descargadas se guardan para la próxima."
