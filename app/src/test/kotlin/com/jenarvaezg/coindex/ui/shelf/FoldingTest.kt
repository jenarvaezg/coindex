package com.jenarvaezg.coindex.ui.shelf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The search box of both hierarchies (ADR 0021 §1).
 *
 * The measured case is «bolivar» finding «Bolívar»: the corpus is written in Spanish by rule and
 * typed on a phone keyboard, so a search that respected accents would answer «0 de 58» to a query
 * about a collection the collector is looking at.
 */
class FoldingTest {
    @Test
    fun `folding drops the accents and the case`() {
        assertEquals("bolivar de venezuela", fold("Bolívar de Venezuela"))
        assertEquals("ruanda", fold("Ruanda"))
        // Not a table of Spanish letters: the first German series has to work too.
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
