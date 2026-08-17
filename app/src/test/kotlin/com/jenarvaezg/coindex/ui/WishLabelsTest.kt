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

    /**
     * The door and the screen it opens share a name, or they read as two features.
     *
     * The pair moved one room in when the shelf window arrived (ADR 0030 §8): «Lo que busco» is now
     * entered from inside «Explorar», so it is that door — the one this label draws — that has to match
     * the screen it opens, and it does. The **index's** door is a different clause and deliberately not
     * this one: ADR 0026 §8 clause 3 has it name what is *behind* it with its count, which is two
     * populations and never a screen title.
     */
    @Test
    fun `the door and the annex it opens share a name`() {
        assertEquals(WishLabels.DESTINATION, screenTitle(Routes.WISHES))
        assertTrue(WishLabels.DESTINATION in wishDoorLabel(7))
        // And the shelf is named by its own word, which is what its masthead prints.
        assertEquals(ShowcaseLabels.DESTINATION, screenTitle(Routes.EXPLORE))
    }

    /**
     * The row draws the first few casillas and counts the rest, and says nothing when it drew them all.
     *
     * The zero is not printed here either (#418's clause): «y 0 más» beside three coins would be a line
     * about the absence of a fourth.
     */
    @Test
    fun `the row counts the marks it had no room to draw`() {
        assertEquals("y 4 más", wishDoorMoreLabel(rest = 4))
        assertEquals("y 1 más", wishDoorMoreLabel(rest = 1))
        assertNull(wishDoorMoreLabel(rest = 0))
        assertNull(wishDoorMoreLabel(rest = -2), "tres dibujadas de dos marcadas no es «y -1 más»")
    }

    /**
     * The row declares that the box above it does not reach it (#515, moved to this row by #520).
     *
     * Its count is of another population — casillas rather than cards — so a search cutting the index to
     * three leaves it where it was, and a number that does not move reads as one that had not been
     * recomputed.
     *
     * **One note and not two**: since #520 the index hangs two annex rows and both count populations the
     * search does not reach, so the sentence is printed on the row at the head — the one the eye crosses
     * right after typing — and `showcaseDoorLabel`'s row at the foot carries nothing. Only while
     * something is typed: the filters persist across launches (ADR 0021 §1), and a line printed on every
     * screen of every session is the frequency ADR 0026 §5 prices.
     */
    @Test
    fun `the row says a search does not reach it`() {
        assertEquals(
            "Lo que escribes arriba no llega hasta aquí.",
            wishDoorNote(searching = true),
        )
        assertNull(wishDoorNote(searching = false))
        // Two sentences about looking, one over the other, would read as being about the same thing.
        assertFalse(WishLabels.DESTINATION in wishDoorNote(searching = true).orEmpty())
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
    fun `the spend is said in Ajustes, named, and in the same unit the gesture promised`() {
        assertEquals("Lo que busco · +14 consultas al mes", wishBudgetLabel(14))
        // The gesture's own sentence, which is the unit the total has to be read in.
        assertTrue("+2 consultas al mes" in WishLabels.MARK_HINT)
        assertTrue(WishLabels.DESTINATION in requireNotNull(wishBudgetLabel(2)))
        // Absent and not «+0»: with nothing marked the pass is the fixed thing it always was.
        assertNull(wishBudgetLabel(0))
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
