package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallDao
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaException
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** First millisecond of the current calendar month in the device's own time zone. */
fun startOfMonthMillis(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone)
        .withDayOfMonth(1)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

/**
 * Counts every request against the monthly cap and records it *before* it is sent.
 *
 * The free API allows roughly 1.500-2.000 requests a month and it is easy to burn a month in
 * one debugging session, so the counter refusing a call is the normal, expected outcome once
 * the cap is reached — never a silent overrun.
 */
class CallBudgetGate(
    private val apiCalls: ApiCallDao,
    private val monthlyBudget: suspend () -> Int,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : CallBudget {
    private val mutex = Mutex()

    override suspend fun reserve(endpoint: String) {
        // Serialized so two concurrent syncs cannot both squeeze past the last slot.
        mutex.withLock {
            val now = nowMillis()
            val budget = monthlyBudget()
            val used = apiCalls.countSince(startOfMonthMillis(now))
            if (used >= budget) {
                throw NumistaException.BudgetExhausted(used, budget)
            }
            apiCalls.record(ApiCallEntity(endpoint = endpoint, calledAt = now))
        }
    }
}
