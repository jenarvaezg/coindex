package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.ui.screens.SheetLayout
import com.jenarvaezg.coindex.ui.screens.sheetImageCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun member(id: String, typeId: Int) = CollectionCatalogAlbumMember(
    member = CollectionCatalogMember(id, id, 2024, typeId),
    status = CollectionCatalogMemberStatus.Missing,
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
    }

    @Test
    fun `the expected picture count skips types with no cached pictures`() {
        val members = listOf(member("a", 1), member("b", 2), member("c", 3))
        val images = mapOf(
            1 to TypeImages("obverse", "reverse"),
            2 to TypeImages("obverse", null),
            // Type 3 has no cached pictures at all and requests none.
        )

        assertEquals(3, sheetImageCount(members, images))
        assertEquals(0, sheetImageCount(members, emptyMap()))
    }
}
