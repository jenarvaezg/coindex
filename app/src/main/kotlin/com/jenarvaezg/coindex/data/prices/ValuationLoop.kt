package com.jenarvaezg.coindex.data.prices

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How long the valuation pass lets the first screen have the phone to itself (ADR 0028 §3). */
private const val VALUATION_START_DELAY_MILLIS = 3_000L

/**
 * When a valuation pass is worth starting, and who gets the network when two things want it
 * (ADR 0028 §3, §6). Like [com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop] (one pass at a
 * time, never the same plan twice, an export stands it down), except:
 *
 * - No wifi condition: the scarce resource is the API budget, not mobile data, and the responses
 *   are small JSON.
 * - A sync outranks it more strictly: both spend the same monthly allowance, so [yieldNetwork]
 *   waits for the pass to unwind before the sync starts spending.
 *
 * The plan is compared whole, not by a count that two changes could cancel out.
 */
class ValuationLoop(
    private val pass: ValuationPass,
    /** Whether a sync is in flight, read when the pass starts, after the start delay. */
    private val syncing: suspend () -> Boolean,
    private val startDelayMillis: Long = VALUATION_START_DELAY_MILLIS,
) {
    private var job: Job? = null

    /** The plan the last pass covered, so the same collection doesn't get a second pass. */
    private var covered: ValuationPlan? = null

    private val _status = MutableStateFlow(ValuationStatus())

    /**
     * What this phone holds of the collection's prices, as far as the last pass got. Observed rather
     * than returned because it must outlive the screen: a later launch with an unchanged plan runs
     * no pass, and «Las cifras» still needs the status.
     */
    val status: StateFlow<ValuationStatus> = _status.asStateFlow()

    /**
     * Starts a pass unless one of the rules says not to.
     *
     * @param force starts one even for an unchanged plan, as after a sync that just ended.
     */
    fun start(scope: CoroutineScope, plan: ValuationPlan, force: Boolean = false) {
        if (job?.isActive == true) return
        if (plan.isEmpty) return
        if (!force && covered == plan) return
        job = scope.launch {
            // Lets the first screen draw before the pass reads the database and the network.
            delay(startDelayMillis)
            val held = if (syncing()) ValuationRefusal.Syncing else null
            val status = pass.run(plan, held) { partial -> _status.value = partial }
            // A pass held by a sync asked for nothing, so it hasn't covered its plan.
            if (held == null) covered = plan
            _status.value = status
        }
    }

    /**
     * Runs one pass now, for the gesture that values a plate of the shelf window (ADR 0030 §3).
     * Unlike [start]: no delay, since the collector is waiting; no `covered` memory, since this plan
     * isn't the collection's; and a background pass in flight is cancelled first rather than
     * skipping, so the button never silently does nothing. The launch pass comes back later.
     *
     * The returned refusal is for the gesture to show; a held pass wrote nothing (ADR 0028 §4). The
     * result is never published to [status]: this plan owns no issue, so it would read as `settled`
     * and open the money section of «Las cifras» (ADR 0028 §7).
     */
    suspend fun valueNow(plan: ValuationPlan): ValuationStatus {
        if (plan.isEmpty) return _status.value
        yieldNetwork()
        val held = if (syncing()) ValuationRefusal.Syncing else null
        return pass.run(plan, held)
    }

    /**
     * Gives the network up without waiting, for an export about to take all four slots. Not joined:
     * the export spends no API budget.
     */
    fun cancel() {
        job?.cancel()
    }

    /**
     * Gives the budget up and waits for the pass to finish unwinding: a pass still unwinding can be
     * inside `reserve()` taking a call the sync needs.
     */
    suspend fun yieldNetwork() {
        job?.cancelAndJoin()
        // What a cancelled pass covered is unknown, so the next pass starts from scratch.
        covered = null
    }
}
