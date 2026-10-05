package com.jenarvaezg.coindex.data

import android.content.Context

/**
 * A value that survives a launch, in the shape the file keeps it. Shared preferences stores a typed
 * entry per key, and reading a key as another type throws, so the type is part of what is stored.
 * [Int32] and [Int64] are distinct entries: widening a stored number is a migration, not an edit.
 */
sealed interface Stored {
    data class Text(val value: String) : Stored

    data class Flag(val value: Boolean) : Stored

    data class Int32(val value: Int) : Stored

    data class Int64(val value: Long) : Stored
}

/**
 * A handful of named values that survive a launch: the one seam over `SharedPreferences`, so the
 * stores built on it are plain classes testable on the JVM with a single fake (#221, #546).
 *
 * A null value is a removal, not an empty string: a blank country would read back as a filter for a
 * country nobody is called.
 */
interface NamedValues {
    fun read(key: String): Stored?

    fun write(values: Map<String, Stored?>)
}

/** The text under [key], or null when nothing, or something that isn't text, is stored there. */
fun NamedValues.text(key: String): String? = (read(key) as? Stored.Text)?.value

/** The flag under [key], where absent is absent and never «off». */
fun NamedValues.flag(key: String): Boolean? = (read(key) as? Stored.Flag)?.value

/** The 32-bit number under [key]. */
fun NamedValues.int32(key: String): Int? = (read(key) as? Stored.Int32)?.value

/** The 64-bit number under [key]. */
fun NamedValues.int64(key: String): Long? = (read(key) as? Stored.Int64)?.value

/** Writes a codec's texts, where a null is a removal. */
fun NamedValues.writeText(values: Map<String, String?>) {
    write(values.mapValues { (_, value) -> value?.let(Stored::Text) })
}

/** Writes a codec's flags, all of them present. */
fun NamedValues.writeFlags(values: Map<String, Boolean>) {
    write(values.mapValues { (_, on) -> Stored.Flag(on) })
}

/** [NamedValues] on one shared preferences file. */
class SharedPreferenceValues(context: Context, name: String) : NamedValues {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    /**
     * Read off the whole map rather than through `getString` and its siblings: a typed getter on a
     * key of another type throws, while the map gives a null, so a key an older version wrote in
     * another shape falls back to the store's default. Copying the map is cheap for these few keys.
     */
    override fun read(key: String): Stored? = when (val stored = prefs.all[key]) {
        is String -> Stored.Text(stored)
        is Boolean -> Stored.Flag(stored)
        is Int -> Stored.Int32(stored)
        is Long -> Stored.Int64(stored)
        else -> null
    }

    override fun write(values: Map<String, Stored?>) {
        prefs.edit().apply {
            values.forEach { (key, value) ->
                when (value) {
                    null -> remove(key)
                    is Stored.Text -> putString(key, value.value)
                    is Stored.Flag -> putBoolean(key, value.value)
                    is Stored.Int32 -> putInt(key, value.value)
                    is Stored.Int64 -> putLong(key, value.value)
                }
            }
        }.apply()
    }
}
