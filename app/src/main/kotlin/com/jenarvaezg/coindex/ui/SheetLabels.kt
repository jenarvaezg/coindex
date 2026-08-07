package com.jenarvaezg.coindex.ui

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
 * What a plate says it holds: **«19 casillas»**.
 *
 * «Casillas» and not «emisiones»: a plate can draw a slot the mint has not struck, and the progress
 * line right above it counts only what was. In the singular where there is one — the old sentence
 * pasted the number in front of the plural and would have read «1 casillas».
 */
fun plateSheetTally(members: Int): String = plural(members, "casilla", "casillas")
