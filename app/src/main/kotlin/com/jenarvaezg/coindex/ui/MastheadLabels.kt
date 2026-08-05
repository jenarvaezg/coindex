package com.jenarvaezg.coindex.ui

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
    route == Routes.SETTINGS -> "Ajustes"
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
