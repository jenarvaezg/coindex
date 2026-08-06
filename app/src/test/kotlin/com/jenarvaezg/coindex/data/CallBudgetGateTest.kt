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
