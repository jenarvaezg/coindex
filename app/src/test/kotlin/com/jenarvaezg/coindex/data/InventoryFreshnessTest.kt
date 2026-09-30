package com.jenarvaezg.coindex.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val NOW = 1_786_170_660_000L
private const val HOUR = 60L * 60 * 1_000

/**
 * When the inventory is worth two consultas again (#605).
 *
 * The arithmetic is the whole decision, so it is tested as arithmetic: a day is a floor and a
 * ceiling at once — under it a day of eight launches pays twice for the same answer, over it a coin
 * bought on Monday is not on its plate on Tuesday.
 */
class InventoryFreshnessTest {
    @Test
    fun `the inventory is believed for a day and not a minute more`() {
        assertFalse(inventoryIsStale(NOW, NOW + 23 * HOUR))
        assertFalse(inventoryIsStale(NOW, NOW + 24 * HOUR - 1))
        assertTrue(inventoryIsStale(NOW, NOW + 24 * HOUR))
    }

    /** A phone that has never synced is the freshly onboarded one, and it fills itself. */
    @Test
    fun `never having synced is stale`() {
        assertTrue(inventoryIsStale(null, NOW))
    }

    /**
     * A stamp in the future is stale too, and that direction is the one worth pinning.
     *
     * A clock that went backwards — a time zone, a manual change, a reboot without a network — would
     * otherwise leave a phone believing an inventory that is arbitrarily old, for ever. Asking costs
     * two consultas; not asking costs the collection.
     */
    @Test
    fun `a clock that went backwards does not certify anything`() {
        assertTrue(inventoryIsStale(NOW, NOW - HOUR))
    }
}
