package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf

/** What the settings screen edits, read from the credential store when it opens. */
data class SettingsValues(val apiKey: String, val userId: String)

/**
 * @param message a one-off notice for the snackbar; it is consumed once shown.
 * @param validation a form error that belongs next to the field that caused it and stays until
 *   the form is submitted again. Keeping the two apart is what stops a dismissed snackbar from
 *   also erasing the inline text under the credential fields.
 */
data class UiState(
    val onboarded: Boolean = false,
    val loading: Boolean = true,
    val syncing: Boolean = false,
    val collection: CollectionState = CollectionState(),
    val lastSync: SyncRecord? = null,
    val message: String? = null,
    val validation: String? = null,
    val fatalError: String? = null,
    val update: UpdateStatus = UpdateStatus.UpToDate,
    val updating: Boolean = false,
    val versionName: String = "",
    /**
     * What the collector is looking through, on each of the two hierarchies (ADR 0021 §1).
     *
     * In the state and not in the screens because it survives a launch, which is exactly what tells
     * it apart from the search text: that one lives in the screen it belongs to and is gone with it.
     */
    val indexShelf: IndexShelf = IndexShelf(),
    val coinsShelf: CoinsShelf = CoinsShelf(),
    /**
     * How the notebook comes out of the printer (#228), across launches like the shelves above.
     *
     * In the state and not in the export sheet because it survives a launch: the collector who
     * printed a checklist last month opens the sheet on a checklist. It is **not** per card
     * (ADR 0021 §7) — it is how the paper looks, not something stored about a collection.
     */
    val notebookOptions: NotebookOptions = NotebookOptions(),
    /**
     * The types whose ficha is being asked for right now (#185).
     *
     * A set and not a flag: the two surfaces that carry the gesture are lists, and the collector who
     * taps two rows must see which two are working — one boolean would have greyed out every button
     * on screen to report one call.
     */
    val refreshingFichas: Set<Int> = emptySet(),
    /** What the phone holds of the catalog's photographs (#191). Read only in the settings screen. */
    val photoCache: PhotoCacheStatus = PhotoCacheStatus(),
    /**
     * Every catalog price and the last spot (ADR 0028).
     *
     * In the state rather than fetched by «Las cifras» when it opens: the value of a piece also lands in
     * its ficha and in the header of its plate, and three screens reading three books is three totals
     * that can disagree about the same coin.
     */
    val prices: PriceBook = PriceBook(),
    /**
     * How far the valuation pass has got, which is what decides whether the money section exists at all.
     *
     * **Absence and not zero** (ADR 0028 §7): while this says the market is still arriving, the total
     * would be `max(silver, paid)` — some 60 % of the real figure — and a total at 60 % is not
     * incomplete, it is false.
     */
    val valuation: ValuationStatus = ValuationStatus(),
)
