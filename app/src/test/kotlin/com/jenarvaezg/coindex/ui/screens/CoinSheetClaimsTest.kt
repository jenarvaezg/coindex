package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val LAMINA = CoinClaim("1 Bolívar", CardDestination.Plate("venezuela-bolivar"))
private val BOX = CoinClaim("Las venezolanas", CardDestination.Box(7))

/** The sheet names every collection that claims its coin, the one it was opened from too (#635). */
class CoinSheetClaimsTest {
    @Test
    fun `opened from Monedas, every collection is a link in index order`() {
        assertEquals(
            listOf(SheetClaim(LAMINA, here = false), SheetClaim(BOX, here = false)),
            sheetClaims(listOf(LAMINA, BOX), here = null),
        )
    }

    @Test
    fun `opened from a lámina, it is named in its place but not linked`() {
        assertEquals(
            listOf(SheetClaim(BOX, here = false), SheetClaim(LAMINA, here = true)),
            sheetClaims(listOf(BOX, LAMINA), here = LAMINA.destination),
        )
    }

    @Test
    fun `a coin only the open lámina claims still names it`() {
        assertEquals(
            listOf(SheetClaim(LAMINA, here = true)),
            sheetClaims(listOf(LAMINA), here = LAMINA.destination),
        )
    }

    @Test
    fun `the heading counts the open lámina too`() {
        assertEquals(
            "En esta colección",
            sheetClaimsHeading(sheetClaims(listOf(LAMINA), LAMINA.destination)),
        )
        assertEquals(
            "En estas colecciones",
            sheetClaimsHeading(sheetClaims(listOf(LAMINA, BOX), LAMINA.destination)),
        )
    }

    /** ADR 0026 §13. A hole is one, even opened from its own lámina. */
    @Test
    fun `a coin no collection claims has no heading`() {
        assertNull(sheetClaimsHeading(sheetClaims(emptyList(), LAMINA.destination)))
    }
}
