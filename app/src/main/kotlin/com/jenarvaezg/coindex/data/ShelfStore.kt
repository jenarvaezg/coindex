package com.jenarvaezg.coindex.data

import android.content.Context
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.ShelfCodec

private const val PREFS = "coindex-shelves"

/**
 * The filters and the sort of the two hierarchies, across launches (ADR 0021 §1).
 *
 * On shared preferences rather than in Room, for the same reason as [SyncLog]: it is a handful of
 * values about this device and this collector's last look at their own notebook, and no query ever
 * joins against them. Nothing here is per card either, so ADR 0021 §7 is untouched — a filter is
 * what the collector is looking through, not something stored about a collection.
 */
class ShelfStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var index: IndexShelf
        get() = ShelfCodec.decodeIndex { key -> prefs.getString(key, null) }
        set(value) = write(ShelfCodec.encode(value))

    var coins: CoinsShelf
        get() = ShelfCodec.decodeCoins { key -> prefs.getString(key, null) }
        set(value) = write(ShelfCodec.encode(value))

    private fun write(values: Map<String, String?>) {
        prefs.edit().apply {
            // A null is a removal and not an empty string: a blank country would read back as a
            // filter for a country nobody is called.
            values.forEach { (key, value) -> if (value == null) remove(key) else putString(key, value) }
        }.apply()
    }
}
