package com.jenarvaezg.coindex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.jenarvaezg.coindex.ui.OPEN_THE_COIN_SHEET

/** The one tap of a casilla or a piece's card, read out the same everywhere (ADR 0026 §3). */
fun Modifier.opensTheCoinSheet(onOpen: () -> Unit): Modifier = clickable(
    role = Role.Button,
    onClickLabel = OPEN_THE_COIN_SHEET,
    onClick = onOpen,
)
