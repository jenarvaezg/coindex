package com.jenarvaezg.coindex.data

/** The preferences file the last sync record lives in. */
const val SYNC_LOG_PREFERENCES: String = "coindex-sync-log"

private const val KEY_AT = "last_sync_at"
private const val KEY_ITEMS = "last_sync_items"
private const val KEY_TYPES = "last_sync_types"
private const val KEY_CALLS = "last_sync_calls"
private const val KEY_PARTIAL = "last_sync_partial"

/**
 * The last [SyncRecord], on named values rather than in Room: one row about the device that no
 * query joins, not worth a schema migration. Tested through a fake [NamedValues] (#546).
 */
class StoredSyncLog(private val values: NamedValues) {
    var last: SyncRecord?
        get() {
            val at = values.int64(KEY_AT)?.takeIf { it > 0 } ?: return null
            return SyncRecord(
                atMillis = at,
                collectionItems = values.int32(KEY_ITEMS) ?: 0,
                typesFetched = values.int32(KEY_TYPES) ?: 0,
                callsSpent = values.int32(KEY_CALLS) ?: 0,
                partialFailure = values.text(KEY_PARTIAL),
            )
        }
        set(value) = values.write(
            if (value == null) {
                // Removed by name: the seam has no `clear()`, and these five keys are all the
                // file has ever held (#10).
                mapOf(
                    KEY_AT to null,
                    KEY_ITEMS to null,
                    KEY_TYPES to null,
                    KEY_CALLS to null,
                    KEY_PARTIAL to null,
                )
            } else {
                mapOf(
                    KEY_AT to Stored.Int64(value.atMillis),
                    KEY_ITEMS to Stored.Int32(value.collectionItems),
                    KEY_TYPES to Stored.Int32(value.typesFetched),
                    KEY_CALLS to Stored.Int32(value.callsSpent),
                    KEY_PARTIAL to value.partialFailure?.let(Stored::Text),
                )
            },
        )
}
