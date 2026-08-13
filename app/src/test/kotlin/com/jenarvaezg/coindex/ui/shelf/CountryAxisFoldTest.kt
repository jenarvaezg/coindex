package com.jenarvaezg.coindex.ui.shelf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The fold of a country block (#417): coins first, one row of absences, the rest behind «… y
 * faltan N».
 *
 * The numbers are the father's own sheet, measured on the HTML mock-up at phone size: Venezuela
 * 42/115 in seven-hole rows, Sudáfrica 2/9 whole, Portugal 38/54 in between.
 */
class CountryAxisFoldTest {
    @Test
    fun `Venezuela paints its coins, one row of absences, and folds the rest`() {
        val fold = block(owned = 42, issued = 115).fold(columns = 7)

        assertEquals(49, fold.cells.size)
        assertEquals(66, fold.foldable)
        assertEquals(42, fold.cells.count { it is CountryAxisCell.Slot && it.owned })
        assertEquals(7, fold.cells.count { it is CountryAxisCell.Slot && !it.owned })
    }

    @Test
    fun `the coins come first, so the absences can be summarised at all`() {
        val fold = block(owned = 3, issued = 30, ownedAt = { it % 10 == 0 }).fold(columns = 7)

        val owned = fold.cells.map { it is CountryAxisCell.Slot && it.owned }
        assertEquals(listOf(true, true, true), owned.take(3))
        assertTrue(owned.drop(3).none { it })
    }

    @Test
    fun `Sudáfrica 2-9 prints whole because the fold would hide less than a row`() {
        val fold = block(owned = 2, issued = 9).fold(columns = 7)

        assertEquals(9, fold.cells.size)
        assertEquals(0, fold.foldable)
    }

    @Test
    fun `a country with exactly one row of absences keeps them all`() {
        val fold = block(owned = 0, issued = 7).fold(columns = 7)

        assertEquals(7, fold.cells.size)
        assertEquals(0, fold.foldable)
    }

    @Test
    fun `an open fold paints everything and still counts what closing it would hide`() {
        val fold = block(owned = 42, issued = 115).fold(columns = 7, expanded = true)

        assertEquals(115, fold.cells.size)
        assertEquals(66, fold.foldable)
    }

    @Test
    fun `a wider block folds later, because the sample row is wider too`() {
        val phone = block(owned = 1, issued = 20).fold(columns = 7)
        val tablet = block(owned = 1, issued = 20).fold(columns = 15)

        assertEquals(12, phone.foldable)
        assertEquals(4, tablet.foldable)
        // And what fitted a phone in two rows fits a tablet in one, so nothing folds at all.
        assertEquals(0, block(owned = 1, issued = 16).fold(columns = 15).foldable)
        assertEquals(16, block(owned = 1, issued = 16).fold(columns = 15).cells.size)
    }

    @Test
    fun `a block measured before layout paints whole rather than hiding everything`() {
        val fold = block(owned = 1, issued = 20).fold(columns = 0)

        assertEquals(20, fold.cells.size)
        assertEquals(0, fold.foldable)
    }

    @Test
    fun `a loose piece travels with the coins, not with the absences`() {
        val cells = listOf(
            CountryAxisCell.Slot("c", "m1", typeId = 1, owned = false, quantity = 0),
            CountryAxisCell.Loose(itemId = 9, typeId = 2, quantity = 1),
        ) + (2..19).map {
            CountryAxisCell.Slot("c", "m$it", typeId = it, owned = false, quantity = 0)
        }
        val fold = CountryAxisBlock(country = "Francia", owned = 1, issued = 20, cells = cells)
            .fold(columns = 7)

        assertTrue(fold.cells.first() is CountryAxisCell.Loose)
        assertEquals(8, fold.cells.size)
        assertEquals(12, fold.foldable)
    }

    @Test
    fun `the fold says the number, and names the way back when it is open`() {
        assertEquals("… y faltan 66", countryAxisFoldLabel(hidden = 66, expanded = false))
        assertEquals("… y falta 1", countryAxisFoldLabel(hidden = 1, expanded = false))
        assertEquals("Plegar las 66", countryAxisFoldLabel(66, expanded = true))
        assertEquals("Plegar la que falta", countryAxisFoldLabel(1, expanded = true))
        assertEquals(
            "Ver 66 casillas que faltan",
            countryAxisFoldAction(hidden = 66, expanded = false),
        )
        assertEquals("Ver 1 casilla que falta", countryAxisFoldAction(hidden = 1, expanded = false))
    }

    private fun block(
        owned: Int,
        issued: Int,
        ownedAt: ((Int) -> Boolean)? = null,
    ): CountryAxisBlock {
        var left = owned
        val cells = (0 until issued).map { index ->
            val mine = if (ownedAt != null) {
                ownedAt(index) && left-- > 0
            } else {
                index < owned
            }
            CountryAxisCell.Slot(
                catalogId = "c",
                memberId = "m$index",
                typeId = index,
                owned = mine,
                quantity = if (mine) 1 else 0,
            )
        }
        return CountryAxisBlock(country = "País", owned = owned, issued = issued, cells = cells)
    }
}
