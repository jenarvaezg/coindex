package com.jenarvaezg.coindex.ui.shelf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * La fila al pie del eje por lámina que lleva a Monedas con «Sin colección» puesto (#634). En
 * `ShelfFixtures` quedan sueltas la Britannia (Reino Unido, una de sus dos piezas) y la moneda sin
 * ficha.
 */
class UnclaimedDoorTest {
    private val rows = coinRows(ShelfFixtures.state)

    @Test
    fun `la puerta cuenta lo mismo que el chip «Sin colección» de Monedas, con y sin país`() {
        val whole = assertNotNull(unclaimedDoor(rows, IndexShelf()))
        val british = assertNotNull(unclaimedDoor(rows, IndexShelf(issuer = "Reino Unido")))

        assertEquals(2, whole.coins)
        assertEquals(1, british.coins)
        for (door in listOf(whole, british)) {
            val chip = coinsFacetCounts(rows, door.shelf, "").membership.of(Membership.InNone)
            assertEquals(chip, door.coins, "la puerta y el chip discrepan con ${door.shelf}")
        }
    }

    @Test
    fun `sólo viaja el país, y el resto del estante del índice no recorta la puerta`() {
        val index = IndexShelf(
            sort = IndexSort.Alphabetical,
            issuer = "Reino Unido",
            weight = OunceBand.OverOne,
            status = PlateStatus.Complete,
        )

        val door = assertNotNull(unclaimedDoor(rows, index))

        assertEquals(CoinsShelf(issuer = "Reino Unido", membership = Membership.InNone), door.shelf)
        assertEquals(1, door.coins)
    }

    /** ADR 0026 §8 cláusula 3. La onza mexicana la reclama su caja. */
    @Test
    fun `a cero no hay puerta`() {
        assertNull(unclaimedDoor(rows, IndexShelf(issuer = "Venezuela")))
        assertNull(unclaimedDoor(rows, IndexShelf(issuer = "México")))
        assertNull(unclaimedDoor(emptyList(), IndexShelf()))
    }
}
