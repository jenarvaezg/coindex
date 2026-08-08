package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.ui.indexCoverageLabel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * How many cards the index puts side by side.
 *
 * The count is the layout's one decision: it picks the number of columns and, with it, whether
 * the heading folds into a spread. A phone in portrait must stay exactly as it was.
 */
class IndexLayoutTest {
    @Test
    fun `an album card prints coverage as a fraction`() {
        assertEquals("4/5", indexCoverageLabel(CoverageRatio(4, 5)))
        assertEquals("22/22", indexCoverageLabel(CoverageRatio(22, 22)))
    }

    @Test
    fun `the Pixel 7 album fits three collection holes across`() {
        assertEquals(3, indexColumns(411.dp))
        assertEquals(3, indexColumns(360.dp))
        assertEquals(2, indexColumns(320.dp))
    }

    @Test
    fun `the same phone held sideways keeps the album cells compact`() {
        // 914dp is the Pixel 7 of the UX review, landscape.
        assertEquals(8, indexColumns(914.dp))
    }

    @Test
    fun `a tablet keeps adding columns rather than stretching the cards`() {
        assertEquals(9, indexColumns(1_100.dp))
    }
}
