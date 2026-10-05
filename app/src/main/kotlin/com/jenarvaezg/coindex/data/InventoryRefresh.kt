package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.NumistaClient
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

/** How long the first screen gets the phone to itself, a second before the pass of ADR 0028 §3. */
private const val INVENTORY_START_DELAY_MILLIS = 2_000L

/**
 * How many fichas an automatic refresh buys before it leaves the rest for a press.
 *
 * A refresh brings the inventory first and the fichas of what is new after it, and the second half
 * is the one with no ceiling of its own: a cache emptied by a reinstall would have the phone buy two
 * hundred fichas because somebody opened the app. Ten covers every batch either collector has ever
 * bought at once — four of the father's 191 types were missing from the seeded cache — and what it
 * leaves over waits for tomorrow's refresh or for «Sincronizar», which has no limit because somebody
 * asked for it and is watching it.
 */
const val AUTOMATIC_FICHA_LIMIT: Int = 10

/**
 * The inventory brought up to date because a day passed, not because anybody pressed anything (#605).
 *
 * The cheap thing had the manual clock and the expensive one had the automatic clock, which is the
 * wrong way round: a refresh of the inventory is **two consultas** and the valuation pass is 442, and
 * the pass is the one that ran on every launch. What that cost is in the father's `api_call_log`: his
 * last `/users/{id}/collected_items` is dated 10 August 2026, and the app was still being opened
 * every day.
 *
 * It is the sibling of [com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop] and of
 * [com.jenarvaezg.coindex.data.prices.ValuationLoop], and it keeps their two rules that matter:
 * **it never speaks** — a refresh that could not reach Numista is not an error worth interrupting a
 * collection with — and **it starts after the first screen is drawn**, a second before the pass so
 * the pass finds [inFlight] already raised and holds instead of being cancelled halfway.
 */
class InventoryRefresh(
    private val sync: CollectionSync,
    /**
     * The refusal the pass wrote down (#579), read here because this is the same key and the same
     * month. Two consultas against a wall that is standing is exactly the rediscovery that ticket
     * removed, and an automatic refresh would commit it once a day for nothing.
     */
    private val wall: RejectionWall,
    private val startDelayMillis: Long = INVENTORY_START_DELAY_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val claimed = AtomicBoolean(false)

    /**
     * Whether a refresh is about to happen or is happening, asked by whoever spends the same budget.
     *
     * Raised **before** the delay and not when the call goes out, which is the whole reason it exists
     * apart from [CollectionSync.inFlight]: the valuation pass reads this three seconds into a launch
     * to decide whether to hold, and a refresh that is still waiting out its two seconds would
     * otherwise look like nothing at all and get its budget taken.
     */
    val inFlight: Boolean get() = claimed.get()

    /** Whether the inventory is old enough and Numista is not refusing. Costs nothing to ask. */
    fun due(): Boolean = wall.standing() == null && inventoryIsStale(sync.last?.atMillis, nowMillis())

    /**
     * Brings the inventory up to date if it is time, and says whether it got through.
     *
     * Returns false without asking anything when it is not time, when Numista is refusing, or when a
     * refresh is already claimed. The outcome of the sync itself is **swallowed** on purpose: the
     * exhausted month, the dead network and the key that stopped working all have a sentence already,
     * and every one of them belongs to the press that asked for it (`syncErrorLabel`) and not to a
     * refresh the collector never requested.
     */
    suspend fun run(client: NumistaClient, userId: Long): Boolean {
        if (!due()) return false
        if (!claimed.compareAndSet(false, true)) return false
        return try {
            delay(startDelayMillis)
            sync.run(client, userId, maxFichas = AUTOMATIC_FICHA_LIMIT) is SyncOutcome.Done
        } finally {
            claimed.set(false)
        }
    }
}
