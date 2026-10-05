package com.jenarvaezg.coindex.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * With animations off (#514) the sheet gets no transition object at all rather than one of zero
 * duration, which could still leak a frame of a half-arrived sheet.
 */
class CoinSheetTransitionTest {
    @Test
    fun `a sheet asked for quiet has no entrance and no exit`() {
        assertSame(EnterTransition.None, sheetEnter(moving = false))
        assertSame(ExitTransition.None, sheetExit(moving = false))
    }

    @Test
    fun `a sheet nobody quietened rises and fades as it always did`() {
        assertNotSame(EnterTransition.None, sheetEnter(moving = true))
        assertNotSame(ExitTransition.None, sheetExit(moving = true))
    }
}
