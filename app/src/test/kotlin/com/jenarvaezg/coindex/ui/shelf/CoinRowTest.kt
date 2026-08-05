package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.matchesQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The other hierarchy: a coin exists whether or not any collection claims it (ADR 0021 §1). */
class CoinRowTest {
    private val rows = coinRows(ShelfFixtures.state)

    @Test
    fun `a coin held twice is one coin, not two receipts`() {
        val fuerte = rows.single { it.typeId == ShelfFixtures.FUERTE }

        assertEquals(3, fuerte.quantity)
        assertEquals(5, ShelfFixtures.state.items.size)
        assertEquals(4, rows.size)
    }

    @Test
    fun `every coin appears, including the one no collection claims`() {
        assertEquals(
            listOf(
                ShelfFixtures.ONZA_MEXICANA,
                ShelfFixtures.BRITANNIA,
                ShelfFixtures.FUERTE,
                ShelfFixtures.UNCACHED,
            ),
            rows.map { it.typeId },
        )
    }

    @Test
    fun `a coin links back to the collections that claim it`() {
        val medal = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }
        val orphan = rows.single { it.typeId == ShelfFixtures.UNCACHED }

        assertEquals(listOf("Las mexicanas"), medal.claims.map { it.name })
        assertEquals(CardDestination.Box(7), medal.claims.single().destination)
        assertTrue(orphan.claims.isEmpty())
    }

    @Test
    fun `an uncached type says what it can and guesses nothing`() {
        val orphan = rows.single { it.typeId == ShelfFixtures.UNCACHED }

        assertNull(orphan.issuer)
        assertNull(orphan.year)
        assertNull(orphan.weightOz)
        // Money is the default with two chips and no third place to put it.
        assertEquals(ObjectClass.Coin, orphan.objectClass)
        assertEquals("Pieza 12", orphan.title)
    }

    @Test
    fun `a medal inside a collection is a medal and stays in its collection`() {
        val medal = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }

        assertEquals(ObjectClass.Exonumia, medal.objectClass)
        assertEquals(listOf("Las mexicanas"), medal.claims.map { it.name })
    }

    /**
     * Dropping a type from a box does not touch the coin (ADR 0013, ADR 0021 §10).
     *
     * It stays in the inventory and in Coins with the same quantity, and what changes is only which
     * collections claim it — which is what makes a box a second membership rather than a move. Here
     * the box was the onza's only claim, so it lands under «Sin colección» and nowhere else.
     */
    @Test
    fun `a coin dropped from a box is still a coin, with one claim fewer`() {
        val after = coinRows(ShelfFixtures.stateWithoutTheBox)
        val before = rows.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }
        val onza = after.single { it.typeId == ShelfFixtures.ONZA_MEXICANA }

        assertEquals(rows.size, after.size)
        assertEquals(before.quantity, onza.quantity)
        assertEquals(before.title, onza.title)
        assertTrue(onza.claims.isEmpty())
    }

    @Test
    fun `the search box reaches the name, the country and the Numista number`() {
        val fuerte = rows.single { it.typeId == ShelfFixtures.FUERTE }

        assertTrue(matchesQuery(fuerte.haystack, "bolivar"))
        assertTrue(matchesQuery(fuerte.haystack, "venezuela"))
        assertTrue(matchesQuery(fuerte.haystack, "100"))
    }
}
