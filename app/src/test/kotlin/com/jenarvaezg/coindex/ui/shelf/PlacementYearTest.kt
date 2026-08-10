package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.placementYear
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Which year a piece is placed by on an axis (ADR 0026 §9).
 *
 * Matching a casilla still reads the engraved year; the axis reads the Gregorian one, and the
 * undated inherit their type's minimum (#326).
 */
class PlacementYearTest {
    @Test
    fun `a Hijri engraved year lands on its Gregorian twin`() {
        // ½ Dirham de Marruecos: says 1316, struck in 1899.
        assertEquals(
            1899,
            placementYear(
                item(issueYear = 1316, gregorianYear = 1899),
                meta = null,
            ),
        )
        // 50 Qirsh de Egipto: says 1375, struck in 1956.
        assertEquals(
            1956,
            placementYear(
                item(issueYear = 1375, gregorianYear = 1956),
                meta = null,
            ),
        )
    }

    @Test
    fun `without a Gregorian year the engraved one is still the placement`() {
        assertEquals(1960, placementYear(item(issueYear = 1960), meta = null))
    }

    @Test
    fun `Numista year zero is not a placement year`() {
        assertEquals(
            1895,
            placementYear(
                item(issueYear = 0, gregorianYear = 0),
                meta = TypeMeta(id = 1, minYear = 1895),
            ),
        )
        assertNull(
            placementYear(
                item(issueYear = 0, gregorianYear = 0),
                meta = null,
            ),
        )
    }

    @Test
    fun `an undated piece inherits the type minimum`() {
        assertEquals(
            1813,
            placementYear(
                item(issueYear = null, gregorianYear = null),
                meta = TypeMeta(id = 1, minYear = 1813),
            ),
        )
        assertNull(placementYear(item(issueYear = null, gregorianYear = null), meta = null))
    }

    private fun item(issueYear: Int?, gregorianYear: Int? = null) = CollectedItem(
        id = 1,
        quantity = 1,
        typeId = 1,
        issueYear = issueYear,
        gregorianYear = gregorianYear,
    )
}
