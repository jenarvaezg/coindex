package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallDao
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** First millisecond of the current calendar month in the device's own time zone. */
fun startOfMonthMillis(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
        .withDayOfMonth(1)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

/**
 * What the collector has spent of this month's API allowance, and the only reader of
 * `api_call_log`.
 *
 * There used to be three of them — the budget gate, the sync report and the index's budget line —
 * each pairing [startOfMonthMillis] with a clock of its own, and the third one reached the DAO by
 * walking from the UI through `AppContainer` into the database. «Llamadas gastadas este mes» is one
 * question with one answer, so it is asked here: the gate refuses on it, the sync report subtracts
 * two readings of it, and the budget line prints it.
 *
 * The month is the calendar month in the device's time zone, because that is the month the
 * collector's own allowance resets on — Numista counts by the month, not by rolling 30 days.
 */
class ApiCallLedger(
    private val apiCalls: ApiCallDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun spentThisMonth(): Int = apiCalls.countSince(startOfMonthMillis(nowMillis()))

    /** Writes down one call, stamped now. When it is written down is [CallBudgetGate]'s rule. */
    suspend fun record(endpoint: String) {
        apiCalls.record(ApiCallEntity(endpoint = endpoint, calledAt = nowMillis()))
    }
}
