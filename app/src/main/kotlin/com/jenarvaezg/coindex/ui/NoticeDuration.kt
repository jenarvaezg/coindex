package com.jenarvaezg.coindex.ui

import androidx.compose.material3.SnackbarDuration

/**
 * How long a notice stays on screen (#435).
 *
 * material3 leaves every `showSnackbar` that carries an `actionLabel` on [SnackbarDuration.Indefinite]
 * unless it is told otherwise, and the download notice was carrying «Abrir». It stayed nailed over the
 * bottom bar — no cross, no swipe — with no way out but tapping Abrir and opening a file nobody asked
 * to see right now. With an action it gets [SnackbarDuration.Long], which is the ten seconds it takes
 * to decide; without one, the short notice of always.
 */
fun noticeDuration(hasAction: Boolean): SnackbarDuration =
    if (hasAction) SnackbarDuration.Long else SnackbarDuration.Short
