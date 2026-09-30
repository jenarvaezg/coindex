package com.jenarvaezg.coindex.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which endpoint belongs to which purse, pinned against the strings `NumistaClient` actually builds.
 *
 * The classification is a substring match on a URL, which is the kind of rule that is right until
 * somebody adds a sixth endpoint. This test is the alarm: the five below are every path the client
 * asks for, copied from the five methods that build them, and a new one that is not here has quietly
 * been given the whole month.
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
