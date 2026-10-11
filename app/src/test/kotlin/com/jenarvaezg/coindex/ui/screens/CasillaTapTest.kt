package com.jenarvaezg.coindex.ui.screens

import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.DrawnCell
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A casilla has one target at a time (ADR 0026 §3, #632): its coin's sheet, or its mark while the
 * marking mode is open (ADR 0029 §5).
 */
class CasillaTapTest {
    private val bolivar = WishKey(typeId = 10_338, year = 1_886, issueId = null)

    private fun casilla(
        typeId: Int? = 10_338,
        missing: Boolean = true,
        wishKey: WishKey? = bolivar,
    ) = DrawnCell(
        id = "casilla",
        label = "Bolívar",
        numistaTypeId = typeId,
        footnote = null,
        year = "1886",
        owned = !missing,
        missing = missing,
        wishKey = wishKey,
    )

    @Test
    fun `an empty casilla opens its coin's sheet`() {
        assertEquals(CasillaTap.Open(10_338), casillaTap(casilla(), picking = false))
    }

    @Test
    fun `a full casilla opens its coin's sheet`() {
        assertEquals(CasillaTap.Open(10_338), casillaTap(casilla(missing = false), picking = false))
    }

    @Test
    fun `an announced member has no ficha to open`() {
        assertEquals(CasillaTap.None, casillaTap(casilla(typeId = null, wishKey = null), picking = false))
    }

    @Test
    fun `in the marking mode an empty casilla marks and opens nothing`() {
        assertEquals(CasillaTap.Mark(bolivar), casillaTap(casilla(), picking = true))
    }

    @Test
    fun `in the marking mode a full casilla answers nothing`() {
        assertEquals(CasillaTap.None, casillaTap(casilla(missing = false), picking = true))
    }

    @Test
    fun `in the marking mode an announced member answers nothing`() {
        assertEquals(
            CasillaTap.None,
            casillaTap(casilla(typeId = null, wishKey = null), picking = true),
        )
    }
}
