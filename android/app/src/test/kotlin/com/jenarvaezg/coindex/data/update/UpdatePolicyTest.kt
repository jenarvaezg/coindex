package com.jenarvaezg.coindex.data.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdatePolicyTest {
    private val hour = 60 * 60 * 1_000L

    @Test
    fun `the first check always runs`() {
        assertTrue(shouldCheckForUpdate(lastCheckMillis = null, nowMillis = 0))
    }

    @Test
    fun `returning to the app does not check again within the interval`() {
        val lastCheck = 100 * hour

        assertFalse(shouldCheckForUpdate(lastCheck, lastCheck))
        assertFalse(shouldCheckForUpdate(lastCheck, lastCheck + hour))
        assertFalse(shouldCheckForUpdate(lastCheck, lastCheck + 5 * hour))
    }

    @Test
    fun `once the interval has passed it checks again`() {
        val lastCheck = 100 * hour

        assertTrue(shouldCheckForUpdate(lastCheck, lastCheck + 6 * hour))
        assertTrue(shouldCheckForUpdate(lastCheck, lastCheck + 20 * hour))
    }

    @Test
    fun `a clock that jumped backwards never locks checking out`() {
        val lastCheck = 100 * hour

        assertTrue(shouldCheckForUpdate(lastCheck, nowMillis = 10 * hour))
    }

    @Test
    fun `the interval is configurable for callers that want a tighter loop`() {
        val lastCheck = 100 * hour

        assertTrue(shouldCheckForUpdate(lastCheck, lastCheck + hour, intervalMillis = hour))
        assertFalse(
            shouldCheckForUpdate(lastCheck, lastCheck + hour / 2, intervalMillis = hour),
        )
    }
}
