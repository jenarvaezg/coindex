package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The resting face of a hole with no declared `printed_side`, shared by the Monedas grid and the
 * pieces of a collection (#423). The turn is offered only when there is a second photograph.
 */
class AlbumFacesTest {
    private val reverse = CoinPhoto(picture = "https://example.test/rev.jpg")
    private val obverse = CoinPhoto(picture = "https://example.test/obv.jpg")

    @Test
    fun `the reverse rests up and the obverse waits behind it`() {
        val (photo, other) = coinAlbumFaces(TypeImages(obverse = obverse, reverse = reverse))

        assertEquals(reverse, photo)
        assertEquals(obverse, other)
    }

    @Test
    fun `a type with only an obverse still has a hole to fly`() {
        val (photo, other) = coinAlbumFaces(TypeImages(obverse = obverse))

        assertEquals(obverse, photo)
        assertNull(other)
    }

    @Test
    fun `a face nobody photographed is no face to turn to`() {
        val unphotographed = CoinPhoto(thumbnail = null, picture = null)
        val (photo, other) = coinAlbumFaces(
            TypeImages(obverse = unphotographed, reverse = reverse),
        )

        assertEquals(reverse, photo)
        assertNull(other)
    }

    @Test
    fun `a type with no pictures at all is a hole and not a turn`() {
        val (photo, other) = coinAlbumFaces(null)

        assertNull(photo)
        assertNull(other)
    }

    /**
     * A declared side wins over the album's reverse-first default (ADR 0020, #227), or the coin
     * would turn over on its way into the sheet (#508).
     */
    @Test
    fun `a casilla opens its sheet on the face its plate declared`() {
        val images = TypeImages(obverse = obverse, reverse = reverse)

        assertEquals(obverse to reverse, printedFaces(images, PrintedSide.Obverse))
        assertEquals(reverse to obverse, printedFaces(images, PrintedSide.Reverse))
    }

    /** The hole's ghost is a drawing, not a face. */
    @Test
    fun `a type nobody photographed still answers with two absent faces`() {
        assertEquals(null to null, printedFaces(null, PrintedSide.Reverse))
    }
}
