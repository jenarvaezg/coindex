package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Counts every request against the monthly cap and records it *before* it is sent.
 *
 * The free API allows roughly 1.500-2.000 requests a month and it is easy to burn a month in
 * one debugging session, so the counter refusing a call is the normal, expected outcome once
 * the cap is reached — never a silent overrun.
 */
class CallBudgetGate(
    private val calls: ApiCallLedger,
    private val monthlyBudget: suspend () -> Int,
) : CallBudget {
    private val mutex = Mutex()

    override suspend fun reserve(endpoint: String) {
        // Serialized so two concurrent syncs cannot both squeeze past the last slot.
        mutex.withLock {
            val budget = monthlyBudget()
            val used = calls.spentThisMonth()
            if (used >= budget) {
                throw NumistaException.BudgetExhausted(used, budget)
            }
            calls.record(endpoint)
        }
    }
}
