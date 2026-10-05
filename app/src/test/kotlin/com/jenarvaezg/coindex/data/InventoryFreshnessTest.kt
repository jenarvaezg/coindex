package com.jenarvaezg.coindex.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val NOW = 1_786_170_660_000L
private const val HOUR = 60L * 60 * 1_000

/**
 * When the inventory is worth two consultas again (#605). A day is both floor and ceiling: less
 * pays twice for one answer across a day's launches, more leaves Monday's coin off its plate on
 * Tuesday.
 */
class InventoryFreshnessTest {
    @Test
    fun `the inventory is believed for a day and not a minute more`() {
        assertFalse(inventoryIsStale(NOW, NOW + 23 * HOUR))
        assertFalse(inventoryIsStale(NOW, NOW + 24 * HOUR - 1))
        assertTrue(inventoryIsStale(NOW, NOW + 24 * HOUR))
    }

    @Test
    fun `never having synced is stale`() {
        assertTrue(inventoryIsStale(null, NOW))
    }

    /**
     * A clock set back (time zone, manual change, reboot offline) would otherwise trust an
     * arbitrarily old inventory for good; asking only costs two consultas.
     */
    @Test
    fun `a clock that went backwards does not certify anything`() {
        assertTrue(inventoryIsStale(NOW, NOW - HOUR))
    }
}
