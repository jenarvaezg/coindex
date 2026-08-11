package com.jenarvaezg.coindex.ui

import android.net.Uri

/**
 * What an export calls what it has just shared.
 *
 * There are two sheets a phone shares as a PNG — the plate of a catalog and the pieces of a
 * collection — and they are **both feminine**, which is what lets one sentence serve both. The
 * printed notebook is not, and it counts pages rather than casillas, so it keeps a sentence of its
 * own (`notebookExportMessage`); that is a fact about Spanish and not an oversight.
 *
 * A type rather than a string so an export cannot invent a noun of its own at the call site — which
 * is what the two hand-written sentences did until #219. It does **not** make the agreement safe by
 * construction: a masculine entry added here would read «Cuaderno completa exportada», and the
 * guard against that is this note plus the notebook already having a sentence of its own.
 */
enum class SharedSheet(
    /** Feminine and lower case: it is read behind an article, «No se pudo exportar la lámina». */
    val noun: String,
) {
    PLATE("lámina"),
    PIECES("hoja"),
}

/**
 * What the collector is told once a sheet has been handed to the share sheet.
 *
 * The exported sheet **is** the product: it gets sent to whoever the collection is being shown to,
 * holes included. The old message called any sheet complete as long as every picture had reported
 * back, and a picture that failed reported back exactly like one that arrived — so a sheet with
 * twelve empty cells announced itself as «lámina completa» (issue #67). Counting the ones that
 * actually painted is what makes the sentence true.
 *
 * [tally] is what the sheet says it holds, and it is **the caller's own sentence rather than a
 * number**: a plate counts casillas, and a collection of pieces counts whatever its card counts,
 * which is `countSentence` and may be a ratio. Spelling that expression out a second time here is
 * precisely how the shared PNG came to count differently from the screen that shared it (#226).
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
 * The same, when the file could not be written or nothing would take it.
 *
 * It keeps the [cause] because the two ways this fails are things the collector can act on — no
 * room on disk, no app to share to — and it names the sheet with the same noun the message above
 * would have used.
 *
 * An exception with nothing to say stops at the sentence. Interpolating it anyway is what put the
 * word «null» in front of the collector, which reads as the app having broken rather than as an
 * export that did not happen.
 */
fun sheetExportFailure(sheet: SharedSheet, cause: String?): String {
    val sentence = "No se pudo exportar la ${sheet.noun}"
    return cause?.takeIf { it.isNotBlank() }?.let { "$sentence: $it" } ?: "$sentence."
}

/**
 * What the collector is told once a sheet has landed in Descargas (#285, #403).
 *
 * Names the folder so the phone is not a scavenger hunt, and pairs with Abrir on the snackbar.
 * Holes still get counted — a lámina with empty cells is not «Descargado en Descargas» alone.
 */
fun sheetDownloadMessage(expectedPhotos: Int, loadedPhotos: Int): String =
    downloadMessage(expectedPhotos, loadedPhotos)

/** The same failure sentence, for a download that never reached Descargas. */
fun sheetDownloadFailure(sheet: SharedSheet, cause: String?): String {
    val sentence = "No se pudo descargar la ${sheet.noun}"
    return cause?.takeIf { it.isNotBlank() }?.let { "$sentence: $it" } ?: "$sentence."
}

/**
 * The snackbar for any download that reached Descargas (#285, #403).
 *
 * One sentence for the sheet and the notebook: both land the same way, and a hole is a hole
 * whether it was a casilla or a page. The folder is in the sentence because the notification
 * may stay silent without POST_NOTIFICATIONS — the snackbar has to be enough by itself.
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

/**
 * Snackbar notice for a file that reached Descargas, with Abrir pointing at it (#403).
 *
 * Same sentence the labels already own; the URI is what turns «Abrir» into ACTION_VIEW.
 */
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
 * What a plate says it holds: **«19 casillas»**.
 *
 * «Casillas» and not «emisiones»: a plate can draw a slot the mint has not struck, and the progress
 * line right above it counts only what was. In the singular where there is one — the old sentence
 * pasted the number in front of the plural and would have read «1 casillas».
 */
fun plateSheetTally(members: Int): String = plural(members, "casilla", "casillas")

/**
 * Handing a sheet to another app, said in one word on the three screens that offer it.
 *
 * Three literals for one word (§5). Compartir stays the secondary action everywhere — Descargar is
 * the filled one since #285 — and one string is what keeps the pair from being drawn the other way
 * round on the third screen.
 */
const val SHARE_ACTION: String = "Compartir"

const val DOWNLOAD_ACTION: String = "Descargar"

/**
 * Downloading a sheet, in the noun the sheet itself carries.
 *
 * One pair of sentences for the plate and for the collection, as [sheetDownloadFailure] already has
 * it: both nouns are feminine, which is what lets «Preparando la ${'$'}noun…» serve both, and writing
 * the pair twice is how the two screens would come to draw Descargar and Compartir the other way
 * round on one of them.
 */
fun sheetDownloadLabel(sheet: SharedSheet, exporting: Boolean): String =
    if (exporting) "Preparando la ${sheet.noun}…" else "Descargar ${sheet.noun}"

/** Where the coins on an exported sheet came from, which the PNG has to carry to be checkable. */
fun sheetSourceLabel(source: String): String = "Fuente: $source"

/** The way out of the album and into the catalog the plate was curated from. */
const val NUMISTA_SOURCE_LINK: String = "Fuente en Numista"

/** What a plate is, said once above its title: somebody else's list the collector is filling. */
const val CURATED_CATALOG_EYEBROW: String = "Catálogo curado"

const val PLATE_UNAVAILABLE_EYEBROW: String = "Lámina no disponible"

/** What a sheet of pieces has instead of a curated catalog to name. */
const val PIECES_SHEET_SOURCE: String = "tu colección en Numista"

/**
 * The two mastheads a shared PNG carries.
 *
 * They say which of the two hierarchies the sheet came out of, because a PNG arrives in a chat with
 * no app around it: «catálogo curado» is somebody else's list the collector is filling, and
 * «colección» is the collector's own pieces (ADR 0021 §1).
 */
const val PLATE_SHEET_MASTHEAD: String = "COINDEX · CATÁLOGO CURADO"
const val PIECES_SHEET_MASTHEAD: String = "COINDEX · COLECCIÓN"

/**
 * Where a download landed, said on the snackbar (#403).
 *
 * The notification keeps the shorter [DOWNLOAD_NOTIFICATION_TITLE]; the snackbar has to name the
 * folder because it is the surface that always shows, permission or not.
 */
const val DOWNLOAD_LANDED_MESSAGE: String = "Descargado en Descargas"

/** Snackbar action that opens the file that just landed (#403). */
const val DOWNLOAD_OPEN_ACTION: String = "Abrir"

/**
 * The openable notification that says a file reached Descargas (#285).
 *
 * Short title plus the file name underneath; Abrir on the snackbar is the other door (#403).
 */
const val DOWNLOAD_NOTIFICATION_TITLE: String = "Descargado"
const val DOWNLOAD_CHANNEL_NAME: String = "Descargas"
const val DOWNLOAD_CHANNEL_EXPLANATION: String = "Láminas y cuadernos guardados en Descargas"

fun downloadNotificationText(fileName: String): String = "$DOWNLOAD_CHANNEL_NAME · $fileName"
