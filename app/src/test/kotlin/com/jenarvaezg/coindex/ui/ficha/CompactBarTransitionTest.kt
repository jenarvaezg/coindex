package com.jenarvaezg.coindex.ui.ficha

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * With animations off (#514) the compact bar gets no transition object at all rather than one of
 * zero duration, which could still leak a frame of a half-arrived bar.
 */
class CompactBarTransitionTest {
    @Test
    fun `a bar asked for quiet has no entrance and no exit`() {
        assertSame(EnterTransition.None, compactBarEnter(moving = false))
        assertSame(ExitTransition.None, compactBarExit(moving = false))
    }

    @Test
    fun `a bar nobody quietened slides in and out`() {
        assertNotSame(EnterTransition.None, compactBarEnter(moving = true))
        assertNotSame(ExitTransition.None, compactBarExit(moving = true))
    }
}
