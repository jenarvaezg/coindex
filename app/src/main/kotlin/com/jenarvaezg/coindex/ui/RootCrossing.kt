package com.jenarvaezg.coindex.ui

import androidx.navigation.NavController

/**
 * The bottom bar. The roots are siblings, not a stack: crossing piles up no entries and each root
 * comes back as it was left, search and scroll included.
 */
internal fun NavController.crossToRoot(route: String) {
    navigate(route) {
        popUpTo(Routes.INDEX) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * A cross that brings its own filters to Monedas (#386, #634) opens it fresh: a search left there
 * would hide what the cross promised (#645). The saved Monedas is dropped rather than left
 * unrestored, which would keep it mapped and let the next bar crossing bring it back. By id: the
 * route overload also drops the mapping the bar keeps for Colecciones.
 */
internal fun NavController.crossToFreshCoins() {
    graph.findNode(Routes.COINS)?.let { clearBackStack(it.id) }
    navigate(Routes.COINS) {
        popUpTo(Routes.INDEX) { saveState = true }
        launchSingleTop = true
    }
}
