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
 * What has been spent of this month's API allowance. The only reader of `api_call_log`, so the
 * budget gate, the sync report and the budget line count the same month: the calendar month in the
 * device's time zone, since Numista counts by calendar month, not rolling 30 days.
 */
class ApiCallLedger(
    private val apiCalls: ApiCallDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun spentThisMonth(): Int = apiCalls.countSince(startOfMonthMillis(nowMillis()))

    /** Records one call, stamped now. [CallBudgetGate] records it before sending. */
    suspend fun record(endpoint: String) {
        apiCalls.record(ApiCallEntity(endpoint = endpoint, calledAt = nowMillis()))
    }
}
