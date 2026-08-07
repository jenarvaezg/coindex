package com.jenarvaezg.coindex.data

import android.content.Context

/**
 * A handful of named strings that survive a launch, and nothing else.
 *
 * What used to be here was [StoredShelves][com.jenarvaezg.coindex.ui.shelf.StoredShelves] itself,
 * which meant the data layer imported `CoinsShelf`, `IndexShelf` and `ShelfCodec` out of
 * `ui.shelf` to know its own storage format — presentation deciding what persistence looks like,
 * the dependency exactly backwards (#221). What actually needed a device was never the shelf: it
 * was these five lines of `SharedPreferences`.
 *
 * A null value is a **removal** and not an empty string, which is the one rule of this format that
 * is not obvious: a blank country would read back as a filter for a country nobody is called.
 */
interface NamedValues {
    fun read(key: String): String?

    fun write(values: Map<String, String?>)
}

/** [NamedValues] on one shared preferences file. */
class SharedPreferenceValues(context: Context, name: String) : NamedValues {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun write(values: Map<String, String?>) {
        prefs.edit().apply {
            values.forEach { (key, value) ->
                if (value == null) remove(key) else putString(key, value)
            }
        }.apply()
    }
}
