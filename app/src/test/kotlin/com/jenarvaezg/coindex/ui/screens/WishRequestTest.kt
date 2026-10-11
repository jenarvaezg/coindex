package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.DrawnWish
import com.jenarvaezg.coindex.ui.ficha.CasillaOrigin
import com.jenarvaezg.coindex.ui.ficha.CoinFichaRequest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A row of «Lo que busco» opens its coin as its casilla on the plate does (#643). */
class WishRequestTest {
    @Test
    fun `a marked casilla opens on its plate's face, empty, priced by its catalogue facts`() {
        val row = DrawnWish(
            id = "fuertes:1930",
            key = WishKey(typeId = 10_340, year = 1_930, issueId = 4_521),
            typeId = 10_340,
            label = "1930",
            year = "1930",
            plate = "Fuertes",
            printedSide = PrintedSide.Obverse,
            cost = null,
        )

        assertEquals(
            CoinFichaRequest(
                typeId = 10_340,
                side = PrintedSide.Obverse,
                casilla = CasillaOrigin(year = 1_930, issueId = 4_521, missing = true),
            ),
            row.fichaRequest(),
        )
    }
}
