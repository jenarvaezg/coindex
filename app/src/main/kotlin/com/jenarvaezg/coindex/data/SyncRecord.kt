package com.jenarvaezg.coindex.data

/**
 * What the last sync left behind, kept beyond the snackbar that announced it: when it ran and
 * whether it finished is durable state, not a transient notice.
 */
data class SyncRecord(
    val atMillis: Long,
    val collectionItems: Int,
    val typesFetched: Int,
    val callsSpent: Int,
    val partialFailure: String? = null,
)
