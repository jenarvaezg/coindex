package com.jenarvaezg.coindex.ui

import androidx.compose.material3.SnackbarDuration

/**
 * How long a notice stays on screen (#435). material3 defaults a snackbar with an action to
 * [SnackbarDuration.Indefinite], which left the download notice stuck over the bottom bar until
 * «Abrir» was tapped.
 */
fun noticeDuration(hasAction: Boolean): SnackbarDuration =
    if (hasAction) SnackbarDuration.Long else SnackbarDuration.Short
