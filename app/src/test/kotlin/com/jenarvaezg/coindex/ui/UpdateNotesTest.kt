package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateNotesTest {
    @Test
    fun `a note that fits carries no hint`() {
        val disclosure = updateNotesDisclosure(expanded = false, truncated = false)

        assertEquals(UPDATE_NOTES_COLLAPSED_LINES, disclosure.maxLines)
        assertNull(disclosure.hint)
    }

    @Test
    fun `a cut note says so, and stays cut until it is opened`() {
        val disclosure = updateNotesDisclosure(expanded = false, truncated = true)

        assertEquals(UPDATE_NOTES_COLLAPSED_LINES, disclosure.maxLines)
        assertEquals("Ver más", disclosure.hint)
    }

    @Test
    fun `an opened note is shown whole, with the way back`() {
        val disclosure = updateNotesDisclosure(expanded = true, truncated = true)

        assertEquals(Int.MAX_VALUE, disclosure.maxLines)
        assertEquals("Ver menos", disclosure.hint)
    }

    /** Unreachable from the UI, which never opens a note that fits; it stays collapsed anyway. */
    @Test
    fun `expanding a note with nothing hidden changes nothing`() {
        val disclosure = updateNotesDisclosure(expanded = true, truncated = false)

        assertEquals(UPDATE_NOTES_COLLAPSED_LINES, disclosure.maxLines)
        assertNull(disclosure.hint)
    }
}
