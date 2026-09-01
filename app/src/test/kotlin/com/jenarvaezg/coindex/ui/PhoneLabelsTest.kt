package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The door «Este teléfono» grows into «Credenciales» (ADR 0028 §6.1, #521).
 *
 * Two of the six states of the valuation card have a cause the collector can act on, and the audit
 * of 14 August 2026 asked for the notices to become doors. This is the answer measured: four of the
 * six say «wait» — for the network, for the sync, for the 1st of the month — and the pass has no
 * handle by design, so only the two that blame the key open anything.
 */
class ValuationDoorTest {
    private fun held(refusal: ValuationRefusal?) =
        ValuationStatus(wanted = 223, missing = 83, held = refusal)

    @Test
    fun `the two states that blame the key open the credentials`() {
        assertTrue(valuationBlamesCredentials(held(ValuationRefusal.NoApiKey)))
        assertTrue(valuationBlamesCredentials(held(ValuationRefusal.Rejected)))
    }

    /**
     * The four that can only be waited for print no row.
     *
     * A door on these would lead to the two fields that are already right, which teaches the
     * collector that the screen it opens is where you go when anything is missing — and the next
     * time the key really is wrong, the row means nothing.
     */
    @Test
    fun `the four states that can only be waited for open nothing`() {
        assertFalse(valuationBlamesCredentials(held(null)))
        assertFalse(valuationBlamesCredentials(held(ValuationRefusal.Syncing)))
        assertFalse(valuationBlamesCredentials(held(ValuationRefusal.BudgetExhausted)))
        assertFalse(valuationBlamesCredentials(held(ValuationRefusal.Offline)))
    }

    /**
     * The door follows the line, not the state underneath it.
     *
     * `valuationLabel` returns before its refusal branch with nothing to price and with the prices
     * settled, so a `held` surviving in either case is a refusal nobody is being told about — and a
     * row hanging under «Los precios están al día» would be a complaint with no complaint above it.
     */
    @Test
    fun `no door hangs under a line that is not complaining`() {
        assertFalse(
            valuationBlamesCredentials(
                ValuationStatus(wanted = 0, missing = 0, held = ValuationRefusal.NoApiKey),
            ),
        )
        assertFalse(
            valuationBlamesCredentials(
                ValuationStatus(wanted = 223, missing = 0, held = ValuationRefusal.Rejected),
            ),
        )
    }
}
