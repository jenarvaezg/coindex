package com.jenarvaezg.coindex.data

import android.content.Context

/**
 * A value that survives a launch, in the shape the file keeps it.
 *
 * Four shapes and not one string, because shared preferences does not store text: it stores a
 * **typed** entry per key, and a key written as a boolean and read as a string throws where it
 * stands. So the shape is part of what is stored, and the only way for four stores to share one
 * seam without changing what any of them has already written down is for the seam to carry it.
 *
 * [Int32] and [Int64] are named after their width and nothing prettier, because the width is the
 * whole of what distinguishes them: they are different entries to the file, a key written as one
 * cannot be read back as the other, and so widening a stored number is a migration and never an
 * edit.
 */
sealed interface Stored {
    data class Text(val value: String) : Stored

    data class Flag(val value: Boolean) : Stored

    data class Int32(val value: Int) : Stored

    data class Int64(val value: Long) : Stored
}

/**
 * A handful of named values that survive a launch, and nothing else.
 *
 * What used to be here was [StoredShelves][com.jenarvaezg.coindex.ui.shelf.StoredShelves] itself,
 * which meant the data layer imported `CoinsShelf`, `IndexShelf` and `ShelfCodec` out of
 * `ui.shelf` to know its own storage format — presentation deciding what persistence looks like,
 * the dependency exactly backwards (#221). What actually needed a device was never the shelf: it
 * was these five lines of `SharedPreferences`.
 *
 * And it was never the notebook, the sync log or the credentials either (#546). Each of those was
 * an interface over one property with a fake of its own, written for no reason but to be readable
 * in JVM — a seam apiece, each with exactly one implementation. They are plain classes over this
 * one now, and the fake that stands in for a device is [NamedValues]' own.
 *
 * A null value is a **removal** and not an empty string, which is the one rule of this format that
 * is not obvious: a blank country would read back as a filter for a country nobody is called.
 */
interface NamedValues {
    fun read(key: String): Stored?

    fun write(values: Map<String, Stored?>)
}

/** The text under [key], or null when nothing — or something that is not text — is stored there. */
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

/** Writes a codec's flags, all of them present: a switch's absence is not something to write. */
fun NamedValues.writeFlags(values: Map<String, Boolean>) {
    write(values.mapValues { (_, on) -> Stored.Flag(on) })
}

/** [NamedValues] on one shared preferences file. */
class SharedPreferenceValues(context: Context, name: String) : NamedValues {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    /**
     * Read off the whole map rather than through `getString` and its siblings.
     *
     * A typed getter aimed at a key of another type does not return its default: it throws where it
     * stands. Asking the map what is there means a store that reads a key an older version wrote in
     * another shape gets a null and its own default, which is a bug it can survive.
     *
     * It copies the file to answer about one key, which is nothing on the handful of keys any of
     * these files holds — and the alternative is every store knowing the shape of every key it has
     * ever written, which is the knowledge this seam exists to hold.
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
