package com.jenarvaezg.coindex.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What «la escala de animaciones a cero» means for the app (#514).
 *
 * Zero is the only value that stops anything: the boundary is a boundary and not a curve, so the
 * 10× a measuring session puts there is movement like any other, and so is the 0.5× of somebody who
 * likes his phone brisk.
 */
class MotionTest {
    @Test
    fun `a device that was never told otherwise moves`() {
        assertTrue(movesAt(1f))
    }

    @Test
    fun `zero is the one value that means stop`() {
        assertFalse(movesAt(0f))
    }

    @Test
    fun `a stretched clock is still a clock`() {
        assertTrue(movesAt(10f))
        assertTrue(movesAt(0.5f))
    }
}
