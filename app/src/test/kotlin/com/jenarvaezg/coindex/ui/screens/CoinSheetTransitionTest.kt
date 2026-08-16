package com.jenarvaezg.coindex.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * The sheet is the ceremony the audit of 14 August 2026 caught in the air (#514), so it is the one
 * written down here: with the system asking for quiet there is **no transition object at all**, and
 * not one of duration zero. The two read the same on a stopwatch and only the first cannot leak a
 * frame of a half-arrived sheet.
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
