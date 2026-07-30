package com.jenarvaezg.coindex.data

import android.content.Context

private const val PREFS = "coindex-sync-log"
private const val KEY_AT = "last_sync_at"
private const val KEY_ITEMS = "last_sync_items"
private const val KEY_TYPES = "last_sync_types"
private const val KEY_CALLS = "last_sync_calls"
private const val KEY_PARTIAL = "last_sync_partial"

/**
 * The last [SyncRecord], on shared preferences rather than in the database.
 *
 * It is a single row about the device, not about the collection, and putting it in Room would
 * cost a schema migration for something no query ever joins against.
 */
class SyncLog(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var last: SyncRecord?
        get() {
            val at = prefs.getLong(KEY_AT, -1L).takeIf { it > 0 } ?: return null
            return SyncRecord(
                atMillis = at,
                collectionItems = prefs.getInt(KEY_ITEMS, 0),
                typesFetched = prefs.getInt(KEY_TYPES, 0),
                callsSpent = prefs.getInt(KEY_CALLS, 0),
                partialFailure = prefs.getString(KEY_PARTIAL, null),
            )
        }
        set(value) {
            if (value == null) {
                prefs.edit().clear().apply()
                return
            }
            prefs.edit()
                .putLong(KEY_AT, value.atMillis)
                .putInt(KEY_ITEMS, value.collectionItems)
                .putInt(KEY_TYPES, value.typesFetched)
                .putInt(KEY_CALLS, value.callsSpent)
                .putString(KEY_PARTIAL, value.partialFailure)
                .apply()
        }
}
