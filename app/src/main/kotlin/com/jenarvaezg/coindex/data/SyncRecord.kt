package com.jenarvaezg.coindex.data

/**
 * What the last synchronization left behind, kept beyond the snackbar that announced it.
 *
 * A sync spends real API budget, so «when was the last one and did it finish» is durable state
 * of the collection, not a transient notice: the report used to live for four seconds and then
 * there was no way to tell a collection synced this morning from one synced in March.
 */
data class SyncRecord(
    val atMillis: Long,
    val collectionItems: Int,
    val typesFetched: Int,
    val callsSpent: Int,
    val partialFailure: String? = null,
)
