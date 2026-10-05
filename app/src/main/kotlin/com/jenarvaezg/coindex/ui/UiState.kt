package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf

/** What «Credenciales» edits, read from the credential store when it opens. */
data class CredentialsValues(val apiKey: String, val userId: String)

/** A one-off snackbar notice. [openFile] is set only after a download, for Abrir (#403). */
data class UiNotice(
    val text: String,
    val openFile: OpenDownloadedFile? = null,
)

/** The file Abrir on the download snackbar hands to ACTION_VIEW (#403). */
data class OpenDownloadedFile(
    /** Content URI as text — `android.net.Uri` is not available to JVM unit tests. */
    val uri: String,
    val mimeType: String,
)

/**
 * @param message a one-off notice for the snackbar; it is consumed once shown.
 * @param validation a form error shown next to its field until the form is submitted again; kept
 *   apart from [message] so dismissing a snackbar doesn't erase it.
 */
data class UiState(
    val onboarded: Boolean = false,
    val loading: Boolean = true,
    val syncing: Boolean = false,
    val collection: CollectionState = CollectionState(),
    val lastSync: SyncRecord? = null,
    val message: UiNotice? = null,
    val validation: String? = null,
    val fatalError: String? = null,
    val update: UpdateStatus = UpdateStatus.UpToDate,
    val updating: Boolean = false,
    /**
     * Whether the raw dump of #548 is being written, so a second tap doesn't open a second chooser.
     * Read only by «Este teléfono».
     */
    val exportingData: Boolean = false,
    val versionName: String = "",
    /**
     * Each hierarchy's shelf (ADR 0021 §1). In the state because it survives a launch; the search
     * text doesn't, and lives in its screen.
     */
    val indexShelf: IndexShelf = IndexShelf(),
    val coinsShelf: CoinsShelf = CoinsShelf(),
    /**
     * How the notebook is printed (#228), kept across launches like the shelves. Global, not per
     * card (ADR 0021 §7).
     */
    val notebookOptions: NotebookOptions = NotebookOptions(),
    /**
     * The types whose ficha is being fetched right now (#185). A set, so each row shows its own
     * progress.
     */
    val refreshingFichas: Set<Int> = emptySet(),
    /** What the phone holds of the catalog's photographs (#191). Read only on «Este teléfono». */
    val photoCache: PhotoCacheStatus = PhotoCacheStatus(),
    /**
     * Every catalog price and the last spot (ADR 0028), in the state so every screen pricing a
     * piece reads the same book.
     */
    val prices: PriceBook = PriceBook(),
    /**
     * When [prices] reached this phone: the «now» every age on screen is measured against. Not
     * `PriceBook.readAt`, which is when one issue was asked about.
     *
     * Stamped when a different book arrives, so ages don't change while being read (ADR 0030 §4),
     * and kept in the state so every screen dates the same book the same way.
     */
    val pricesArrivedAt: Long = 0L,
    /**
     * The casillas the collector marked, as the table holds them (ADR 0029). Rows, not resolved
     * slots: they meet the inventory only in `wishedSlots`, never in the collection's assembly
     * (ADR 0029 §3).
     */
    val wishes: List<Wish> = emptyList(),
    /**
     * How far the valuation pass has got, which decides whether the money section exists. While
     * the market is arriving the total would be `max(silver, paid)`, false rather than incomplete,
     * so it is absent, not zero (ADR 0028 §7).
     */
    val valuation: ValuationStatus = ValuationStatus(),
    /**
     * The shelf-window plate being valued right now, by catalog id (ADR 0030 §3). One at a time:
     * the gesture shows «Preguntando a Numista…» from it, and it stops a second press from starting
     * a second pass.
     */
    val valuingPlate: String? = null,
)
