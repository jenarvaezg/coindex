package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What a collection of pieces counts, and what the collector is told after sharing it.
 *
 * It counts coins and not «casillas», because a sheet of pieces has no slot the mint has not struck
 * — the whole difference between it and a plate (ADR 0021 §9) — except for the one card that
 * reaches this screen carrying a ratio (§7), which keeps the ratio it arrived with.
 *
 * Whichever of the two it is, it counts it **once**: the screen, the exported sheet, the notebook
 * page and the message all read the same sentence off the subject, because #226 was three surfaces
 * spelling the expression out and one of them spelling it differently.
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
     * The message counts what the sheet counts. It used to report the number of rows drawn while
     * the sheet printed `countLabel` under the title, so a collection of four types held in ten
     * pieces was told «6 piezas» about a sheet that said something else.
     */
    @Test
    fun `everything painted reports the sheet in the sheet's own words`() {
        assertEquals(
            "Hoja completa exportada · 10 monedas · 4 tipos",
            piecesExportMessage(subject = subject(), expectedPhotos = 12, loadedPhotos = 12),
        )
        assertEquals(
            "Hoja completa exportada · 0 de 12 · te faltan 12",
            piecesExportMessage(
                subject = subject(coverage = CoverageRatio(0, 12), distinctTypes = 3, quantity = 4),
                expectedPhotos = 12,
                loadedPhotos = 12,
            ),
        )
    }

    /** A picture that never arrived is a hole in the sheet that has just been shared. */
    @Test
    fun `a missing photo is admitted rather than counted as a complete sheet`() {
        assertEquals(
            "Hoja exportada, pero una foto no llegó a cargar",
            piecesExportMessage(subject = subject(), expectedPhotos = 12, loadedPhotos = 11),
        )
        assertEquals(
            "Hoja exportada, pero 3 fotos no llegaron a cargar",
            piecesExportMessage(subject = subject(), expectedPhotos = 12, loadedPhotos = 9),
        )
    }

    /** More painted than expected is not a negative complaint: the count floors at zero. */
    @Test
    fun `more photos than expected never reads as a negative`() {
        assertEquals(
            "Hoja completa exportada · 2 monedas · 2 tipos",
            piecesExportMessage(
                subject = subject(distinctTypes = 2, quantity = 2),
                expectedPhotos = 2,
                loadedPhotos = 4,
            ),
        )
    }
}
