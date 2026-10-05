package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class CallBudgetGateTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun millis(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, madrid).toInstant().toEpochMilli()

    @Test
    fun `the counter refuses the call that would cross the cap`() = runTest {
        val dao = FakeApiCallDao()
        val now = millis(2026, 7, 30)
        val gate = CallBudgetGate(ApiCallLedger(dao) { now }, monthlyBudget = { 2 })

        gate.reserve("/types/1")
        gate.reserve("/types/2")
        val error = assertFailsWith<NumistaException.BudgetExhausted> { gate.reserve("/types/3") }

        assertEquals(2, error.used)
        assertEquals(2, error.budget)
        assertEquals(2, dao.calls.size)
    }

    @Test
    fun `each call is recorded before it is sent so the counter cannot drift low`() = runTest {
        val dao = FakeApiCallDao()
        val now = millis(2026, 7, 30)
        val gate = CallBudgetGate(ApiCallLedger(dao) { now }, monthlyBudget = { 10 })

        gate.reserve("/oauth_token")

        assertEquals(listOf("/oauth_token"), dao.calls.map { it.endpoint })
        assertEquals(now, dao.calls.single().calledAt)
    }

    /**
     * The shape of August 2026, and the reason the reserve exists (#605).
     *
     * On the 11th the father's phone spent 1.484 consultas in one day against the same plan (#560),
     * hit the cap, and from then until the 1st the gate refused **everything** — including the two
     * that would have told him what he owned. His inventory stayed frozen from 10 August. That bug is
     * fixed; the coupling that let a bug in the expensive thing freeze the cheap thing is what this
     * pins.
     */
    @Test
    fun `a month the pass has emptied still has the inventory's consultas in it`() = runTest {
        val dao = FakeApiCallDao()
        val now = millis(2026, 8, 11)
        val gate = CallBudgetGate(ApiCallLedger(dao) { now }, monthlyBudget = { DEFAULT_MONTHLY_BUDGET })
        repeat(DEFAULT_MONTHLY_BUDGET - INVENTORY_RESERVE) {
            dao.calls += ApiCallEntity(endpoint = "/types/30/issues/$it/prices", calledAt = now)
        }

        assertFailsWith<NumistaException.BudgetExhausted> { gate.reserve("/types/30/issues/1/prices") }
        assertFailsWith<NumistaException.BudgetExhausted> { gate.reserve("/types/30/issues") }
        gate.reserve("/oauth_token")
        gate.reserve("/users/568461/collected_items")
        gate.reserve("/types/404044")

        assertEquals(
            DEFAULT_MONTHLY_BUDGET - INVENTORY_RESERVE + 3,
            dao.calls.size,
            "las tres del sync pasan y las dos del pase no",
        )
    }

    /** And the reserve is a floor and not a second month: the pass keeps 1.200 of the 1.500. */
    @Test
    fun `the pass spends everything up to the reserve`() = runTest {
        val dao = FakeApiCallDao()
        val now = millis(2026, 8, 11)
        val gate = CallBudgetGate(ApiCallLedger(dao) { now }, monthlyBudget = { DEFAULT_MONTHLY_BUDGET })
        repeat(DEFAULT_MONTHLY_BUDGET - INVENTORY_RESERVE - 1) {
            dao.calls += ApiCallEntity(endpoint = "/types/30/issues/$it/prices", calledAt = now)
        }

        gate.reserve("/types/30/issues/999/prices")

        val error = assertFailsWith<NumistaException.BudgetExhausted> {
            gate.reserve("/types/30/issues/1000/prices")
        }
        assertEquals(DEFAULT_MONTHLY_BUDGET - INVENTORY_RESERVE, error.budget, "el pase ve su techo, no el del mes")
    }

    @Test
    fun `last month's calls do not count against this month's budget`() = runTest {
        val dao = FakeApiCallDao()
        dao.calls += ApiCallEntity(endpoint = "/types/1", calledAt = millis(2026, 6, 30))
        val now = millis(2026, 7, 1)
        val gate = CallBudgetGate(ApiCallLedger(dao) { now }, monthlyBudget = { 1 })

        gate.reserve("/types/2")

        assertEquals(2, dao.calls.size)
    }
}
