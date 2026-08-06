package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.NumistaClient

/**
 * What one sync attempt left behind.
 *
 * A record or a throwable, and nothing said in words: the sentences the collector reads are the
 * screen's ([com.jenarvaezg.coindex.ui.syncReportLabel], [com.jenarvaezg.coindex.ui.syncErrorLabel]),
 * and a failure that arrived here already flattened into prose could no longer be told apart by the
 * one thing that matters — an exhausted budget reads differently from a dead network.
 */
sealed interface SyncOutcome {
    data class Done(val record: SyncRecord) : SyncOutcome

    data class Failed(val error: Throwable) : SyncOutcome
}

/**
 * One explicit sync, from the collector's tap to the record it leaves behind.
 *
 * [SyncService] does the work; what lives here is the sentence around it that used to be inline in
 * the ViewModel: **stamp it, write it down, then announce it**. The stamp is the reason this is a
 * class with a clock rather than three lines at the call site — `System.currentTimeMillis()` read in
 * place is what made «la última sincronización fue ayer» impossible to test, while the label that
 * prints it has taken an injected clock since the day it was written.
 *
 * The log is written **before** the outcome is returned, and that order is the point: the snackbar
 * is the copy, not the original. An app killed in the second between them still shows the collector,
 * on the next launch, that the sync happened.
 */
class CollectionSync(
    private val syncService: SyncService,
    private val syncLog: SyncLog,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    /** The last sync there was, so a launch opens on it instead of on a blank line. */
    val last: SyncRecord? get() = syncLog.last

    suspend fun run(client: NumistaClient, userId: Long): SyncOutcome {
        val outcome = runCatching { syncService.run(client, userId) }
        return outcome.fold(
            onSuccess = { report ->
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
            // Nothing is written down: a sync that failed did not happen, and the record of the
            // last one that did is exactly what the collector still needs to see.
            onFailure = { error -> SyncOutcome.Failed(error) },
        )
    }
}
