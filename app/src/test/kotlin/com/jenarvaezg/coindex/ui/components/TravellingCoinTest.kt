package com.jenarvaezg.coindex.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * The two journeys of ADR 0026 §3 share a layout but not a key: a catalog flight (index → casilla)
 * and a type flight (Monedas → ficha) can show the same photograph, and one key would let Compose
 * pick the wrong end.
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
}
