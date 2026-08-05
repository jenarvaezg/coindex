package com.jenarvaezg.coindex.data.photos

import android.content.Context
import java.util.concurrent.CopyOnWriteArraySet

/** The photographs Numista has answered `404` for, so they are not asked for on every launch. */
interface GonePhotographs {
    fun all(): Set<String>

    fun remember(url: String)
}

/**
 * Remembers them on the device, because the prefetch runs on every launch (#191).
 *
 * Coil's disk cache only ever holds answers that arrived, so a photograph that is **not** there
 * leaves no trace at all: without this list the prefetch would ask Numista for the same missing
 * picture every time the app opens, for as long as the phone lives, and the counter in settings
 * would say «faltan 12» for ever over twelve pictures that do not exist.
 *
 * Written by the network interceptor, from whatever thread OkHttp is on, and read by the prefetch:
 * a [CopyOnWriteArraySet] in front of the preferences keeps that free of locks, and the reads are
 * what happen 1.600 times while the writes are the rare ones.
 */
class StoredGonePhotographs(context: Context) : GonePhotographs {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val known = CopyOnWriteArraySet(prefs.getStringSet(KEY_GONE, emptySet()).orEmpty())

    override fun all(): Set<String> = known

    override fun remember(url: String) {
        // The set is what deduplicates: the same photograph is refused once per launch at most,
        // and rewriting the preferences on each of those is a write nobody asked for.
        if (!known.add(url)) return
        // A copy, because `getStringSet` hands back the very instance the preferences hold and
        // storing a mutated one is documented as undefined.
        prefs.edit().putStringSet(KEY_GONE, known.toSet()).apply()
    }

    private companion object {
        const val PREFS = "coindex-photos"
        const val KEY_GONE = "gone_photographs"
    }
}
