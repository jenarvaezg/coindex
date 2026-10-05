package com.jenarvaezg.coindex.ui

const val APP_NAME: String = "COINDEX"

/**
 * What the sewn edge's glyph opens, named after what the screen is for, maintaining the inventory,
 * rather than its top fields (#521). «Este teléfono» is the subject its lines already have: the
 * photographs, the data and the key are all «en este teléfono».
 */
const val PHONE_LABEL: String = "Este teléfono"

/** The masthead's other action: a screen offers either this or «Este teléfono». */
const val BACK_LABEL: String = "Volver"

/**
 * The three cells of the hierarchy bar, each naming what its destination is made of, with the
 * count: cards, Numista types owned, and grams. Never money (#316).
 *
 * The middle cell says «Tipos» because it counts types, one card per type, as its destination
 * draws them (#516). Counting pieces instead would repeat the sewn edge's number under another
 * word (#400).
 */
fun collectionsCellLabel(collections: Int?): String =
    "Colecciones · ${collections?.toString() ?: UNKNOWN_COUNT}"

fun typesCellLabel(types: Int?): String =
    "Tipos · ${types?.toString() ?: UNKNOWN_COUNT}"

fun figuresCellLabel(count: String): String = "${FiguresLabels.DESTINATION} · $count"

/**
 * What the app says when the curated data it ships with won't load: it stops rather than draw a
 * wrong plate. The exception's message goes under it, for whoever gets sent the screenshot.
 */
const val FATAL_HEADING: String = "No se pudo arrancar"
const val FATAL_EXPLANATION: String =
    "Los datos curados que viajan con la app no son válidos, así que Coindex se detiene en lugar " +
        "de mostrarte una lámina incorrecta."

/**
 * The sewn edge's three magnitudes, computed once above the three roots so they can't show three
 * different totals (#400). Callers pass `null` to [sewnEdgeLabel] until [UiState.loading] clears,
 * so a cold start doesn't claim an empty collection (#418).
 */
data class SewnEdgeCounts(val collections: Int, val pieces: Int, val types: Int)

/** What a count says before the snapshot has landed (#418). */
const val UNKNOWN_COUNT: String = "—"

/**
 * The sewn-edge line, owned here so the album chrome carries no copy of its own. The middle count
 * is pieces (quantities ×N), never «monedas» (#400, #516). Words in full, and no time: the line
 * never meant «last sync» (#419). Just «—» while [counts] is null (#418).
 */
fun sewnEdgeLabel(counts: SewnEdgeCounts?): String =
    if (counts == null) {
        UNKNOWN_COUNT
    } else {
        "${counts.collections} colecciones · ${counts.pieces} piezas · ${counts.types} tipos"
    }

/**
 * The masthead subtitle where no screen names itself: onboarding, the fatal error, an unrecognised
 * route. The roots draw the sewn edge instead of the masthead (ADR 0026 §1).
 */
private const val STRAPLINE = "Inventario de campo · plata bullion"

fun screenTitle(route: String?, subjectName: String? = null): String = screenTitleOf(
    route = route,
    subjectName = subjectName?.weldUnits(),
)

private fun screenTitleOf(route: String?, subjectName: String?): String = when {
    route == Routes.PHONE -> PHONE_LABEL
    route == Routes.NOTICES -> NOTICES_LABEL
    // The row that opens it and its masthead share one string, like the notices (ADR 0026 §14).
    route == Routes.CREDENTIALS -> CREDENTIALS_LABEL
    // Each annex room is titled with the string its door prints (ADR 0029 §6, ADR 0030 §8), as
    // `PrunedVocabularyTest` holds for «Avisos y licencias».
    route == Routes.EXPLORE -> ShowcaseLabels.DESTINATION
    route == Routes.WISHES -> WishLabels.DESTINATION
    // The short name (`ScreenReading.catalogName`), since the plate prints the full `name` just
    // below (#511). Still said, because this bar doesn't scroll and the plate's title does.
    Routes.isPlate(route) -> subjectName?.let { "Lámina · $it" } ?: "Lámina"
    // Both pieces routes say the same word: there is one species of collection (ADR 0021 §2).
    Routes.isPieces(route) -> subjectName?.let { "Colección · $it" } ?: "Colección"
    // The two roots, and anything unrecognised: never a blank masthead.
    else -> STRAPLINE
}
