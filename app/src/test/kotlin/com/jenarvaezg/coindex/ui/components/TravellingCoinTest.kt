package com.jenarvaezg.coindex.ui.components

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.ui.screens.coinAlbumFaces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * The two journeys of ADR 0026 §3 share a layout and must not share a key.
 *
 * A catalog flight (index → casilla) and a type flight (Monedas → ficha) can both be on screen
 * for the same photograph — Lunar Series III's Snake is the cover of a card and a cell of
 * Monedas. One key for both would make Compose pick an end that is not the one the finger
 * opened.
 */
class TravellingCoinTest {
    @Test
    fun `a catalog journey is keyed by the catalog, not by the type`() {
        assertEquals("coin-lunar-series-iii-1oz", travellingCatalogKey("lunar-series-iii-1oz"))
    }

    @Test
    fun `a type journey is keyed by the Numista type`() {
        assertEquals("type-404064", travellingTypeKey(404_064))
    }

    @Test
    fun `the two journeys never collide on the same photograph`() {
        assertNotEquals(
            travellingCatalogKey("lunar-series-iii-1oz"),
            travellingTypeKey(404_064),
        )
    }

    @Test
    fun `Monedas prefers the reverse so the ficha lands on the same face`() {
        val reverse = CoinPhoto(picture = "https://example.test/rev.jpg")
        val obverse = CoinPhoto(picture = "https://example.test/obv.jpg")
        val (photo, other) = coinAlbumFaces(TypeImages(obverse = obverse, reverse = reverse))

        assertEquals(reverse, photo)
        assertEquals(obverse, other)
    }

    @Test
    fun `a type with only an obverse still has a hole to fly`() {
        val obverse = CoinPhoto(picture = "https://example.test/obv.jpg")
        val (photo, other) = coinAlbumFaces(TypeImages(obverse = obverse))

        assertEquals(obverse, photo)
        assertNull(other)
    }
}
