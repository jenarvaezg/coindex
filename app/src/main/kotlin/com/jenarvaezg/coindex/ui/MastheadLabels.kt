package com.jenarvaezg.coindex.ui

/**
 * What the masthead says you are looking at.
 *
 * The root keeps the notebook's own strapline, because on the index the screen and the app are
 * the same thing; every other screen names itself, so a plate opened three taps deep still says
 * which plate it is.
 */
private const val STRAPLINE = "Inventario de campo · plata bullion"

fun screenTitle(route: String?, subjectName: String? = null): String = when {
    route == Routes.UNCLASSIFIED -> "Sin clasificar"
    route == Routes.SETTINGS -> "Ajustes"
    Routes.isPlate(route) -> subjectName?.let { "Lámina · $it" } ?: "Lámina"
    Routes.isDerivedCollection(route) -> subjectName?.let { "Colección · $it" } ?: "Colección"
    Routes.isOwnGrouping(route) -> subjectName?.let { "Tu agrupación · $it" } ?: "Tu agrupación"
    // The index, and anything unrecognised: never a blank masthead.
    else -> STRAPLINE
}

/** The installed version rides along with the screen name; an APK build needs to be identifiable. */
fun mastheadSubtitle(screenTitle: String, versionName: String): String =
    if (versionName.isEmpty()) screenTitle else "$screenTitle · v$versionName"
