package com.jenarvaezg.coindex.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What «la escala de animaciones a cero» means for the app (#514): only zero stops motion. */
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
