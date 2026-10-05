package com.jenarvaezg.coindex.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where a plate opens decides whether the coin has anywhere to land (#396): the landing casilla
 * must already be on screen when Compose looks for the journey's other end.
 */
class PlateOpeningTest {
    /** Three columns: the Pixel 7 the map was measured on. */
    private val columns = 3

    @Test
    fun `a plate whose coin lands on the first row opens at the top`() {
        assertEquals(0, plateOpeningItem(landingCell = 0, columns = columns))
        assertEquals(0, plateOpeningItem(landingCell = 1, columns = columns))
        assertEquals(0, plateOpeningItem(landingCell = 2, columns = columns))
    }

    @Test
    fun `a plate whose coin lands further down opens on the casilla, heading counted`() {
        // The heading is one item ahead of every casilla.
        assertEquals(4, plateOpeningItem(landingCell = 3, columns = columns))
        assertEquals(6, plateOpeningItem(landingCell = 5, columns = columns))
    }

    /** Owned coins at the end of a 22-casilla run, as in #304. */
    @Test
    fun `the far end of a long date run is what the sheet opens on`() {
        assertEquals(20, plateOpeningItem(landingCell = 19, columns = columns))
    }

    /** With nothing owned on the plate, no coin is in flight. */
    @Test
    fun `a plate with no landing opens at the top`() {
        assertEquals(0, plateOpeningItem(landingCell = null, columns = columns))
    }

    @Test
    fun `the first row is as wide as the screen makes it`() {
        assertEquals(0, plateOpeningItem(landingCell = 3, columns = 4))
        assertEquals(4, plateOpeningItem(landingCell = 3, columns = 2))
        assertEquals(2, plateOpeningItem(landingCell = 1, columns = 1))
    }
}
