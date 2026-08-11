package com.jenarvaezg.coindex.ui

const val APP_NAME: String = "COINDEX"
const val SETTINGS_LABEL: String = "Ajustes"

/** The way back, which is the masthead's other half: a screen has either this or the way into Ajustes. */
const val BACK_LABEL: String = "Volver"

/**
 * The three cells of the hierarchy bar, each naming its grain with the count of it.
 *
 * The count is what the destination is **made of** rather than how many things are inside it: cards,
 * Numista types owned, and grams. «Las cifras» counts weight and never money — an amount in a
 * permanent bar is a pocket ticker that puts the collector's estate in front of anyone glancing at
 * the phone (#316).
 */
fun collectionsCellLabel(collections: Int): String = "Colecciones · $collections"

fun coinsCellLabel(coins: Int): String = "Monedas · $coins"

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
 */
data class SewnEdgeCounts(val collections: Int, val pieces: Int, val types: Int)

/**
 * The complete sewn-edge line, owned here so the album chrome carries no copy of its own.
 *
 * The middle count is **pieces** (quantities ×N), never «monedas»: that word already names the
 * sibling hierarchy and its type count in the bar, and using it here made Las cifras print the
 * type count twice under two names (#400). All three words are written in full — abbreviating
 * only «col» left the line half-spoken. The wall-clock minute is gone (#419): the system clock is
 * five millimetres away, and the line never meant «last sync».
 */
fun sewnEdgeLabel(counts: SewnEdgeCounts): String =
    "${counts.collections} colecciones · ${counts.pieces} piezas · ${counts.types} tipos"

/**
 * What the masthead says you are looking at.
 *
 * **Both roots keep the notebook's own strapline** (ADR 0021 §1): Collections and Coins are two
 * hierarchies of one notebook, the bottom bar already says which one you are in, and the heading
 * below names it in full. Every other screen names itself, so a plate opened three taps deep still
 * says which plate it is.
 */
private const val STRAPLINE = "Inventario de campo · plata bullion"

fun screenTitle(route: String?, subjectName: String? = null): String = when {
    route == Routes.SETTINGS -> SETTINGS_LABEL
    route == Routes.NOTICES -> NOTICES_LABEL
    Routes.isPlate(route) -> subjectName?.let { "Lámina · $it" } ?: "Lámina"
    // Both pieces routes say the same word: there is one species of collection (ADR 0021 §2), and
    // «Tu agrupación» was the last place in the app that ranked a box below the rest.
    Routes.isPieces(route) -> subjectName?.let { "Colección · $it" } ?: "Colección"
    // The two roots, and anything unrecognised: never a blank masthead.
    else -> STRAPLINE
}

/** The installed version rides along with the screen name; an APK build needs to be identifiable. */
fun mastheadSubtitle(screenTitle: String, versionName: String): String =
    if (versionName.isEmpty()) screenTitle else "$screenTitle · v$versionName"
