package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Copy of «Lo que busco» (ADR 0029, ADR 0026 §5, §6): the strings that the door, the masthead, the
 * paper and the mark share, each of which someone could write twice.
 */
class WishLabelsTest {
    /** The arrow is drawn (`ForwardGlyph`): neither Bitter nor Barlow has the glyph (#298). */
    @Test
    fun `the door prints the name and the count and no arrow`() {
        assertEquals("Lo que busco · 7", wishDoorLabel(7))
        assertEquals("Lo que busco · 1", wishDoorLabel(1))
        assertFalse('→' in wishDoorLabel(7), "la flecha se dibuja, no se teclea")
    }

    /**
     * Otherwise they read as two features. This door lives inside «Explorar» (ADR 0030 §8); the
     * index's door follows ADR 0026 §8 clause 3 instead and is not a screen title.
     */
    @Test
    fun `the door and the annex it opens share a name`() {
        assertEquals(WishLabels.DESTINATION, screenTitle(Routes.WISHES))
        assertTrue(WishLabels.DESTINATION in wishDoorLabel(7))
        // And the shelf is named by its own word, which is what its masthead prints.
        assertEquals(ShowcaseLabels.DESTINATION, screenTitle(Routes.EXPLORE))
    }

    /** Nothing when every casilla was drawn: the zero is not printed (#418). */
    @Test
    fun `the row counts the marks it had no room to draw`() {
        assertEquals("y 4 más", wishDoorMoreLabel(rest = 4))
        assertEquals("y 1 más", wishDoorMoreLabel(rest = 1))
        assertNull(wishDoorMoreLabel(rest = 0))
        assertNull(wishDoorMoreLabel(rest = -2), "tres dibujadas de dos marcadas no es «y -1 más»")
    }

    /**
     * The row counts casillas, not cards, so a search leaves its number alone (#515). Only this
     * head row carries the note, not `showcaseDoorLabel`'s (#520), and only while something is
     * typed: filters persist across launches (ADR 0021 §1) and a line on every screen breaks
     * ADR 0026 §5.
     */
    @Test
    fun `the row says a search does not reach it`() {
        assertEquals(
            "Lo que escribes arriba no llega hasta aquí.",
            wishDoorNote(searching = true),
        )
        assertNull(wishDoorNote(searching = false))
        // Two sentences about looking, one over the other, would read as the same thing.
        assertFalse(WishLabels.DESTINATION in wishDoorNote(searching = true).orEmpty())
    }

    /** Casillas first, because they are what was marked. */
    @Test
    fun `the census counts casillas and the plates they came from`() {
        assertEquals("7 casillas en 5 láminas", wishCensusLabel(slots = 7, plates = 5))
        assertEquals("1 casilla en 1 lámina", wishCensusLabel(slots = 1, plates = 1))
    }

    /** Without the «+», the figure would read as the whole pass instead of what the marks add. */
    @Test
    fun `the spend is said in Ajustes, named, and in the same unit the gesture promised`() {
        assertEquals("Lo que busco · +14 consultas al mes", wishBudgetLabel(14))
        // The gesture's own sentence sets the unit.
        assertTrue("+2 consultas al mes" in WishLabels.MARK_HINT)
        assertTrue(WishLabels.DESTINATION in requireNotNull(wishBudgetLabel(2)))
        // Absent rather than «+0» when nothing is marked.
        assertNull(wishBudgetLabel(0))
    }

    /** The verb tells the gesture on a plate apart from the screen, so the door can't be reused. */
    @Test
    fun `the marking mode is named by its verb`() {
        assertEquals("Marcar lo que busco", WishLabels.MARK_ACTION)
        assertTrue(WishLabels.MARK_ACTION.startsWith("Marcar"))
        // «Marcar» plus the annex's name, lower-cased, so the two can't drift apart.
        assertTrue(WishLabels.DESTINATION.lowercase() in WishLabels.MARK_ACTION.lowercase())
    }

    /** One string for the chip in the hole and the printed casilla's line (ADR 0026 §4). */
    @Test
    fun `the mark is two words in lower case, and the same on paper`() {
        assertEquals("lo busco", WishLabels.MARK_WORD)
        assertEquals(WishLabels.MARK_WORD, WishLabels.MARK_WORD.lowercase())
    }

    /** It is only reached empty through «Quitar» on the last row, since the door hides at zero. */
    @Test
    fun `the empty annex says where a casilla is marked`() {
        assertTrue("lámina" in WishLabels.EMPTY_EXPLANATION)
        assertTrue(WishLabels.REMOVE_ACTION == "Quitar")
    }
}
