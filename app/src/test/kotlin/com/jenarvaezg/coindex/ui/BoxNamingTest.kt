package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The baptism of a box: one field, 40 characters, and unique at creation (ADR 0021 §4, §11).
 *
 * The three cases #173 asks for are empty, repeated — against a curated file **and** against another
 * box — and longer than the limit. What makes them one function is that all three answer the same two
 * questions the dialog has: can «Crear» be pressed, and is there anything to say about why not.
 */
class BoxNamingTest {
    private val taken = listOf(
        // A curated `short_name` from `data/`, and a box the collector already typed.
        "French regions",
        "Bolívar de Venezuela",
        "Las que cambié",
    )

    @Test
    fun `an empty field cannot create, and says nothing about it`() {
        val name = boxName("", taken)

        assertFalse(name.canSave)
        assertNull(name.problem)
        assertEquals("0/40 · tiene que caber en una tarjeta", name.counter)
        // Whitespace is not a name either, and it is still not a complaint.
        assertFalse(boxName("   ", taken).canSave)
        assertNull(boxName("   ", taken).problem)
    }

    @Test
    fun `a name that is taken says which one, and cannot create`() {
        val name = boxName("French regions", taken)

        assertFalse(name.canSave)
        assertEquals(
            "Ya hay una colección que se llama «French regions». Ponle otro nombre.",
            name.problem,
        )
    }

    @Test
    fun `it is taken whether the clash is a curated file or another box`() {
        assertFalse(boxName("Bolívar de Venezuela", taken).canSave)
        assertFalse(boxName("Las que cambié", taken).canSave)
    }

    @Test
    fun `accents and case are not a distinction`() {
        // The same shelf in anybody's head, and two cards a letter apart would be a filing mistake.
        assertFalse(boxName("french REGIONS", taken).canSave)
        assertFalse(boxName("bolivar de venezuela", taken).canSave)
        assertFalse(boxName("Las que cambie", taken).canSave)
        // And the clash names the existing one as it is actually written.
        assertEquals(
            "Ya hay una colección que se llama «Bolívar de Venezuela». Ponle otro nombre.",
            boxName("BOLIVAR DE VENEZUELA", taken).problem,
        )
    }

    @Test
    fun `longer than the limit says how long it is, and cannot create`() {
        val fortyOne = "a".repeat(BOX_NAME_LIMIT + 1)

        val name = boxName(fortyOne, taken)

        assertFalse(name.canSave)
        assertEquals(
            "Son 41 caracteres y el límite son 40: tiene que caber en una tarjeta.",
            name.problem,
        )
        // Exactly at the limit is fine: the measured files run from 6 to 37 characters.
        assertTrue(boxName("a".repeat(BOX_NAME_LIMIT), taken).canSave)
    }

    @Test
    fun `a good name creates, and what is stored is the trimmed text`() {
        val name = boxName("  Las francesas  ", taken)

        assertTrue(name.canSave)
        assertNull(name.problem)
        assertEquals("Las francesas", name.stored)
        // The counter counts what would be stored, so trailing spaces do not inflate it.
        assertEquals("13/40 · tiene que caber en una tarjeta", name.counter)
    }
}
