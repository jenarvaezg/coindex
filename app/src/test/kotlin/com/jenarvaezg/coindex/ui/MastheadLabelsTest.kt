package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MastheadLabelsTest {
    /**
     * Collections, pieces and types on every root (#400), in full; «monedas» already names the
     * sibling hierarchy. No wall-clock minute (#419): the line never meant «last sync».
     */
    @Test
    fun `the sewn edge owns the three counts`() {
        assertEquals(
            "70 colecciones · 574 piezas · 192 tipos",
            sewnEdgeLabel(SewnEdgeCounts(70, 574, 192)),
        )
    }

    /**
     * Zeros would claim an empty collection on a cold start (#418); the placeholder already says
     * the phone is still reading.
     */
    @Test
    fun `while the snapshot is unread the chrome stays silent about counts`() {
        assertEquals("—", sewnEdgeLabel(null))
        assertEquals("Colecciones · —", collectionsCellLabel(null))
        assertEquals("Tipos · —", typesCellLabel(null))
        assertEquals("Las cifras · —", figuresCellLabel(figuresCellCount(null)))
    }

    /** Both hierarchies are one notebook (ADR 0021 §1); the bottom bar says which one. */
    @Test
    fun `both roots keep the notebook's own strapline`() {
        assertEquals("Inventario de campo · plata bullion", screenTitle(Routes.INDEX))
        assertEquals("Inventario de campo · plata bullion", screenTitle(Routes.COINS))
    }

    @Test
    fun `every screen reached through them names itself`() {
        assertEquals("Este teléfono", screenTitle(Routes.PHONE))
        assertEquals("Credenciales", screenTitle(Routes.CREDENTIALS))
        assertEquals("Avisos y licencias", screenTitle(Routes.NOTICES))
    }

    @Test
    fun `a plate names the catalog it is showing`() {
        assertEquals("Lámina · Lunar II", screenTitle(Routes.PLATE, subjectName = "Lunar II"))
    }

    @Test
    fun `a plate whose catalog cannot be resolved still says it is a plate`() {
        assertEquals("Lámina", screenTitle(Routes.PLATE, subjectName = null))
    }

    @Test
    fun `a derived collection names the collection, with the curator's own word for it`() {
        // The collector's word (#13), from the catalog's `short_name` like any card name (#22).
        assertEquals(
            "Colección · Paquillos",
            screenTitle(Routes.DERIVED_COLLECTION, subjectName = "Paquillos"),
        )
        assertEquals("Colección", screenTitle(Routes.DERIVED_COLLECTION, subjectName = null))
    }

    /** One species (ADR 0021 §2), one screen (§9): «Tu agrupación» would mark provenance again. */
    @Test
    fun `a box is a collection too, and the masthead calls it one`() {
        assertEquals(
            "Colección · Las francesas",
            screenTitle(Routes.OWN_GROUPING, subjectName = "Las francesas"),
        )
        assertEquals("Colección", screenTitle(Routes.OWN_GROUPING, subjectName = null))
    }

    @Test
    fun `an unresolved route falls back to the strapline instead of going blank`() {
        assertEquals("Inventario de campo · plata bullion", screenTitle(null))
        assertEquals("Inventario de campo · plata bullion", screenTitle("quién-sabe"))
    }

    /**
     * «Las cifras» counts weight, never money: a permanent bar must not show the collection's value
     * to anyone glancing at the phone (#316). The middle cell says «Tipos» since #516.
     */
    @Test
    fun `the three cells count what their destination is made of`() {
        assertEquals("Colecciones · 58", collectionsCellLabel(58))
        assertEquals("Tipos · 191", typesCellLabel(191))
        assertEquals("Las cifras · 4,2 kg", figuresCellLabel("4,2 kg"))
    }

    /** It stops rather than draw a wrong plate, and the collector can't fix the assets. */
    @Test
    fun `a fatal start says it stopped rather than that it lost anything`() {
        assertEquals("No se pudo arrancar", FATAL_HEADING)
        assertTrue("lámina incorrecta" in FATAL_EXPLANATION)
    }
}
