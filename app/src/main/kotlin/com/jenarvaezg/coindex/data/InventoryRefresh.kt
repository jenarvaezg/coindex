package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.NumistaClient
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

/** How long the first screen gets the phone to itself; a second less than the valuation pass's. */
private const val INVENTORY_START_DELAY_MILLIS = 2_000L

/**
 * How many new fichas an automatic refresh fetches before leaving the rest for later. Without it a
 * cache emptied by a reinstall would buy hundreds of fichas just because the app was opened. Ten
 * covers an ordinary batch of purchases; the rest waits for the next refresh or for «Sincronizar»,
 * which has no limit because the collector asked for it.
 */
const val AUTOMATIC_FICHA_LIMIT: Int = 10

/**
 * The inventory brought up to date because a day passed, without a press (#605): it costs two
 * calls, far less than the valuation pass that already runs on every launch.
 *
 * Like [com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop] and
 * [com.jenarvaezg.coindex.data.prices.ValuationLoop], it never reports errors and starts after the
 * first screen is drawn, a second before the pass, so the pass finds [inFlight] raised and holds
 * instead of being cancelled halfway.
 */
class InventoryRefresh(
    private val sync: CollectionSync,
    /** The refusal the pass wrote down (#579): same key, same month, so no point asking. */
    private val wall: RejectionWall,
    private val startDelayMillis: Long = INVENTORY_START_DELAY_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val claimed = AtomicBoolean(false)

    /**
     * Whether a refresh is about to happen or is happening. Raised before the delay, unlike
     * [CollectionSync.inFlight], so the valuation pass sees a refresh still waiting out its delay
     * and holds.
     */
    val inFlight: Boolean get() = claimed.get()

    /** Whether the inventory is old enough and Numista is not refusing. Costs nothing to ask. */
    fun due(): Boolean = wall.standing() == null && inventoryIsStale(sync.last?.atMillis, nowMillis())

    /**
     * Brings the inventory up to date if it is time, and says whether it got through. Returns false
     * without asking when it isn't due, Numista is refusing or a refresh is already claimed. Sync
     * errors are swallowed on purpose: their messages (`syncErrorLabel`) belong to a manual sync,
     * not to a refresh the collector never requested.
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
