package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The search of both hierarchies (ADR 0021 §1) ignores accents and case: names are written in
 * Spanish but typed on a phone keyboard, so «bolivar» must find «Bolívar».
 */
class FoldingTest {
    @Test
    fun `folding drops the accents and the case`() {
        assertEquals("bolivar de venezuela", fold("Bolívar de Venezuela"))
        assertEquals("ruanda", fold("Ruanda"))
        // Not only Spanish letters: German series names must fold too.
        assertEquals("munzgeschichte", fold("Münzgeschichte"))
        assertEquals("sao tome", fold("São Tomé"))
    }

    @Test
    fun `a query finds an accented name typed without accents`() {
        val haystack = fold("Bolívar de Venezuela · 0,804 oz")

        assertTrue(matchesQuery(haystack, "bolivar"))
        assertTrue(matchesQuery(haystack, "BOLIVAR"))
        assertTrue(matchesQuery(haystack, "Bolívar"))
    }

    @Test
    fun `every word has to appear, in any order`() {
        val haystack = fold("Panda de China, plata")

        assertTrue(matchesQuery(haystack, "panda plata"))
        assertTrue(matchesQuery(haystack, "plata panda"))
        assertFalse(matchesQuery(haystack, "panda oro"))
    }

    @Test
    fun `nothing typed is not a filtered screen`() {
        assertTrue(matchesQuery(fold("cualquier cosa"), ""))
        assertTrue(matchesQuery(fold("cualquier cosa"), "   "))
    }
}
