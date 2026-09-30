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
 *
 * **The cap is not the same for everybody who asks** (#605): the valuation pass stops
 * [INVENTORY_RESERVE] short of it, so the two consultas the inventory costs are there in a month the
 * pass has otherwise emptied. Which ceiling applies is [ceilingFor]'s to say, and it reads the
 * endpoint — the one thing the gate is given and the one thing that already names the purpose.
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
                // The ceiling and not the budget: what the caller is told it ran out of is the
                // allowance it had, which for the pass is the one that stops short of the reserve.
                throw NumistaException.BudgetExhausted(used, ceiling)
            }
            calls.record(endpoint)
        }
    }
}
