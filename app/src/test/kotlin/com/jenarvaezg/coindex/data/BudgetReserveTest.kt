package com.jenarvaezg.coindex.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The endpoint classification is a substring match on the URLs `NumistaClient` builds. The five
 * below are every path it asks for; a new endpoint missing here silently gets the whole month.
 */
class BudgetReserveTest {
    @Test
    fun `only the two endpoints of the valuation pass are capped`() {
        assertTrue(isValuationCall("/types/30/issues"))
        assertTrue(isValuationCall("/types/30/issues/297/prices"))
        assertFalse(isValuationCall("/oauth_token"))
        assertFalse(isValuationCall("/users/568461/collected_items"))
        assertFalse(isValuationCall("/types/404044"))
    }

    @Test
    fun `the pass stops short of the reserve and everything else does not`() {
        assertEquals(1_200, ceilingFor("/types/30/issues/297/prices", 1_500))
        assertEquals(1_500, ceilingFor("/users/568461/collected_items", 1_500))
    }

    /** A budget under the reserve leaves the pass at zero, never below it. */
    @Test
    fun `a budget smaller than the reserve is all reserved`() {
        assertEquals(0, ceilingFor("/types/30/issues", 100))
        assertEquals(100, ceilingFor("/oauth_token", 100))
    }
}
