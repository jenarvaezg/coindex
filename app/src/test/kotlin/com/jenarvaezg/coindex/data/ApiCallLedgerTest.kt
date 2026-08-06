package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallEntity
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ApiCallLedgerTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun millis(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, madrid).toInstant().toEpochMilli()

    @Test
    fun `what is spent this month starts counting on the first day of the month`() = runTest {
        val dao = FakeApiCallDao()
        dao.calls += ApiCallEntity(endpoint = "/types/1", calledAt = millis(2026, 6, 30))
        dao.calls += ApiCallEntity(endpoint = "/types/2", calledAt = millis(2026, 7, 1))
        val ledger = ApiCallLedger(dao) { millis(2026, 7, 30) }

        assertEquals(1, ledger.spentThisMonth())
    }

    @Test
    fun `a recorded call is stamped with the ledger's own clock`() = runTest {
        val dao = FakeApiCallDao()
        val now = millis(2026, 7, 30)
        val ledger = ApiCallLedger(dao) { now }

        ledger.record("/oauth_token")

        assertEquals(listOf("/oauth_token"), dao.calls.map { it.endpoint })
        assertEquals(now, dao.calls.single().calledAt)
    }

    @Test
    fun `the month starts at midnight local time on the first day`() {
        val start = startOfMonthMillis(millis(2026, 7, 30), madrid)

        assertEquals(
            ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, madrid).toInstant().toEpochMilli(),
            start,
        )
    }
}
