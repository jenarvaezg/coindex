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

    /**
     * No font on the phone has U+1F7CC, the six-pointed star of the 100 pesetas of Franco
     * (N#1885); the Dingbats one is in Noto Sans Symbols.
     */
    @Test
    fun `a star no font has is written as the nearest one that has it`() {
        val star = "\uD83D\uDFCC"

        assertEquals(listOf(LetteringRun("✶ 1966 ✶")), letteringRuns("$star 1966 $star"))
    }

    @Test
    fun `a summary reads the legend flat, its digits upright`() {
        assertEquals("2 ★ 2 ✶", flatLegend("2 ★ ↊ \uD83D\uDFCC"))
    }
}
