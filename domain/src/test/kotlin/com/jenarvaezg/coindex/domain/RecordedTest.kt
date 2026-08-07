package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The two ways Numista says «nobody filled this in» without leaving the field out.
 *
 * These used to be a `takeIf` inside a JSON extractor, where «a diameter of zero is not a
 * diameter» read as a parsing detail instead of as the claim about the catalogue that it is
 * (#221).
 */
class RecordedTest {
    @Test
    fun `a blank field is nothing written`() {
        assertNull(recordedText(null))
        assertNull(recordedText(""))
        assertNull(recordedText("   "))
        assertEquals("Australia", recordedText("Australia"))
    }

    @Test
    fun `a diameter of zero is a field nobody filled in`() {
        assertNull(recordedDiameter(null))
        assertNull(recordedDiameter(0.0))
        assertNull(recordedDiameter(-1.0))
        assertEquals(40.6, recordedDiameter(40.6))
    }
}
