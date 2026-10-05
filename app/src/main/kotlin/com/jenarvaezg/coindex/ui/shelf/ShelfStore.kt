package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.NamedValues
import com.jenarvaezg.coindex.data.text
import com.jenarvaezg.coindex.data.writeText

/**
 * Where each hierarchy's shelf is remembered. An interface so tests can check that a chip is
 * written through as soon as it is tapped (ADR 0021 §1).
 */
interface ShelfStore {
    var index: IndexShelf

    var coins: CoinsShelf
}

/** The preferences file the shelves live in. */
const val SHELF_PREFERENCES: String = "coindex-shelves"

/**
 * Both shelves' filters and sort, persisted across launches (ADR 0021 §1).
 *
 * Named values rather than Room, like `StoredSyncLog`: a few device-local values nothing joins
 * against. Nothing is stored per card, so ADR 0021 §7 is unaffected. Lives beside [ShelfCodec]
 * rather than in `data` (#221), since the codec is its storage format; it only needs [NamedValues].
 */
class StoredShelves(private val values: NamedValues) : ShelfStore {
    override var index: IndexShelf
        get() = ShelfCodec.decodeIndex { key -> values.text(key) }
        set(value) = values.writeText(ShelfCodec.encode(value))

    override var coins: CoinsShelf
        get() = ShelfCodec.decodeCoins { key -> values.text(key) }
        set(value) = values.writeText(ShelfCodec.encode(value))
}
