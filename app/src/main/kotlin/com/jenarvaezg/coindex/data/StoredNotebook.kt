package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.ui.print.NotebookCodec
import com.jenarvaezg.coindex.ui.print.NotebookOptions

/** The preferences file the notebook switches live in. */
const val NOTEBOOK_PREFERENCES: String = "coindex-notebook"

/**
 * How the collector prints their notebook, across launches (#228).
 *
 * On named values rather than in Room, for the same reason as [StoredSyncLog] and the shelves of
 * ADR 0021 §1: it is seven booleans about this device and this collector's last trip to the printer,
 * and no query ever joins against them. Nothing here is per card either, so ADR 0021 §7 is
 * untouched — how a notebook is printed is what the collector is looking through, not something
 * stored about a collection.
 *
 * Its own preferences file and not seven more keys in the shelves', because these are not a shelf:
 * a filter decides *which* collections come out, and these decide *what the paper looks like*.
 *
 * A plain class and no longer an interface with a fake of its own (#546). What has to be checkable
 * is *when* it is written — on the export and never on a toggle — and a fake [NamedValues] shows
 * that as well as a fake store did, while this class stops being a seam nobody had a second
 * implementation for.
 */
class StoredNotebook(private val values: NamedValues) {
    var options: NotebookOptions
        // An absent key reaches the codec as absent, so the default it reads back is the switch's
        // own — and the default of «fotos» is on. A missing key silently becoming false is a
        // notebook with no coins in it.
        get() = NotebookCodec.decode { key -> values.flag(key) }
        set(value) = values.writeFlags(NotebookCodec.encode(value))
}
