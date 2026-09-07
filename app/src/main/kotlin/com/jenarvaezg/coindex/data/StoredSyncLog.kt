package com.jenarvaezg.coindex.data

/** The preferences file the last sync record lives in. */
const val SYNC_LOG_PREFERENCES: String = "coindex-sync-log"

private const val KEY_AT = "last_sync_at"
private const val KEY_ITEMS = "last_sync_items"
private const val KEY_TYPES = "last_sync_types"
private const val KEY_CALLS = "last_sync_calls"
private const val KEY_PARTIAL = "last_sync_partial"

/**
 * The last [SyncRecord], on named values rather than in the database.
 *
 * It is a single row about the device, not about the collection, and putting it in Room would
 * cost a schema migration for something no query ever joins against.
 *
 * A plain class and no longer an interface with a fake of its own (#546): what writes it —
 * [CollectionSync] — still has to be readable without a device, because the record carries a
 * timestamp and a timestamp nobody can pin down is a line of the masthead nobody can test. That is
 * what a fake [NamedValues] is for.
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
                // The five keys by name and not a `clear()` of the file: the seam removes what it
                // is told to, and these five are everything this file has held since it was
                // written (#10) — no sixth key was ever added to it and later dropped.
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
