package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.NamedValues

/**
 * Where the shelf of each hierarchy is remembered.
 *
 * Two properties and an interface, so that «a chip is written through the moment it is tapped» is
 * something a test can watch happen rather than something the code merely says (ADR 0021 §1).
 */
interface ShelfStore {
    var index: IndexShelf

    var coins: CoinsShelf
}

/** The preferences file the shelves live in. */
const val SHELF_PREFERENCES: String = "coindex-shelves"

/**
 * The filters and the sort of the two hierarchies, across launches (ADR 0021 §1).
 *
 * On named values rather than in Room, for the same reason as `SyncLog`: it is a handful of values
 * about this device and this collector's last look at their own notebook, and no query ever joins
 * against them. Nothing here is per card either, so ADR 0021 §7 is untouched — a filter is what the
 * collector is looking through, not something stored about a collection.
 *
 * It lives beside [ShelfCodec] and not in `data`, which is where it used to be (#221). A shelf's
 * storage format *is* the codec, so a store in the data layer had to import three presentation
 * types to do its job; what it actually needed from the data layer was [NamedValues], and that is
 * now the only thing it takes.
 */
class StoredShelves(private val values: NamedValues) : ShelfStore {
    override var index: IndexShelf
        get() = ShelfCodec.decodeIndex(values::read)
        set(value) = values.write(ShelfCodec.encode(value))

    override var coins: CoinsShelf
        get() = ShelfCodec.decodeCoins(values::read)
        set(value) = values.write(ShelfCodec.encode(value))
}
