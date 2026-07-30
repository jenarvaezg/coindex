package com.jenarvaezg.coindex.ui

/** Every destination the notebook has. The masthead reads these to name the current screen. */
object Routes {
    const val INDEX = "index"
    const val UNCLASSIFIED = "unclassified"
    const val SETTINGS = "settings"
    const val PLATE = "plate/{catalogId}"

    fun plate(catalogId: String): String = "plate/$catalogId"

    fun isPlate(route: String?): Boolean = route == PLATE
}
