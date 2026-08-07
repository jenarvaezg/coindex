package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CoverageRatio
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the collector is told once a sheet has left for the share sheet.
 *
 * The sheet that gets shared is the product, so what it is called after exporting has to match what
 * is on it. It used to call any sheet «completa» as long as every picture had reported back, and a
 * picture that failed reported back exactly like one that arrived: twelve empty cells of the 1000
 * escudos were announced as a complete plate (issue #67).
 *
 * **One sentence for the two sheets** (#219). A plate and a collection's pieces used to have a
 * message each — same three branches, same arithmetic, two nouns — and that is the shape #226
 * drifted in. What differs between them is what they are called and what they count, and both are
 * arguments now, so the drift has nowhere left to happen.
 */
class SheetLabelsTest {
    @Test
    fun `an exported sheet is only called complete when every picture is on it`() {
        assertEquals(
            "Lámina completa exportada · 19 casillas",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(19), 38, 38),
        )
        assertEquals(
            "Hoja completa exportada · 10 monedas · 4 tipos",
            sheetExportMessage(SharedSheet.PIECES, countLabel(4, 10), 12, 12),
        )
    }

    @Test
    fun `a sheet with holes says how many, in the plural it needs`() {
        assertEquals(
            "Lámina exportada, pero 10 fotos no llegaron a cargar",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(19), 38, 28),
        )
        assertEquals(
            "Lámina exportada, pero una foto no llegó a cargar",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(19), 38, 37),
        )
        assertEquals(
            "Hoja exportada, pero 3 fotos no llegaron a cargar",
            sheetExportMessage(SharedSheet.PIECES, countLabel(4, 10), 12, 9),
        )
        assertEquals(
            "Hoja exportada, pero una foto no llegó a cargar",
            sheetExportMessage(SharedSheet.PIECES, countLabel(4, 10), 12, 11),
        )
    }

    /** A catalog whose types have no cached pictures exports a complete sheet of silhouettes. */
    @Test
    fun `a sheet that asked for no pictures is complete`() {
        assertEquals(
            "Lámina completa exportada · 3 casillas",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(3), 0, 0),
        )
    }

    /** More painted than expected is not a negative complaint: the count floors at zero. */
    @Test
    fun `more photos than expected never reads as a negative`() {
        assertEquals(
            "Hoja completa exportada · 2 monedas · 2 tipos",
            sheetExportMessage(SharedSheet.PIECES, countLabel(2, 2), 2, 4),
        )
    }

    /**
     * A plate of one slot says «1 casilla». The old sentence pasted the number in front of the
     * plural and read «1 casillas» — invisible on the catalogs there are, and wrong the day one has
     * a single member.
     */
    @Test
    fun `a plate of one slot counts it in the singular`() {
        assertEquals(
            "Lámina completa exportada · 1 casilla",
            sheetExportMessage(SharedSheet.PLATE, plateSheetTally(1), 2, 2),
        )
    }

    /**
     * The message counts what the sheet counts, because both read [countSentence] off the same
     * collection: a card carrying a ratio is announced with the ratio and not with a count of rows
     * (#226).
     */
    @Test
    fun `a shared sheet is announced in the words its collection counts itself with`() {
        val subject = PiecesSubject(
            title = "Las francesas",
            issuer = "Francia",
            variant = "Plata · 1 oz",
            coverage = CoverageRatio(0, 12),
            distinctTypes = 3,
            quantity = 4,
            pieces = emptyList(),
            boxId = null,
        )

        assertEquals(
            "Hoja completa exportada · 0 de 12 · te faltan 12",
            sheetExportMessage(SharedSheet.PIECES, subject.countSentence, 12, 12),
        )
    }

    /**
     * A failed export names what it could not share, with the noun the message would have used.
     *
     * It is prose and not a stack trace, but it keeps the cause: the two ways this fails — no room
     * on disk, no app to share to — are things the collector can act on.
     */
    @Test
    fun `a failed export says which sheet it could not share`() {
        assertEquals(
            "No se pudo exportar la lámina: la lámina aún no se ha dibujado",
            sheetExportFailure(SharedSheet.PLATE, "la lámina aún no se ha dibujado"),
        )
        assertEquals(
            "No se pudo exportar la hoja: ENOSPC",
            sheetExportFailure(SharedSheet.PIECES, "ENOSPC"),
        )
    }

    /**
     * An exception with nothing to say stops at the sentence.
     *
     * Interpolated straight in, a null message put the word «null» in front of the collector, which
     * reads as the app having broken rather than as an export that did not happen.
     */
    @Test
    fun `an exception with no message never says null`() {
        assertEquals(
            "No se pudo exportar la lámina.",
            sheetExportFailure(SharedSheet.PLATE, null),
        )
        assertEquals(
            "No se pudo exportar la hoja.",
            sheetExportFailure(SharedSheet.PIECES, "   "),
        )
    }
}
