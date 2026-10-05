package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Counts every request against the monthly cap and records it before it is sent, so reaching the
 * cap refuses the call instead of overrunning (ADR 0003). The valuation pass stops
 * [INVENTORY_RESERVE] short of the cap (#605); [ceilingFor] picks the ceiling from the endpoint.
 */
class CallBudgetGate(
    private val calls: ApiCallLedger,
    private val monthlyBudget: suspend () -> Int,
) : CallBudget {
    private val mutex = Mutex()

    override suspend fun reserve(endpoint: String) {
        // Serialized so two concurrent syncs cannot both squeeze past the last slot.
        mutex.withLock {
            val ceiling = ceilingFor(endpoint, monthlyBudget())
            val used = calls.spentThisMonth()
            if (used >= ceiling) {
                // The caller's own ceiling, which for the pass stops short of the reserve.
                throw NumistaException.BudgetExhausted(used, ceiling)
            }
            calls.record(endpoint)
        }
    }
}
