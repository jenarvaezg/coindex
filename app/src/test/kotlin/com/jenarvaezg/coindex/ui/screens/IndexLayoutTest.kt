package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.dp
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
    fun `a phone held upright is one column, as it always was`() {
        assertEquals(1, indexColumns(411.dp))
        assertEquals(1, indexColumns(360.dp))
        // Even a narrow one: a card below its minimum is still better than half a card.
        assertEquals(1, indexColumns(320.dp))
    }

    @Test
    fun `the same phone held sideways has room for two`() {
        // 914dp is the Pixel 7 of the UX review, landscape.
        assertEquals(2, indexColumns(914.dp))
    }

    @Test
    fun `a tablet keeps adding columns rather than stretching the cards`() {
        assertEquals(3, indexColumns(1_100.dp))
    }
}
