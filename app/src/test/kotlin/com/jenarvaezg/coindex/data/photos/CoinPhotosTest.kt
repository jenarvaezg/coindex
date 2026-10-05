package com.jenarvaezg.coindex.data.photos

import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.toImages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A plate asks for every cell's photograph at once, and a burst of originals made Numista's edge
 * answer `503` (#67), so the 180-pixel thumbnail goes first and the original is the fallback.
 */
class CoinPhotosTest {
    private val original = "https://en.numista.com/catalogue/photos/portugal/10207-original.jpg"
    private val thumbnail = "https://en.numista.com/catalogue/photos/portugal/10207-180.jpg"

    @Test
    fun `the thumbnail is asked for first and the original waits behind it`() {
        val photo = CoinPhoto(thumbnail = thumbnail, picture = original)

        assertEquals(listOf(thumbnail, original), photo.candidates)
        assertTrue(photo.hasPicture)
    }

    @Test
    fun `a face cached before the thumbnails existed still has its original to ask for`() {
        val photo = CoinPhoto(thumbnail = null, picture = original)

        assertEquals(listOf(original), photo.candidates)
        assertTrue(photo.hasPicture)
    }

    /** The seed falls back to the thumbnail when a ficha has no original, so both can coincide. */
    @Test
    fun `the same URL under both names is asked for once`() {
        val photo = CoinPhoto(thumbnail = thumbnail, picture = thumbnail)

        assertEquals(listOf(thumbnail), photo.candidates)
    }

    @Test
    fun `a face with no picture at all asks for nothing`() {
        val photo = CoinPhoto()

        assertEquals(emptyList(), photo.candidates)
        assertFalse(photo.hasPicture)
    }

    @Test
    fun `a cached type carries both sizes of both faces`() {
        val entity = TypeMetaEntity(
            typeId = 10_207,
            title = "1000 escudos",
            family = null,
            issuerCode = "portugal",
            minYear = null,
            maxYear = null,
            weightGrams = 27.0,
            obverseUrl = original,
            reverseUrl = "$original?reverse",
            raw = "{}",
            fetchedAt = 0,
            obverseThumbnailUrl = thumbnail,
            reverseThumbnailUrl = "$thumbnail?reverse",
        )

        assertEquals(
            TypeImages(
                obverse = CoinPhoto(thumbnail, original),
                reverse = CoinPhoto("$thumbnail?reverse", "$original?reverse"),
            ),
            entity.toImages(),
        )
    }
}
