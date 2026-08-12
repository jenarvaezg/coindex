package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The resting face of a hole nobody declared a `printed_side` for.
 *
 * Two surfaces read this — the album grid of Monedas with its ficha, and the pieces of a collection
 * without an issue list or of a box (#423) — so it is tested where it lives rather than as a footnote
 * of one of them. The rule it defends is that the turn is only ever offered when there is a second
 * photograph to turn to: a hole that swings round onto a silhouette is a coin with no back.
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
}
