package com.jenarvaezg.coindex.ui

import androidx.navigation.NavController

/**
 * The bottom bar. Crossing piles up no entries, and each root comes back as it was left, search
 * and scroll included.
 */
internal fun NavController.crossToRoot(route: String) {
    navigate(route) {
        popUpTo(Routes.INDEX) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * A cross that brings its own filters to Monedas (#386, #634) opens it fresh, so a search left
 * there cannot hide what the cross promised (#645).
 */
internal fun NavController.crossToFreshCoins() {
    // A Monedas left saved would come back on the next bar crossing. By id, because the route
    // overload also drops the entry the bar keeps for Colecciones.
    graph.findNode(Routes.COINS)?.let { clearBackStack(it.id) }
    navigate(Routes.COINS) {
        popUpTo(Routes.INDEX) { saveState = true }
        launchSingleTop = true
    }
}
