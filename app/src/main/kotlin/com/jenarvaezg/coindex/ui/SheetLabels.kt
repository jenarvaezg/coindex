package com.jenarvaezg.coindex.ui

import android.net.Uri

/**
 * What an export calls what it has just shared (#219). Every noun here is feminine, so one set of
 * sentences serves them all; a masculine entry would read «Cuaderno completa exportada». The
 * notebook keeps its own sentences (`notebookExportMessage`) for that reason. The list is the
 * annex's export (ADR 0029 §7).
 */
enum class SharedSheet(
    /** Feminine and lower case: it is read behind an article, «No se pudo exportar la lámina». */
    val noun: String,
) {
    PLATE("lámina"),
    PIECES("hoja"),
    LIST("lista"),
}

/**
 * What the collector is told once a sheet reaches the share sheet. Photographs that didn't paint
 * are counted, so a sheet with holes isn't called complete (#67).
 *
 * [tally] is the caller's own sentence, not a number: a plate counts casillas, a collection of
 * pieces uses `countSentence`, which may be a ratio (#226).
 */
fun sheetExportMessage(
    sheet: SharedSheet,
    tally: String,
    expectedPhotos: Int,
    loadedPhotos: Int,
): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    val head = sheet.noun.replaceFirstChar(Char::uppercaseChar)
    return when (absent) {
        0 -> "$head completa exportada · $tally"
        1 -> "$head exportada, pero una foto no llegó a cargar"
        else -> "$head exportada, pero $absent fotos no llegaron a cargar"
    }
}

/**
 * The same when the file couldn't be written or nothing would take it. Keeps the [cause], which
 * the collector can usually act on (no space, no app to share to); a blank cause is left out
 * rather than printed as «null».
 */
fun sheetExportFailure(sheet: SharedSheet, cause: String?): String {
    val sentence = "No se pudo exportar la ${sheet.noun}"
    return cause?.takeIf { it.isNotBlank() }?.let { "$sentence: $it" } ?: "$sentence."
}

/** What the collector is told once a sheet lands in Descargas (#285, #403), holes counted. */
fun sheetDownloadMessage(expectedPhotos: Int, loadedPhotos: Int): String =
    downloadMessage(expectedPhotos, loadedPhotos)

/** The same failure sentence, for a download that never reached Descargas. */
fun sheetDownloadFailure(sheet: SharedSheet, cause: String?): String {
    val sentence = "No se pudo descargar la ${sheet.noun}"
    return cause?.takeIf { it.isNotBlank() }?.let { "$sentence: $it" } ?: "$sentence."
}

/**
 * The snackbar for any download that reached Descargas (#285, #403), sheet or notebook. It names
 * the folder because without POST_NOTIFICATIONS the notification may not show.
 */
fun downloadMessage(expectedPhotos: Int, loadedPhotos: Int): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    val landed = DOWNLOAD_LANDED_MESSAGE
    return when (absent) {
        0 -> landed
        1 -> "$landed, pero una foto no llegó a cargar"
        else -> "$landed, pero $absent fotos no llegaron a cargar"
    }
}

/** Snackbar notice for a file that reached Descargas, with Abrir pointing at it (#403). */
fun downloadLandedNotice(
    expectedPhotos: Int,
    loadedPhotos: Int,
    uri: Uri,
    mimeType: String,
): UiNotice = UiNotice(
    text = downloadMessage(expectedPhotos, loadedPhotos),
    openFile = OpenDownloadedFile(uri.toString(), mimeType),
)

/**
 * What a plate says it holds: «19 casillas». Not «emisiones»: a plate can draw a slot not yet
 * struck, and the progress line above counts only struck ones.
 */
fun plateSheetTally(members: Int): String = plural(members, "casilla", "casillas")

/**
 * Handing a sheet to another app, one word on every screen that offers it (ADR 0026 §5). The
 * secondary action; Descargar is the filled one (#285).
 */
const val SHARE_ACTION: String = "Compartir"

const val DOWNLOAD_ACTION: String = "Descargar"

/**
 * The one way into «Cómo se exporta», with the sheet's noun (#434). The destination is chosen at
 * the end of the panel, as with [notebookExportLabel].
 */
fun sheetExportLabel(sheet: SharedSheet, exporting: Boolean): String =
    if (exporting) "Preparando la ${sheet.noun}…" else "Exportar ${sheet.noun}"

/** The way out of the album and into the catalog the plate was curated from. */
const val NUMISTA_SOURCE_LINK: String = "Fuente en Numista"

/** Above a plate's title: a list somebody else curated, which the collector is filling. */
const val CURATED_CATALOG_EYEBROW: String = "Catálogo curado"

const val PLATE_UNAVAILABLE_EYEBROW: String = "Lámina no disponible"

/**
 * Where a download landed, on the snackbar (#403). It names the folder because the snackbar always
 * shows; the notification uses the shorter [DOWNLOAD_NOTIFICATION_TITLE].
 */
const val DOWNLOAD_LANDED_MESSAGE: String = "Descargado en Descargas"

/** Snackbar action that opens the file that just landed (#403). */
const val DOWNLOAD_OPEN_ACTION: String = "Abrir"

/** What Abrir says when no app on the phone can open the file (#436). */
const val DOWNLOAD_NO_VIEWER_MESSAGE: String = "No hay ninguna aplicación que pueda abrirlo"

/** The notification that a file reached Descargas (#285): a short title over the file name. */
const val DOWNLOAD_NOTIFICATION_TITLE: String = "Descargado"
const val DOWNLOAD_CHANNEL_NAME: String = "Descargas"
const val DOWNLOAD_CHANNEL_EXPLANATION: String = "Láminas y cuadernos guardados en Descargas"

fun downloadNotificationText(fileName: String): String = "$DOWNLOAD_CHANNEL_NAME · $fileName"
