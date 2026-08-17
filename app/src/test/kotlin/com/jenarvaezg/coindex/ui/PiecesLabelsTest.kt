package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What a collection of pieces counts, and how the sheet it exports says it.
 *
 * It counts coins and not «casillas», because a sheet of pieces has no slot the mint has not struck
 * — the whole difference between it and a plate (ADR 0021 §9) — except for the one card that
 * reaches this screen carrying a ratio (§7), which keeps the ratio it arrived with.
 *
 * Whichever of the two it is, it counts it **once**: the screen, the exported sheet, the notebook
 * page and the message all read the same sentence off the subject, because #226 was three surfaces
 * spelling the expression out and one of them spelling it differently. The message itself is
 * [SheetLabelsTest] now — one sentence for the two sheets since #219 — and it is handed this same
 * [countSentence].
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

    /** With no issue list there is nothing to be missing from, so the sentence is the count. */
    @Test
    fun `a collection with no issue list counts its coins`() {
        assertEquals("10 monedas · 4 tipos", subject().countSentence)
        assertEquals("1 moneda · 1 tipo", subject(distinctTypes = 1, quantity = 1).countSentence)
    }

    /**
     * A collection whose catalog it owns no issued member of yet keeps the card's own ratio — by
     * ADR 0021 §7 and §9 it is the only one that arrives here carrying one, and the same collection
     * cannot count one way on the card and another one tap later.
     */
    @Test
    fun `a collection carrying a ratio keeps counting the ratio`() {
        assertEquals(
            "0 de 12 · te faltan 12",
            subject(coverage = CoverageRatio(0, 12), distinctTypes = 3, quantity = 4).countSentence,
        )
    }

    /**
     * The sheet's masthead is the subject's own sentence. It used to spell out `countLabel`, so the
     * PNG that had just been shared said «3 tipos distintos · 4 piezas» about a collection the
     * screen behind it was calling «0 de 12 · te faltan 12» (#226).
     */
    @Test
    fun `the shared sheet counts what the screen that shared it counts`() {
        val piezas = subject(coverage = CoverageRatio(0, 12), distinctTypes = 3, quantity = 4)
        assertEquals(
            listOf(
                "País" to "Francia",
                "Variante" to "Plata · 1 oz",
                "Piezas" to "0 de 12 · te faltan 12",
            ),
            piecesSheetFacts(piezas),
        )
    }

    /**
     * A country the pieces disagree about, and the variant a box does not have, go unsaid rather
     * than printed empty: the sheet outlives the app, and a blank field reads as something lost.
     */
    @Test
    fun `what nothing can name goes unprinted`() {
        assertEquals(
            listOf("Piezas" to "10 monedas · 4 tipos"),
            piecesSheetFacts(subject(issuer = null, variant = null)),
        )
    }

    /**
     * The button carries the cost before it is pressed (ADR 0021 §11).
     *
     * Seeding unconditionally offered the whole collection — 191 coins — so the count is on the
     * button precisely so the collector sees that the filter wants narrowing first. With nothing
     * narrowing the list there is nothing to seed, so it says neither a count nor «estas».
     *
     * The word is **colección** since #516: ADR 0021 §11 wrote it as «Agrupar estas 6», and that
     * verb was the third name this one feature had for the thing it makes.
     */
    @Test
    fun `making one says how many it would seed, and only when there is a seed`() {
        assertEquals("Hacer una colección con estas 59", boxDoorLabel(seeded = true, shown = 59))
        assertEquals("Hacer una colección", boxDoorLabel(seeded = false, shown = 191))
    }

    /**
     * Which side the work starts from: with a seed there is something to remove; without one the
     * gesture is the card itself — not a phantom «Elegir» (#402).
     */
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

    /**
     * The baptism counts what is about to be named, and the species word is the eyebrow's alone
     * (#516): «Tu colección» over «2 monedas elegidas» says it once on a card three lines tall.
     */
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
