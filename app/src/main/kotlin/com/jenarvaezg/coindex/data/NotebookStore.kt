package com.jenarvaezg.coindex.data

import android.content.Context
import com.jenarvaezg.coindex.ui.print.NotebookCodec
import com.jenarvaezg.coindex.ui.print.NotebookOptions

private const val PREFS = "coindex-notebook"

/**
 * How the collector prints their notebook, across launches (#228).
 *
 * On shared preferences rather than in Room, for the same reason as [ShelfStore] and [SyncLog]: it is
 * five booleans about this device and this collector's last trip to the printer, and no query ever
 * joins against them. Nothing here is per card either, so ADR 0021 §7 is untouched — how a notebook
 * is printed is what the collector is looking through, not something stored about a collection.
 *
 * Its own preferences file and not a sixth property of [ShelfStore], because these are not a shelf:
 * a filter decides *which* collections come out, and these decide *what the paper looks like*.
 */
class NotebookStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var options: NotebookOptions
        // `contains` and not `getBoolean(key, default)`: an absent key has to reach the codec as
        // absent, so the default it reads back is the switch's own — and the default of «fotos» is
        // on. A missing key silently becoming false is a notebook with no coins in it.
        get() = NotebookCodec.decode { key ->
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        }
        set(value) {
            prefs.edit().apply {
                NotebookCodec.encode(value).forEach { (key, on) -> putBoolean(key, on) }
            }.apply()
        }
}
