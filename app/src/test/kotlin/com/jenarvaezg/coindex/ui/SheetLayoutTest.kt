package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.ui.screens.SheetLayout
import com.jenarvaezg.coindex.ui.screens.sheetImageCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun cell(id: String, typeId: Int) = DrawnCell(
    id = id,
    label = id,
    numistaTypeId = typeId,
    footnote = null,
    owned = false,
    missing = true,
)

private fun piece(typeId: Int) = DrawnPiece(
    item = CollectedItem(id = typeId.toLong(), quantity = 1, typeId = typeId),
    emissionLabel = null,
)

class SheetLayoutTest {
    @Test
    fun `the sheet squares off its grid instead of following the screen`() {
        assertEquals(2, SheetLayout.forMemberCount(1).columns)
        assertEquals(4, SheetLayout.forMemberCount(8).columns)
        assertEquals(6, SheetLayout.forMemberCount(21).columns)
        assertEquals(8, SheetLayout.forMemberCount(37).columns)
        // A 121-issue catalog stays at eight columns rather than growing without bound.
        assertEquals(8, SheetLayout.forMemberCount(121).columns)
    }

    @Test
    fun `density shrinks as the catalog grows so the bitmap stays a sane size`() {
        val small = SheetLayout.forMemberCount(12)
        val medium = SheetLayout.forMemberCount(37)
        val large = SheetLayout.forMemberCount(121)

        assertTrue(small.density.density > medium.density.density)
        assertTrue(medium.density.density > large.density.density)

        // Rough pixel budget: no sheet should be wider than a few thousand pixels.
        listOf(small, medium, large).forEach { layout ->
            val widthPx = layout.width.value * layout.density.density
            assertTrue(widthPx in 500f..3_500f, "ancho fuera de rango: $widthPx")
        }
    }

    @Test
    fun `a zero member count never produces an unusable layout`() {
        val layout = SheetLayout.forMemberCount(0)

        assertEquals(2, layout.columns)
        assertTrue(layout.width.value > 0f)
        assertTrue(layout.headerScale >= 1f)
    }

    @Test
    fun `the heading grows with the sheet instead of staying at phone size`() {
        val narrow = SheetLayout.forMemberCount(2)
        val medium = SheetLayout.forMemberCount(12)
        val wide = SheetLayout.forMemberCount(121)

        assertTrue(narrow.headerScale < medium.headerScale)
        assertTrue(medium.headerScale < wide.headerScale)

        // Never smaller than the cell titles it presides over, never a billboard either.
        listOf(narrow, medium, wide).forEach { layout ->
            assertTrue(
                layout.headerScale in 1f..2f,
                "escala de cabecera fuera de rango: ${layout.headerScale}",
            )
        }
    }

    @Test
    fun `the expected picture count skips types with no cached pictures`() {
        val cells = listOf(cell("a", 1), cell("b", 2), cell("c", 3))
        val images = mapOf(
            1 to TypeImages(CoinPhoto(picture = "obverse"), CoinPhoto(picture = "reverse")),
            2 to TypeImages(CoinPhoto(picture = "obverse"), CoinPhoto()),
            // Type 3 has no cached pictures at all and requests none.
        )

        assertEquals(3, sheetImageCount(cells, images) { it.numistaTypeId })
        assertEquals(0, sheetImageCount(cells, emptyMap()) { it.numistaTypeId })
    }

    @Test
    fun `a plate waits only for the catalog printed side`() {
        val cells = listOf(cell("a", 1), cell("b", 2))
        val images = mapOf(
            1 to TypeImages(CoinPhoto(picture = "obverse-1"), CoinPhoto(picture = "reverse-1")),
            2 to TypeImages(CoinPhoto(), CoinPhoto(picture = "reverse-2")),
        )

        assertEquals(
            1,
            sheetImageCount(cells, images, PrintedSide.Obverse) { it.numistaTypeId },
        )
        assertEquals(
            2,
            sheetImageCount(cells, images, PrintedSide.Reverse) { it.numistaTypeId },
        )
    }

    @Test
    fun `the catalog declaration chooses the resting screen and PNG photo`() {
        val images = TypeImages(
            obverse = CoinPhoto(picture = "the-obverse"),
            reverse = CoinPhoto(picture = "the-reverse"),
        )

        assertEquals("the-obverse", images.printedPhoto(PrintedSide.Obverse).picture)
        assertEquals("the-reverse", images.printedPhoto(PrintedSide.Reverse).picture)
    }

    /**
     * The same count, over the rows of a collection rather than the slots of a catalog.
     *
     * One function for the two sheets (#219): they differ in where the Numista type is read from —
     * a cell's may be absent, a piece's never is — and in nothing else, and two copies of the
     * arithmetic is how one of them would come to wait for a photograph the other does not.
     */
    @Test
    fun `a sheet of pieces counts its pictures the same way`() {
        val pieces = listOf(piece(1), piece(2), piece(3))
        val images = mapOf(
            1 to TypeImages(CoinPhoto(picture = "obverse"), CoinPhoto(picture = "reverse")),
            2 to TypeImages(CoinPhoto(picture = "obverse"), CoinPhoto()),
        )

        assertEquals(3, sheetImageCount(pieces, images) { it.item.typeId })
        assertEquals(0, sheetImageCount(pieces, emptyMap()) { it.item.typeId })
    }

    /** A slot no Numista type names asks for nothing rather than for the whole map. */
    @Test
    fun `a cell with no type requests no pictures`() {
        val cells = listOf(cell("a", 1).copy(numistaTypeId = null))
        val images = mapOf(
            1 to TypeImages(CoinPhoto(picture = "obverse"), CoinPhoto(picture = "reverse")),
        )

        assertEquals(0, sheetImageCount(cells, images) { it.numistaTypeId })
    }

    /**
     * A face is one cell of the sheet whether it is asked for once or twice: the thumbnail and
     * the original are two attempts at the same picture, and counting both would leave the
     * export waiting for photographs nobody is going to request.
     */
    @Test
    fun `a face counts once however many sizes of it there are to try`() {
        val cells = listOf(cell("a", 1))
        val images = mapOf(
            1 to TypeImages(
                obverse = CoinPhoto(thumbnail = "small", picture = "big"),
                reverse = CoinPhoto(thumbnail = "small", picture = "big"),
            ),
        )

        assertEquals(2, sheetImageCount(cells, images) { it.numistaTypeId })
    }
}
