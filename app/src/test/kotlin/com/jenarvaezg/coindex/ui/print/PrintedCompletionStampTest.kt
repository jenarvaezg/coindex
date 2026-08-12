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
    fun `the printed celebration gives the dynamic ratio typographic air`() {
        assertEquals("22 / 22", printedCompletionRatio("22/22"))
        assertEquals("3 / 3", printedCompletionRatio("3 / 3"))
    }

    /**
     * What the band reserves is the **turned** rectangle and not the one it was drawn as (#476).
     *
     * A 24 × 22 frame tilted 5,5° measures 26,0 × 24,2, and those two millimetres are where the four
     * corners of the caucho live: reserving the frame alone is what let a layer clip them off.
     */
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
