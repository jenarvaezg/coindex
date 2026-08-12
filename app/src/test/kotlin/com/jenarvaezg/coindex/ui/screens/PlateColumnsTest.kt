package com.jenarvaezg.coindex.ui.screens

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The plate reads the grid's own arithmetic one step ahead of it, because which casillas share a
 * row is what decides which of them reserve a name box (#337).
 *
 * If `GridCells.Adaptive` ever counted differently, a row would reserve the box for a neighbour
 * that is not on it and the tags would stop lining up — quietly, and only on some screen widths.
 */
class PlateColumnsTest {
    /** The Pixel 7 the whole map was measured on: 411 dp, 20 dp of margin on each side. */
    @Test
    fun `a 411 dp phone gets the three columns the map measured`() {
        assertEquals(3, plateColumns(411.dp - 40.dp))
    }

    @Test
    fun `a narrow screen still gets one column instead of none`() {
        assertEquals(1, plateColumns(0.dp))
        assertEquals(1, plateColumns(103.dp))
    }

    @Test
    fun `a column is added exactly when its own gutter is paid for too`() {
        assertEquals(1, plateColumns(104.dp))
        assertEquals(1, plateColumns(119.dp))
        assertEquals(2, plateColumns(224.dp))
        assertEquals(4, plateColumns(464.dp))
    }

    /**
     * The width a name is measured against before it is drawn (#412): the gutters belong to the
     * spaces *between* columns, so three columns on a Pixel 7 pay for two of them and not three.
     */
    @Test
    fun `the width a name is measured against is the column the grid will give it`() {
        assertEquals(113f, plateCellWidth(411.dp - 40.dp, columns = 3).value, 0.01f)
        assertEquals(371f, plateCellWidth(411.dp - 40.dp, columns = 1).value, 0.01f)
    }
}
