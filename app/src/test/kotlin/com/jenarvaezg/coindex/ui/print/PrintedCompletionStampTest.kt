package com.jenarvaezg.coindex.ui.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Where the notebook stamp fits, which is the decision #371 had to make before drawing anything.
 *
 * A shared folio (#232) can carry two complete plates, so the caucho lands on each plate's own
 * heading and not once on the page. The slim band is fourteen millimetres; the masthead is forty —
 * both frames have to fit the band that hosts them, or the ink is clipped by the heading itself.
 */
class PrintedCompletionStampTest {
    @Test
    fun `the masthead stamp fits inside the forty-millimetre band`() {
        val size = printedStampSize(PrintHeading.Masthead)

        assertEquals(24f, size.width.value)
        assertEquals(22f, size.height.value)
        assertTrue(size.height.value < PrintHeading.Masthead.millimetres)
    }

    @Test
    fun `the slim stamp of a shared folio fits inside fourteen millimetres`() {
        val size = printedStampSize(PrintHeading.Slim)

        assertEquals(12f, size.width.value)
        assertEquals(11f, size.height.value)
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
