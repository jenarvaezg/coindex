package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What «Lo que busco» says, and the three promises its copy makes (ADR 0029, ADR 0026 §5, §6).
 *
 * The door, the masthead and the paper all print the destination's name, and the mark is the same two
 * words on the phone and on the page. Those pairings are what this pins: each of them is a string
 * somebody could reasonably write twice.
 */
class WishLabelsTest {
    /**
     * The door names what is behind it with its count, and the arrow is not in the string.
     *
     * ADR 0026 §8 writes the door as «Lo que busco · 7 →», and the arrow is drawn (`ForwardGlyph`)
     * because neither Bitter nor Barlow has that glyph (#298): typed into the label it would come out
     * in whatever the system substitutes, in the middle of the album's own type.
     */
    @Test
    fun `the door prints the name and the count and no arrow`() {
        assertEquals("Lo que busco · 7", wishDoorLabel(7))
        assertEquals("Lo que busco · 1", wishDoorLabel(1))
        assertFalse('→' in wishDoorLabel(7), "la flecha se dibuja, no se teclea")
    }

    /** The door and the screen it opens share a name, or they read as two features. */
    @Test
    fun `the door and the annex it opens share a name`() {
        assertEquals(WishLabels.DESTINATION, screenTitle(Routes.EXPLORE))
        assertTrue(WishLabels.DESTINATION in wishDoorLabel(7))
    }

    /** Two units, because the list is what crosses plates; casillas first, which is what was marked. */
    @Test
    fun `the census counts casillas and the plates they came from`() {
        assertEquals("7 casillas en 5 láminas", wishCensusLabel(slots = 7, plates = 5))
        assertEquals("1 casilla en 1 lámina", wishCensusLabel(slots = 1, plates = 1))
    }

    /**
     * The spend is a monthly ceiling and it is said as one, in the gesture and in Ajustes.
     *
     * «+2» is what the mode promises per casilla, so the total has to be spoken in the same unit: a
     * figure that said «14 consultas» without the «+» would read as the whole of the pass rather than
     * as what the marks add to it.
     */
    @Test
    fun `the spend is said as what it adds to the month`() {
        assertEquals("+14 consultas al mes", wishSpendLabel(14))
        assertTrue("+2 consultas al mes" in WishLabels.MARK_HINT)
        // Absent and not «+0»: with nothing marked the pass is the fixed thing it always was, and the
        // rule about the zero is written once, where the amount is worded.
        assertNull(wishSpendLabel(0))
        assertNull(wishBudgetLabel(0))
    }

    /**
     * In Ajustes the same figure carries its subject, which is what ADR 0029 §5 asked for.
     *
     * On the annex the screen is the subject and the amount can stand alone. On the valuation's card
     * nothing else is about the marks, so «+2 consultas al mes» under a sentence about the pass is a
     * number nobody can attribute — and the two readings are one string wide apart, not two wordings.
     */
    @Test
    fun `in Ajustes the spend is named`() {
        assertEquals("Lo que busco · +2 consultas al mes", wishBudgetLabel(2))
        assertTrue(requireNotNull(wishSpendLabel(2)) in requireNotNull(wishBudgetLabel(2)))
    }

    /**
     * The mode's two words, and the one thing they may not do: name the destination.
     *
     * «Marcar lo que busco» opens the gesture on a plate and «Lo que busco» is a screen; the mode is
     * named after what it does — the verb is the whole difference, and it is why the door of the annex
     * cannot be reused as the door of the mode.
     */
    @Test
    fun `the marking mode is named by its verb`() {
        assertEquals("Marcar lo que busco", WishLabels.MARK_ACTION)
        assertTrue(WishLabels.MARK_ACTION.startsWith("Marcar"))
        // The destination's own words, lower-cased mid-sentence: the mode is «marcar» plus what the
        // annex is called, so the two cannot come to be about different things.
        assertTrue(WishLabels.DESTINATION.lowercase() in WishLabels.MARK_ACTION.lowercase())
    }

    /**
     * The mark itself is one string, because it is one mark: the chip in the hole and the line the
     * printed casilla carries (ADR 0026 §4). Written in lower case, like the note it is.
     */
    @Test
    fun `the mark is two words in lower case, and the same on paper`() {
        assertEquals("lo busco", WishLabels.MARK_WORD)
        assertEquals(WishLabels.MARK_WORD, WishLabels.MARK_WORD.lowercase())
    }

    /**
     * The empty screen says where the marks are made, because there is nowhere else to find out.
     *
     * It is reachable empty by exactly one route — «Quitar» on the last row — since the door is not
     * printed at zero, so a sentence that only said «no hay nada» would be a dead end.
     */
    @Test
    fun `the empty annex says where a casilla is marked`() {
        assertTrue("lámina" in WishLabels.EMPTY_EXPLANATION)
        assertTrue(WishLabels.REMOVE_ACTION == "Quitar")
    }
}
