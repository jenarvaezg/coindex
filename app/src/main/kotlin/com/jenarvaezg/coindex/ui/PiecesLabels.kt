package com.jenarvaezg.coindex.ui

/**
 * The one sentence a collection of pieces counts itself with.
 *
 * It normally counts **piezas**, because a sheet of pieces only ever draws what is owned — which is
 * exactly why a collection without an issue list can have one at all (ADR 0021 §9). But a collection
 * whose catalog it owns no issued member of yet arrives here carrying the card's ratio (§7), and it
 * has to keep saying «0 de 12 · te faltan 12».
 *
 * It is read off the subject and never spelled out again, because the four surfaces that count this
 * collection — the screen, the shared PNG, the notebook page and the message about the file that has
 * just been shared — cannot count differently. Three of them spelled the same expression out and one
 * of them spelled it wrong (#226).
 */
val PiecesSubject.countSentence: String
    get() = coverage?.let(::coverageLabel) ?: countLabel(distinctTypes, quantity)

/**
 * The masthead of the sheet of pieces: what the collection is, and how much of it there is.
 *
 * A pure list rather than a composable's body so a JVM test can read what the shared file says.
 * `PiecesSheetHeading` was private and unreachable, and that is how the PNG came to count
 * differently from the screen that shared it without a single test going red.
 *
 * What nothing can name goes unsaid instead of printed blank: a box has no country of its own and
 * spans whatever physical variants the collector put in it.
 */
fun piecesSheetFacts(subject: PiecesSubject): List<Pair<String, String>> = buildList {
    subject.issuer?.let { issuer -> add("País" to issuer) }
    subject.variant?.let { variant -> add("Variante" to variant) }
    add("Piezas" to subject.countSentence)
}

/**
 * What the collector is told once a sheet of pieces has been handed to the share sheet.
 *
 * It is given the subject and not two numbers: the message counts what the file counts, and the only
 * way to guarantee that is for both to read [countSentence] off the same collection.
 */
fun piecesExportMessage(
    subject: PiecesSubject,
    expectedPhotos: Int,
    loadedPhotos: Int,
): String {
    val absent = (expectedPhotos - loadedPhotos).coerceAtLeast(0)
    return when (absent) {
        // The same sentence the sheet itself prints under the title, so the message and the file
        // that has just been shared cannot count differently.
        0 -> "Hoja completa exportada · ${subject.countSentence}"
        1 -> "Hoja exportada, pero una foto no llegó a cargar"
        else -> "Hoja exportada, pero $absent fotos no llegaron a cargar"
    }
}
