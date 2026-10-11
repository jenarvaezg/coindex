package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.Routes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The coin's screen is a destination, so what it was opened with travels in its route (#643). */
class CoinFichaRouteTest {
    @Test
    fun `a casilla's request survives the route`() {
        val request = CoinFichaRequest(
            typeId = 10_340,
            side = PrintedSide.Obverse,
            casilla = CasillaOrigin(year = 1929, issueId = 4_521, missing = true, dated = true),
        )

        assertEquals(request, read(Routes.coin(request)))
    }

    @Test
    fun `a request from monedas carries no casilla`() {
        val request = CoinFichaRequest(typeId = 401_375)

        assertEquals("coin/401375?side=reverse", Routes.coin(request))
        assertEquals(request, read(Routes.coin(request)))
    }

    @Test
    fun `a casilla with no year or issue keeps the rest`() {
        val request = CoinFichaRequest(
            typeId = 578_835,
            side = PrintedSide.Reverse,
            casilla = CasillaOrigin(year = null, missing = false, dated = false),
        )

        assertEquals(request, read(Routes.coin(request)))
    }

    @Test
    fun `a route without a type id opens nothing`() {
        assertNull(coinFichaRequestOf(null, "reverse", null, null, null, null))
        assertNull(coinFichaRequestOf("no", "reverse", null, null, null, null))
    }

    @Test
    fun `an unknown side falls back to the reverse`() {
        val request = coinFichaRequestOf("7", "canto", null, null, null, null)

        assertEquals(PrintedSide.Reverse, request?.side)
    }

    /** What `NavHost` hands the destination: each query parameter as a string, absent as null. */
    private fun read(route: String): CoinFichaRequest? {
        val typeId = route.substringAfter("coin/").substringBefore('?')
        val query = route.substringAfter('?', "").split('&').filter(String::isNotEmpty)
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        return coinFichaRequestOf(
            typeId = typeId,
            side = query["side"],
            year = query["year"],
            issue = query["issue"],
            missing = query["missing"],
            dated = query["dated"],
        )
    }
}
