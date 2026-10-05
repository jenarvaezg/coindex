package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * How a collection of pieces counts itself: coins, not casillas (ADR 0021 §9), unless its card
 * carries a ratio (§7). Every surface reads this one [PiecesSubject.countSentence] (#226).
 */
class PiecesLabelsTest {
    private fun subject(
        coverage: CoverageRatio? = null,
        distinctTypes: Int = 4,
        quantity: Int = 10,
        issuer: String? = "Francia",
        variant: String? = "Plata · 1 oz",
    ) = PiecesSubject(
        title = "Las francesas",
        issuer = issuer,
        variant = variant,
        coverage = coverage,
        distinctTypes = distinctTypes,
        quantity = quantity,
        pieces = emptyList(),
        boxId = null,
    )

    @Test
    fun `a collection with no issue list counts its coins`() {
        assertEquals("10 monedas · 4 tipos", subject().countSentence)
        assertEquals("1 moneda · 1 tipo", subject(distinctTypes = 1, quantity = 1).countSentence)
    }

    /** As on its card (ADR 0021 §7, §9): one collection can't count two ways one tap apart. */
    @Test
    fun `a collection carrying a ratio keeps counting the ratio`() {
        assertEquals(
            "0 de 12 · te faltan 12",
            subject(coverage = CoverageRatio(0, 12), distinctTypes = 3, quantity = 4).countSentence,
        )
    }

    // Its printed heading (#431) is pinned in `NotebookSectionsTest` (#543).

    /**
     * The count is on the button so the collector sees when the filter wants narrowing first
     * (ADR 0021 §11). «Colección» replaced §11's «Agrupar» in #516.
     */
    @Test
    fun `making one says how many it would seed, and only when there is a seed`() {
        assertEquals("Hacer una colección con estas 59", boxDoorLabel(seeded = true, shown = 59))
        assertEquals("Hacer una colección", boxDoorLabel(seeded = false, shown = 191))
    }

    /** With a seed there is something to remove; without one, tapping is the gesture (#402). */
    @Test
    fun `the selection hint says which way the work goes`() {
        assertEquals(
            "Vienen elegidas las 59 que enseñaba el filtro. Quita las que no.",
            selectionHintLabel(seeded = true, shown = 59),
        )
        assertEquals(
            "Toca cada moneda que quieras meter en la colección.",
            selectionHintLabel(seeded = false, shown = 0),
        )
    }

    /** «Colección» is said by the eyebrow only, not again in the heading (#516). */
    @Test
    fun `the baptism counts what is about to be named`() {
        assertEquals("Tu colección", BOX_EYEBROW)
        assertEquals("2 monedas elegidas", boxDialogHeading(2))
        assertEquals("1 moneda elegida", boxDialogHeading(1))
        assertEquals("Nombrar la colección · 2", namePickedBoxLabel(2))
    }

    @Test
    fun `the rename toggle says what pressing it will do`() {
        assertEquals("Renombrar", renameToggleLabel(renaming = false))
        assertEquals("Cerrar el nombre", renameToggleLabel(renaming = true))
    }
}
