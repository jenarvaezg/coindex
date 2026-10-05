package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The valuation card on «Este teléfono» opens «Credenciales» only in the two states that blame the
 * key (ADR 0028 §6.1, #521); the other four can only be waited for.
 */
class ValuationDoorTest {
    private fun held(refusal: ValuationRefusal?) =
        ValuationStatus(wanted = 223, missing = 83, held = refusal)

    @Test
    fun `the two states that blame the key open the credentials`() {
        assertTrue(valuationBlamesCredentials(held(ValuationRefusal.NoApiKey)))
        assertTrue(valuationBlamesCredentials(held(ValuationRefusal.Rejected)))
    }

    /** A door here would lead to fields that are already right and devalue the real one. */
    @Test
    fun `the four states that can only be waited for open nothing`() {
        assertFalse(valuationBlamesCredentials(held(null)))
        assertFalse(valuationBlamesCredentials(held(ValuationRefusal.Syncing)))
        assertFalse(valuationBlamesCredentials(held(ValuationRefusal.BudgetExhausted)))
        assertFalse(valuationBlamesCredentials(held(ValuationRefusal.Offline)))
    }

    /**
     * The door follows the line, not `held`: with nothing to price or the prices settled,
     * `valuationLabel` shows no refusal.
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
