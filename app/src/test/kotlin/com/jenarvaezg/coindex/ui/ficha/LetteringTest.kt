package com.jenarvaezg.coindex.ui.ficha

import kotlin.test.Test
import kotlin.test.assertEquals

/** Neither Bitter nor Barlow has `↊` or `↋`, so the digit is drawn turned over instead (#643). */
class LetteringTest {
    @Test
    fun `the national police two euros' edge turns its twos`() {
        assertEquals(
            listOf(
                LetteringRun("2 ★ ★ "),
                LetteringRun("2", turned = true),
                LetteringRun(" ★ ★ "),
                LetteringRun("3", turned = true),
            ),
            letteringRuns("2 ★ ★ ↊ ★ ★ ↋"),
        )
    }

    @Test
    fun `a legend with no turned digit is one run`() {
        assertEquals(listOf(LetteringRun("2 EURO")), letteringRuns("2 EURO"))
        assertEquals(emptyList(), letteringRuns(""))
    }
}
