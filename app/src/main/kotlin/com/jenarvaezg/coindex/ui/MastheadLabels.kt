package com.jenarvaezg.coindex.ui

const val APP_NAME: String = "COINDEX"

/**
 * What the sewn edge's glyph opens, named after the trip and not after its top two fields (#521).
 *
 * It was «Ajustes» while the screen led with the API key, and the audit of 14 August 2026 measured
 * what that word was covering: 87 of the 165 words on it — `Sincronizar`, the two queues and the
 * export — are the **maintenance of the inventory**, and the one filled action on the screen was the
 * sync. «Datos» was the audit's own candidate and it is not this: `Exportar datos` is a button on the
 * screen, and a page named after one of its buttons reads as that button's room.
 *
 * «Este teléfono» is the subject every line on it already had — the photographs «en este teléfono»,
 * the base «de este teléfono», the key stored «en este teléfono» — so the name is the notebook's own
 * words rather than a category borrowed from an operating system.
 */
const val PHONE_LABEL: String = "Este teléfono"

/**
 * The way back, which is the masthead's other half: a screen has either this or the way into «Este
 * teléfono».
 */
const val BACK_LABEL: String = "Volver"

/**
 * The three cells of the hierarchy bar, each naming its grain with the count of it.
 *
 * The count is what the destination is **made of** rather than how many things are inside it: cards,
 * Numista types owned, and grams. «Las cifras» counts weight and never money — an amount in a
 * permanent bar is a pocket ticker that puts the collector's estate in front of anyone glancing at
 * the phone (#316).
 *
 * The middle cell says **«Tipos»** and not «Monedas» (#516). It always counted types, and over a
 * collection of 72 coins in 15 types the bar's one permanent number was under the wrong word. The
 * count is the one to keep — it is what that destination draws, one card per type, and what the
 * screen's own filter shelf tallies two rows below it — so the label is what moved. Counting pieces
 * instead was the other candidate and costs more: «Monedas · 72» over a sewn edge that reads
 * «72 piezas» is one number under two words, which is the clash of #400 rebuilt.
 */
fun collectionsCellLabel(collections: Int?): String =
    "Colecciones · ${collections?.toString() ?: UNKNOWN_COUNT}"

fun typesCellLabel(types: Int?): String =
    "Tipos · ${types?.toString() ?: UNKNOWN_COUNT}"

fun figuresCellLabel(count: String): String = "${FiguresLabels.DESTINATION} · $count"

/**
 * What the app says when the curated data it ships with will not load.
 *
 * It stops rather than draw a wrong plate, and it says so in those terms: the collector cannot fix
 * the assets, so what they need is the reason the screen is empty and the confidence that nothing of
 * theirs was lost. The exception's own message rides under it, because that one is for whoever gets
 * sent the screenshot.
 */
const val FATAL_HEADING: String = "No se pudo arrancar"
const val FATAL_EXPLANATION: String =
    "Los datos curados que viajan con la app no son válidos, así que Coindex se detiene en lugar " +
        "de mostrarte una lámina incorrecta."

/**
 * The three magnitudes of the sewn edge, computed once above the three roots so Colecciones,
 * Monedas and Las cifras cannot invent three totals for the same words (#400).
 *
 * **Absent while the snapshot is unread** (#418): a cold start with zeros would claim the
 * collection is empty for a second before the placeholder finishes reading it. Callers pass
 * `null` to [sewnEdgeLabel] until [UiState.loading] clears.
 */
data class SewnEdgeCounts(val collections: Int, val pieces: Int, val types: Int)

/** What a count says before the snapshot has landed (#418). */
const val UNKNOWN_COUNT: String = "—"

/**
 * The complete sewn-edge line, owned here so the album chrome carries no copy of its own.
 *
 * The middle count is **pieces** (quantities ×N), never «monedas»: using that word here made Las
 * cifras print the type count twice under two names (#400), and the word has no owner left to borrow
 * it from since the bar stopped spending it on a count of types (#516). All three words are written
 * in full — abbreviating only «col» left the line half-spoken. The wall-clock minute is gone (#419):
 * the system clock is five millimetres away, and the line never meant «last sync».
 *
 * While [counts] is null the line is just «—»: inventing «0 colecciones · 0 piezas · 0 tipos»
 * would contradict the placeholder that is still reading (#418).
 */
fun sewnEdgeLabel(counts: SewnEdgeCounts?): String =
    if (counts == null) {
        UNKNOWN_COUNT
    } else {
        "${counts.collections} colecciones · ${counts.pieces} piezas · ${counts.types} tipos"
    }

/**
 * What the masthead says you are looking at.
 *
 * **Both roots keep the notebook's own strapline** (ADR 0021 §1): Collections and Coins are two
 * hierarchies of one notebook, the bottom bar already says which one you are in, and the heading
 * below names it in full. Every other screen names itself, so a plate opened three taps deep still
 * says which plate it is.
 */
private const val STRAPLINE = "Inventario de campo · plata bullion"

fun screenTitle(route: String?, subjectName: String? = null): String = screenTitleOf(
    route = route,
    subjectName = subjectName?.weldUnits(),
)

private fun screenTitleOf(route: String?, subjectName: String?): String = when {
    route == Routes.PHONE -> PHONE_LABEL
    route == Routes.NOTICES -> NOTICES_LABEL
    // The same pairing the notices have kept since §14: the row that opens it and the masthead of
    // what opens are one string, so a foot that led somewhere called something else cannot happen.
    route == Routes.CREDENTIALS -> CREDENTIALS_LABEL
    // Each of the annex's two rooms names itself with the string its own door prints (ADR 0029 §6,
    // ADR 0030 §8): a door that opened a screen called something else would read as two features,
    // which is the pairing `PrunedVocabularyTest` already holds for «Avisos y licencias». The shelf's
    // door is the index's last row and says what is behind it with its count, which is §8's clause 3.
    route == Routes.EXPLORE -> ShowcaseLabels.DESTINATION
    route == Routes.WISHES -> WishLabels.DESTINATION
    // The **card-sized** name and not the editorial one (`CoindexViewModel.catalogName`): the plate
    // prints `name` whole two lines under this bar, and printing it here as well was one sentence
    // said twice on one screen (#511). Silence was the other candidate and it costs more than it
    // saves — this bar does not scroll, so on a plate of 22 casillas it is the only thing left
    // saying which lámina you are in.
    Routes.isPlate(route) -> subjectName?.let { "Lámina · $it" } ?: "Lámina"
    // Both pieces routes say the same word: there is one species of collection (ADR 0021 §2), and
    // «Tu agrupación» was where this screen ranked a box below the rest — not the last such place in
    // the app, which is what this comment claimed until #516 found four more: the door of the mode,
    // its band, the eyebrow of the baptism and the índice's ounce chip.
    Routes.isPieces(route) -> subjectName?.let { "Colección · $it" } ?: "Colección"
    // The two roots, and anything unrecognised: never a blank masthead.
    else -> STRAPLINE
}
