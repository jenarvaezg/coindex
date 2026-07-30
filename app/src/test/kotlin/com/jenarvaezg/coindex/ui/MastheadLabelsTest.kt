package com.jenarvaezg.coindex.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MastheadLabelsTest {
    @Test
    fun `the index keeps the notebook's own strapline`() {
        assertEquals("Inventario de campo · plata bullion", screenTitle(Routes.INDEX))
    }

    @Test
    fun `every other screen names itself`() {
        assertEquals("Sin clasificar", screenTitle(Routes.UNCLASSIFIED))
        assertEquals("Ajustes", screenTitle(Routes.SETTINGS))
    }

    @Test
    fun `a plate names the catalog it is showing`() {
        assertEquals("Lámina · Lunar II", screenTitle(Routes.PLATE, catalogName = "Lunar II"))
    }

    @Test
    fun `a plate whose catalog cannot be resolved still says it is a plate`() {
        assertEquals("Lámina", screenTitle(Routes.PLATE, catalogName = null))
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
