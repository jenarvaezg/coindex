package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.ui.print.NotebookCodec
import com.jenarvaezg.coindex.ui.print.NotebookOptions

/** The preferences file the notebook switches live in. */
const val NOTEBOOK_PREFERENCES: String = "coindex-notebook"

/**
 * How the collector printed their notebook last time (#228): a few device-local switches no query
 * joins, so named values rather than Room, like [StoredSyncLog] and the shelves (ADR 0021 §1). Not
 * per card, so ADR 0021 §7 holds. A file of its own: the shelves choose which collections come out,
 * these how the paper looks. Written on export, never on a toggle (#546).
 */
class StoredNotebook(private val values: NamedValues) {
    var options: NotebookOptions
        // An absent key reaches the codec as absent, so each switch gets its own default; «fotos»
        // defaults to on, and a missing key read as false would print no coins.
        get() = NotebookCodec.decode { key -> values.flag(key) }
        set(value) = values.writeFlags(NotebookCodec.encode(value))
}
