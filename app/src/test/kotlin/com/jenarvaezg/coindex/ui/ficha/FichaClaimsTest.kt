package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.shelf.CoinClaim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val LAMINA = CoinClaim("1 Bolívar", CardDestination.Plate("venezuela-bolivar"))
private val BOX = CoinClaim("Las venezolanas", CardDestination.Box(7))

/** The coin's screen names every collection that claims it, the one it came from too (#635). */
class FichaClaimsTest {
    @Test
    fun `opened from Monedas, every collection is a link in index order`() {
        assertEquals(
            listOf(FichaClaim(LAMINA, here = false), FichaClaim(BOX, here = false)),
            fichaClaims(listOf(LAMINA, BOX), here = null),
        )
    }

    @Test
    fun `opened from a lámina, it is named in its place but not linked`() {
        assertEquals(
            listOf(FichaClaim(BOX, here = false), FichaClaim(LAMINA, here = true)),
            fichaClaims(listOf(BOX, LAMINA), here = LAMINA.destination),
        )
    }

    @Test
    fun `a coin only the open lámina claims still names it`() {
        assertEquals(
            listOf(FichaClaim(LAMINA, here = true)),
            fichaClaims(listOf(LAMINA), here = LAMINA.destination),
        )
    }

    @Test
    fun `the heading counts the open lámina too`() {
        assertEquals(
            "En esta colección",
            fichaClaimsHeading(fichaClaims(listOf(LAMINA), LAMINA.destination)),
        )
        assertEquals(
            "En estas colecciones",
            fichaClaimsHeading(fichaClaims(listOf(LAMINA, BOX), LAMINA.destination)),
        )
    }

    /** ADR 0026 §13. A hole opened from its lámina is named by the screen, not here (#643). */
    @Test
    fun `a coin no collection claims has no heading`() {
        assertNull(fichaClaimsHeading(fichaClaims(emptyList(), LAMINA.destination)))
    }
}
