package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.ui.shelf.ANY_FILTER
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One string, one owner (ADR 0026 §5).
 *
 * `CopyLivesInOnePlaceTest` defends **where** copy is written; it counts nothing and cannot tell one
 * wording from four. This is the other half: the handful of places where a string was said more than
 * once, and where saying it twice again would go unnoticed because both copies would be in a copy
 * file and both would be green.
 *
 * It does not test every formatter in the copy files. What it holds are the pruning's own promises,
 * and each `assertEquals` here is a sentence somebody could reintroduce.
 *
 * **It counts nothing.** An `assertTrue(words <= 8)` was written here first and taken back out: the
 * bar is not a test (§6), and a word count is exactly the shape §6 refused — it would go red at a
 * better rewording and green at a worse one of the same length. What the sentences are pinned by is
 * their own text, which a reviewer reads in the diff.
 */
class PrunedVocabularyTest {
    /**
     * Four wordings became one, and the one is the short one.
     *
     * 13, 16, 8 and 32 words in three files, for one event. The three surfaces that say it now say
     * it identically — a route with nothing behind it, a derived collection whose last piece went,
     * and a plate whose variant no longer describes anything.
     */
    @Test
    fun `a collection that is gone is said the same way wherever it is said`() {
        assertEquals("Esta colección ya no existe. Vuelve al índice.", COLLECTION_NO_LONGER_EXISTS)
        assertEquals(
            COLLECTION_NO_LONGER_EXISTS,
            plateUnavailableLabel(PlateUnavailable.NotACollection),
        )
    }

    /**
     * An empty box is **not** the sentence above: it survives with nothing in it, because it is the
     * one thing the collector typed (ADR 0021 §11), and it says so.
     */
    @Test
    fun `an empty box keeps a sentence of its own`() {
        assertTrue(EMPTY_BOX_EXPLANATION != COLLECTION_NO_LONGER_EXISTS)
        assertTrue("Sigue aquí" in EMPTY_BOX_EXPLANATION)
    }

    /**
     * Ten chips said six words; they say one.
     *
     * `Todos`, `Todas`, `Todo`, `Cualquier año` and `Da igual` each agreed with the noun of the facet
     * they were written next to, and read as a shelf they were six ways of saying nothing is
     * narrowed. There is no way to assert the ten call sites from here — the chips are drawn by two
     * screens — so what is held is the word itself and that it agrees with nothing.
     */
    @Test
    fun `the no-filter chip is one word that agrees with no facet`() {
        assertEquals("Cualquiera", ANY_FILTER)
    }

    /**
     * The credential promise is one string read on two screens.
     *
     * Never on screen together, so it saves no words on either one; what it saves is the pair
     * drifting apart, which is what happens to a promise about encryption written twice. Both halves
     * of it have to survive: what is stored, and that it does not leave the phone.
     */
    @Test
    fun `the credential promise says both of its halves once`() {
        assertTrue("cifrados" in CREDENTIALS_EXPLANATION)
        assertTrue("nunca salen de él" in CREDENTIALS_EXPLANATION)
        assertTrue("API key" in CREDENTIALS_EXPLANATION)
    }

    /**
     * Signing out is a word on a button and not also a title above it (§5).
     *
     * The explanation under it may not repeat the button, because the card is three lines and one of
     * them would be the same two words twice.
     */
    @Test
    fun `signing out is said once, on the control that does it`() {
        assertEquals("Cerrar sesión", SIGN_OUT_ACTION)
        assertTrue(SIGN_OUT_ACTION !in SIGN_OUT_EXPLANATION)
    }

    /**
     * The way into the notices and the name of what it opens are one string (§14).
     *
     * Three literals for three words, and the masthead of the screen it opens is one of the three:
     * a row in Ajustes that opened a screen called something else would read as two features.
     */
    @Test
    fun `the notices entry and the screen it opens share a name`() {
        assertEquals("Avisos y licencias", NOTICES_LABEL)
        assertEquals(NOTICES_LABEL, screenTitle(Routes.NOTICES))
    }

    /** Ajustes is named once, by the masthead, and the screen no longer repeats it. */
    @Test
    fun `Ajustes is the masthead's word`() {
        assertEquals(SETTINGS_LABEL, screenTitle(Routes.SETTINGS))
    }
}
