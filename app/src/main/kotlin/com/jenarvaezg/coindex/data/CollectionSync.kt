package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.NumistaClient
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What one sync attempt left behind: a record or a throwable, never prose. The screen words them
 * ([com.jenarvaezg.coindex.ui.syncReportLabel], [com.jenarvaezg.coindex.ui.syncErrorLabel]) and
 * needs the error's type to tell an exhausted budget from a dead network.
 */
sealed interface SyncOutcome {
    data class Done(val record: SyncRecord) : SyncOutcome

    data class Failed(val error: Throwable) : SyncOutcome
}

/**
 * One sync, from the tap to the record it leaves behind. [SyncService] does the work; this stamps
 * the result with an injectable clock, writes it to the log and then returns it, so an app killed
 * before the snackbar still shows the sync on the next launch.
 */
class CollectionSync(
    private val syncService: SyncService,
    private val syncLog: StoredSyncLog,
    /**
     * The valuation pass's refusal, cleared by a sync that got through (#600): reaching Numista
     * proves the wall is gone, and syncing is what the collector does when something looks stuck.
     */
    private val wall: RejectionWall,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    /** The last sync there was, so a launch opens on it instead of on a blank line. */
    val last: SyncRecord? get() = syncLog.last

    private val running = AtomicBoolean(false)

    /**
     * Whether a sync is running, for whoever else spends the same budget (ADR 0028 §6). Atomic
     * because the valuation pass reads it from `Dispatchers.IO`.
     */
    val inFlight: Boolean get() = running.get()

    suspend fun run(client: NumistaClient, userId: Long, maxFichas: Int = Int.MAX_VALUE): SyncOutcome {
        running.set(true)
        val outcome = runCatching { syncService.run(client, userId, maxFichas) }
        running.set(false)
        return outcome.fold(
            onSuccess = { report ->
                wall.clear()
                val record = SyncRecord(
                    atMillis = nowMillis(),
                    collectionItems = report.collectionItems,
                    typesFetched = report.typesFetched,
                    callsSpent = report.callsSpent,
                    partialFailure = report.partialFailure,
                )
                syncLog.last = record
                SyncOutcome.Done(record)
            },
            // A failed sync leaves the log alone, so it keeps showing the last one that worked.
            onFailure = { error -> SyncOutcome.Failed(error) },
        )
    }
}
