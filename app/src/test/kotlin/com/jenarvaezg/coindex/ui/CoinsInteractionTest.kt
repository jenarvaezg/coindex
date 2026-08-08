package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CoinsInteractionTest {
    @Test
    fun `a coin tap opens its ficha except while a box selection is active`() {
        assertEquals(CoinTap.OpenFicha, coinTap(picking = false))
        assertEquals(CoinTap.ToggleSelection, coinTap(picking = true))
    }
}
