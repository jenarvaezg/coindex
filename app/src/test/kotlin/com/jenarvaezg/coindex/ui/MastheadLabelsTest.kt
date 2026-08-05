package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MastheadLabelsTest {
    /**
     * The two hierarchies of the top level are one notebook (ADR 0021 §1), so both keep its
     * strapline: the bottom bar says which of the two you are in, and the heading names it in full.
     */
    @Test
    fun `both roots keep the notebook's own strapline`() {
        assertEquals("Inventario de campo · plata bullion", screenTitle(Routes.INDEX))
        assertEquals("Inventario de campo · plata bullion", screenTitle(Routes.COINS))
    }

    @Test
    fun `every screen reached through them names itself`() {
        assertEquals("Ajustes", screenTitle(Routes.SETTINGS))
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
        // «Paquillos» is the collector's word (#13) and now reaches the masthead the way every
        // other card name does: from the catalog's `short_name`, not from an alias in code (#22).
        assertEquals(
            "Colección · Paquillos",
            screenTitle(Routes.DERIVED_COLLECTION, subjectName = "Paquillos"),
        )
        assertEquals("Colección", screenTitle(Routes.DERIVED_COLLECTION, subjectName = null))
    }

    /**
     * The masthead was the last place that ranked a box below a curated collection. With one
     * species (ADR 0021 §2) and one screen for both (§9), «Tu agrupación · …» has nothing left to
     * distinguish — and saying it would reintroduce the word of provenance the card dropped.
     */
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

    @Test
    fun `the installed version rides along, and is omitted when unknown`() {
        assertEquals("Ajustes · v0.3.1", mastheadSubtitle("Ajustes", "0.3.1"))
        assertEquals("Ajustes", mastheadSubtitle("Ajustes", ""))
    }
}
