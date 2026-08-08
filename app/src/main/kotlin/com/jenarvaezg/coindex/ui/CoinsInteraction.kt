package com.jenarvaezg.coindex.ui

enum class CoinTap { OpenFicha, ToggleSelection }

/** Selection owns the coin tap while it is active; the ficha cannot steal that gesture. */
fun coinTap(picking: Boolean): CoinTap =
    if (picking) CoinTap.ToggleSelection else CoinTap.OpenFicha
