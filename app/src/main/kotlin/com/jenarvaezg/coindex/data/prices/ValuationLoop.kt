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
 * When a pass of the valuation is worth starting, and who gets the network when two things want it
 * (ADR 0028 §3, §6).
 *
 * The exact sibling of [com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop], with **one condition
 * removed and one made graver**:
 *
 * - **The wifi is gone.** What wifi protects over there is the data tariff — 30 MB of photographs
 *   nobody asked for — and what is scarce here is the API budget, which waiting for wifi does not
 *   protect at all. It is some 487 JSON responses.
 * - **A sync outranks it, and here it matters more than it did there.** The two spend the *same*
 *   monthly allowance, so a pass in flight can eat the calls the sync needs and make it fail with
 *   `BudgetExhausted`. [yieldNetwork] hands over and waits for the pass to unwind.
 *
 * The other three rules are the same: one pass at a time, nothing to do twice — the plan is compared
 * as the plan it is, not by a count that two changes could cancel out — and an export stands it down
 * entirely.
 */
class ValuationLoop(
    private val pass: ValuationPass,
    /**
     * Whether a sync is in flight, read when the pass starts rather than when it is asked for: three
     * seconds of a cold start is long enough for the collector to have pressed sync.
     */
    private val syncing: suspend () -> Boolean,
    private val startDelayMillis: Long = VALUATION_START_DELAY_MILLIS,
) {
    private var job: Job? = null

    /** The plan the last pass covered, so the same collection does not buy a second pass. */
    private var covered: ValuationPlan? = null

    private val _status = MutableStateFlow(ValuationStatus())

    /**
     * What this phone holds of the collection's prices, as far as the last pass got.
     *
     * Observed and not handed back, for the same reason the photograph count is: this outlives the
     * screen that started the pass. «Las cifras» opened on the second launch of a month gets no new
     * pass — the plan has not changed — so a status that travelled with the pass would leave the money
     * section absent over a phone that has every price it needs.
     */
    val status: StateFlow<ValuationStatus> = _status.asStateFlow()

    /**
     * Starts a pass unless one of the rules says not to.
     *
     * @param force starts one anyway, for the moment there is something new to ask about that the plan
     *   itself does not show: a sync that has just ended.
     */
    fun start(scope: CoroutineScope, plan: ValuationPlan, force: Boolean = false) {
        if (job?.isActive == true) return
        if (plan.isEmpty) return
        if (!force && covered == plan) return
        job = scope.launch {
            // The index is drawn first. A pass opens with a database read and two network calls, and
            // doing that while the first screen is still laying itself out is the cold start the
            // collector would feel.
            delay(startDelayMillis)
            val held = if (syncing()) ValuationRefusal.Syncing else null
            val status = pass.run(plan, held) { partial -> _status.value = partial }
            // Only a pass that was allowed to run has covered its plan. One held by a sync has asked
            // for nothing, and must not talk the next launch out of trying.
            if (held == null) covered = plan
            _status.value = status
        }
    }

    /**
     * Gives the network up without waiting, for an export that is about to take all four slots.
     *
     * Not joined, unlike [yieldNetwork]: the export does not spend API budget, so a pass unwinding
     * beside it collides with nothing.
     */
    fun cancel() {
        job?.cancel()
    }

    /**
     * Gives the budget up and waits for the pass to have finished unwinding.
     *
     * Waited for and not merely cancelled, and this is the one place this class is stricter than its
     * sibling: what the sync needs back is not bandwidth but **calls**, and a pass still unwinding can
     * still be inside `reserve()` taking one of them.
     */
    suspend fun yieldNetwork() {
        job?.cancelAndJoin()
        // The plan is forgotten, so the pass that follows the sync starts from scratch: what a
        // cancelled pass covered is unknown, and remembering it would strand whatever it never asked.
        covered = null
    }
}
