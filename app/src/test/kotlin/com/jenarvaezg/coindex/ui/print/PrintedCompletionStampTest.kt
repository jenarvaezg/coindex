package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The printed completion stamp (#371). It goes on each plate's heading, since a shared folio can
 * hold two complete plates (#232), so it must fit both the 14 mm slim band and the masthead.
 */
class PrintedCompletionStampTest {
    @Test
    fun `the printed celebration gives the dynamic ratio typographic air`() {
        assertEquals("22 / 22", printedCompletionRatio("22/22"))
        assertEquals("3 / 3", printedCompletionRatio("3 / 3"))
    }

    /** The band reserves the tilted rectangle (#476): 24 × 22 turned 5,5° is 26,0 × 24,2. */
    @Test
    fun `the masthead stamp reserves the air its tilt needs and still fits the band`() {
        val size = printedStampSize(PrintHeading.Masthead)

        assertEquals(26.0f, size.width.value, 0.05f)
        assertEquals(24.2f, size.height.value, 0.05f)
        assertTrue(size.height.value < PrintHeading.Masthead.millimetres)
    }

    @Test
    fun `the slim stamp of a shared folio fits inside fourteen millimetres, tilt included`() {
        val size = printedStampSize(PrintHeading.Slim)

        assertEquals(13.0f, size.width.value, 0.05f)
        assertEquals(12.1f, size.height.value, 0.05f)
        assertTrue(size.height.value < PrintHeading.Slim.millimetres)
    }

    @Test
    fun `plain and masthead share the full frame`() {
        assertEquals(
            printedStampSize(PrintHeading.Masthead),
            printedStampSize(PrintHeading.Plain),
        )
    }
}
