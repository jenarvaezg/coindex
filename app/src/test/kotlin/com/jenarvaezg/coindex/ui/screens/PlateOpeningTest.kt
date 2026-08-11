package com.jenarvaezg.coindex.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where a plate opens decides whether the coin has anywhere to land (#396).
 *
 * The nine journeys the father filmed split exactly along this arithmetic: every plate that had to
 * jump was a plate whose coin did not fly in, because the jump used to be an effect that ran a frame
 * after Compose had already looked for the other end of the journey. So the numbers below are not a
 * preference about scrolling — they are the flight, and the phone is where they were read off.
 */
class PlateOpeningTest {
    /** Three columns is the Pixel 7 the whole map was measured on. */
    private val columns = 3

    @Test
    fun `a plate whose coin lands on the first row opens at the top`() {
        // The Lion and the Eagle (1/3), Southern Cross (1/2), The Queen's Beasts (2/11) and The
        // Royal Tudor Beasts (3/9): the four that flew in, and the four that never moved.
        assertEquals(0, plateOpeningItem(landingCell = 0, columns = columns))
        assertEquals(0, plateOpeningItem(landingCell = 1, columns = columns))
        assertEquals(0, plateOpeningItem(landingCell = 2, columns = columns))
    }

    @Test
    fun `a plate whose coin lands further down opens on the casilla, heading counted`() {
        // 100 bolívares (1/4) and Onza Troy (1/4) land on casilla 4; 5 Reichsmark (1/6) and Nautical
        // Ounce de Ruanda (2/10) on casilla 6. The heading is one item ahead of every casilla.
        assertEquals(4, plateOpeningItem(landingCell = 3, columns = columns))
        assertEquals(6, plateOpeningItem(landingCell = 5, columns = columns))
    }

    /** The four Bolívares the father owns are casillas 19 to 22 of 22 (#304). */
    @Test
    fun `the far end of a long date run is what the sheet opens on`() {
        assertEquals(20, plateOpeningItem(landingCell = 19, columns = columns))
    }

    /** A plate the collector owns nothing official of has no coin in the air to serve. */
    @Test
    fun `a plate with no landing opens at the top`() {
        assertEquals(0, plateOpeningItem(landingCell = null, columns = columns))
    }

    /** A wider screen puts more casillas on the first row, and then fewer plates need to jump. */
    @Test
    fun `the first row is as wide as the screen makes it`() {
        assertEquals(0, plateOpeningItem(landingCell = 3, columns = 4))
        assertEquals(4, plateOpeningItem(landingCell = 3, columns = 2))
        assertEquals(2, plateOpeningItem(landingCell = 1, columns = 1))
    }
}
