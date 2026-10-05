package com.jenarvaezg.coindex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jenarvaezg.coindex.AppContainer
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.InventoryRefresh
import com.jenarvaezg.coindex.data.StoredCredentials
import com.jenarvaezg.coindex.data.StoredNotebook
import com.jenarvaezg.coindex.data.SyncOutcome
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.db.DatabaseExport
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop
import com.jenarvaezg.coindex.data.prices.ValuationLoop
import com.jenarvaezg.coindex.data.prices.showcaseValuationPlan
import com.jenarvaezg.coindex.data.prices.valuationPlan
import com.jenarvaezg.coindex.data.update.UPDATE_CHECK_INTERVAL_MILLIS
import com.jenarvaezg.coindex.data.update.UpdateFlow
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.NotebookSubject
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.PrintSection
import com.jenarvaezg.coindex.ui.print.forSheetExport
import com.jenarvaezg.coindex.ui.print.notebookSections
import com.jenarvaezg.coindex.ui.print.printGeometry
import com.jenarvaezg.coindex.ui.print.printPages
import com.jenarvaezg.coindex.ui.print.wishSections
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.ShelfStore
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one state the screens read ([UiState]), and the gestures that move it.
 *
 * Each gesture writes a field or hands the work to the module it belongs to: [CollectionSync],
 * [PhotoPrefetchLoop], [UpdateFlow], [credentialsEntry], [boxToCreate]. What the screens derive
 * from the state lives in [reading] (#542).
 *
 * Collaborators, the clock included, are passed one by one rather than as an `AppContainer` so a
 * test can substitute each of them (#220).
 */
class CoindexViewModel(
    /**
     * Resolved on first use, not in the factory: a curated file that fails to parse must surface as
     * [UiState.fatalError] inside [start]'s `try`, not as a crash at launch.
     */
    repository: () -> CoindexRepository,
    private val credentials: StoredCredentials,
    private val shelves: ShelfStore,
    private val notebook: StoredNotebook,
    private val collectionSync: CollectionSync,
    private val inventoryRefresh: InventoryRefresh,
    private val typeRefresh: TypeRefresh,
    private val updates: UpdateFlow,
    private val photos: PhotoPrefetchLoop,
    /** When the catalog prices are asked for, and who yields to whom (ADR 0028). */
    private val valuation: ValuationLoop,
    /** A client bound to the stored API key, or null while onboarding is pending. */
    private val client: () -> NumistaClient?,
    /**
     * Tops up the shipped ficha cache before the collection is first read. Awaited, not launched
     * alongside: a plate drawn before its fichas exist shows holes (#67).
     */
    private val warmUpFichaCache: suspend () -> Unit,
    /** A checkpointed copy of the base, for whatever the share sheet hands it to (#548). */
    private val dataExport: DatabaseExport,
    private val installedVersionName: String,
    /** Only stamps a price book's arrival ([UiState.pricesArrivedAt]). Injectable (#220). */
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val repository by lazy(repository)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * The curated files shipped with the app; constant for the process lifetime (#217). Private:
     * screens get answers through [reading], not the files.
     */
    private val curation get() = repository.curation

    /**
     * The last reading handed out, so the next caller gets the same object (#542). Its walks are
     * `by lazy` and belong to the instance, and the ViewModel shares them with the screens (the
     * pass's plan and the notebook's pages read the same wishes the annex draws).
     *
     * One slot: a miss costs the walks again, never a wrong answer. Only touched on the main
     * thread, by the composition and by `viewModelScope`.
     */
    private var lastReading: ScreenReading? = null

    /**
     * The collection's half of the reading, memoised apart because it changes far less often
     * (#218): prices land row by row during a pass, and one memo would rebuild the shelf window and
     * every open plate per row.
     */
    private var lastCollectionReading: CollectionReading? = null

    /**
     * Everything the screens read, derived from [state] and the curated files.
     *
     * The state is a parameter so a composition reads the state it is drawing, not a newer one;
     * commands pass nothing and get the current state. The same instance comes back while the
     * slices in [ScreenReading]'s constructor are equal, so a screen can key `remember` on it.
     *
     * With the curated files unreadable the reading uses none instead of raising the fatal error
     * again: the masthead and the sewn edge still draw over [UiState.fatalError].
     */
    fun reading(state: UiState = _state.value): ScreenReading {
        val next = state.reading(collectionReading(state))
        return lastReading?.takeIf { it == next } ?: next.also { lastReading = it }
    }

    /** The same memo one level in, over the half of a reading that prices don't affect. */
    private fun collectionReading(state: UiState): CollectionReading {
        val next = CollectionReading(
            curation = if (state.fatalError == null) curation else NO_CURATION,
            collection = state.collection,
        )
        return lastCollectionReading?.takeIf { it == next }
            ?: next.also { lastCollectionReading = it }
    }

    init {
        _state.update {
            it.copy(
                versionName = installedVersionName,
                // Launch time, so no age is measured against 1970 before the first book lands.
                pricesArrivedAt = now(),
                lastSync = collectionSync.last,
                indexShelf = shelves.index,
                coinsShelf = shelves.coins,
                notebookOptions = notebook.options,
            )
        }
        start()
        refreshInventory()
        watchPhotoCache()
        watchValuation()
        watchPrices()
        watchWishes()
        checkForUpdate(force = true)
        pollForUpdates()
    }

    /**
     * Mirrors the valuation pass's progress (ADR 0028). Observed, not returned by the pass: it
     * outlives the screen, and on a later launch with an unchanged plan no pass runs at all.
     */
    private fun watchValuation() {
        viewModelScope.launch {
            valuation.status.collect { status -> _state.update { it.copy(valuation = status) } }
        }
    }

    /**
     * The prices, which change during a pass and never rebuild the index. The arrival is stamped
     * only when the book actually changed, since every age on screen is measured from it
     * (ADR 0030 §4).
     */
    private fun watchPrices() {
        viewModelScope.launch {
            repository.observePrices().collect { book ->
                _state.update { state ->
                    if (state.prices == book) state else state.copy(prices = book, pricesArrivedAt = now())
                }
            }
        }
    }

    /**
     * The casillas the collector marked (ADR 0029), observed like the prices and never joined to the
     * collection.
     *
     * Every change forces a pass, so a new mark is priced now rather than next launch. Forcing is
     * free when nothing is new: a pass asks only for what the phone doesn't hold (ADR 0028 §1). If
     * this emits before the collection is read, the plan is empty and `ValuationLoop.start` returns
     * without recording anything as covered.
     */
    private fun watchWishes() {
        viewModelScope.launch {
            repository.observeWishes().collect { wishes ->
                _state.update { it.copy(wishes = wishes) }
                valuePrices(force = true)
            }
        }
    }

    /** Marks an empty casilla or unmarks it (ADR 0029 §5). Silent: the mark is the answer. */
    fun toggleWish(key: WishKey) {
        val marked = _state.value.wishes.any { it.key == key }
        viewModelScope.launch {
            if (marked) repository.unmarkWish(key) else repository.markWish(key)
        }
    }

    /** Takes one mark off, from the annex, where the casilla is not there to be pressed again. */
    fun removeWish(key: WishKey) {
        viewModelScope.launch { repository.unmarkWish(key) }
    }

    /**
     * Prices one plate of the shelf window on request (ADR 0030 §3): only its holes, so the spend
     * is the number the gesture printed. Besides the ficha refresh, the only gesture that spends
     * the budget on purpose.
     *
     * Fresh prices are not asked for again (ADR 0028 §5). Success is silent, since the figure
     * appears in the header; a refusal is reported here, and «nothing to ask» by
     * [PlateFinance.press].
     */
    fun valuePlate(catalogId: String) {
        val plate = reading().showcasePlateOf(catalogId) ?: return
        if (_state.value.valuingPlate != null) return
        val plan = showcaseValuationPlan(plate)
        if (plan.isEmpty) return
        viewModelScope.launch {
            _state.update { it.copy(valuingPlate = catalogId) }
            val status = valuation.valueNow(plan)
            _state.update { it.copy(valuingPlate = null) }
            status.held?.let { refusal -> showMessage(UiNotice(showcaseRefusalMessage(refusal))) }
            // Restart the pass this displaced: it forgot what it had covered when it handed over
            // the budget.
            valuePrices(force = true)
        }
    }

    /**
     * Mirrors the photo cache into the state (#191). Observed rather than written by
     * [prefetchPhotographs]: the pass outlives the screen, and a new ViewModel may start none.
     */
    private fun watchPhotoCache() {
        viewModelScope.launch {
            photos.status.collect { status -> _state.update { it.copy(photoCache = status) } }
        }
    }

    /** Keeps looking while the app stays open, so a long session still notices a release. */
    private fun pollForUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(UPDATE_CHECK_INTERVAL_MILLIS)
                checkForUpdate()
            }
        }
    }

    private fun start() {
        viewModelScope.launch {
            _state.update { it.copy(onboarded = credentials.credentials() != null) }
            try {
                warmUpFichaCache()
                repository.observeState().collect { collection ->
                    _state.update { it.copy(collection = collection, loading = false) }
                    prefetchPhotographs()
                    valuePrices()
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, fatalError = error.message ?: error.toString())
                }
            }
        }
    }

    /**
     * Fetches the catalog's photographs quietly once the collection is read (#191). Called on every
     * emission; [PhotoPrefetchLoop] decides whether a repeat call does anything.
     */
    private fun prefetchPhotographs(force: Boolean = false) {
        photos.start(
            scope = viewModelScope,
            images = _state.value.collection.images.values.toList(),
            syncing = { _state.value.syncing },
            force = force,
        )
    }

    /**
     * Prices what is owned and the holes within reach (ADR 0028). Called on every emission;
     * [ValuationLoop] compares the plan itself to decide whether a repeat call does anything.
     */
    private fun valuePrices(force: Boolean = false) {
        val state = _state.value.collection
        valuation.start(
            scope = viewModelScope,
            plan = valuationPlan(
                items = state.items,
                curation = curation,
                albums = state.albums,
                evidencedCatalogIds = state.evidencedCatalogIds,
                // A marked casilla is priced whatever its plate's shape (ADR 0029 §4).
                wishes = reading().livingWishes,
            ),
            force = force,
        )
    }

    /**
     * Pauses photo prefetch and valuation while the notebook exports, and resumes them after: the
     * export needs all four of the loader's slots and the collector is waiting on it (#191).
     */
    fun notebookExporting(active: Boolean) {
        if (active) {
            photos.cancel()
            valuation.cancel()
        } else {
            prefetchPhotographs(force = true)
            valuePrices(force = true)
        }
    }

    /**
     * Retries the missing photographs when the app returns to the foreground. A pass reads its
     * conditions (wifi) only when it starts, so otherwise a phone that joins a wifi would wait for
     * the next launch. Skipped when nothing is missing, to avoid rescanning the cache.
     */
    fun retryPhotoPrefetch() {
        if (_state.value.photoCache.missing == 0) return
        prefetchPhotographs(force = true)
    }

    /** The sign-up form, which has nowhere to go back to and so reports only through the state. */
    fun completeOnboarding(apiKey: String, userId: String) {
        when (val entry = onboardingEntry(apiKey, userId)) {
            is CredentialsEntry.Refused -> _state.update { it.copy(validation = entry.problem) }
            is CredentialsEntry.Accepted -> {
                credentials.save(entry.credentials.apiKey, entry.credentials.userId)
                _state.update { it.copy(onboarded = true, validation = null) }
            }
        }
    }

    /** The stored credentials, so «Credenciales» opens on what is in effect. */
    fun currentCredentials(): CredentialsValues {
        val stored = credentials.credentials()
        return CredentialsValues(
            apiKey = stored?.apiKey.orEmpty(),
            userId = stored?.userId?.toString().orEmpty(),
        )
    }

    /**
     * Saves the «Credenciales» form as one unit, reporting inline whether it took.
     *
     * @return true when everything was stored, so the caller can leave the screen.
     */
    fun saveCredentials(apiKey: String, userId: String): Boolean =
        when (val entry = credentialsEntry(apiKey, userId)) {
            is CredentialsEntry.Refused -> {
                _state.update { it.copy(validation = entry.problem) }
                false
            }
            is CredentialsEntry.Accepted -> {
                credentials.save(entry.credentials.apiKey, entry.credentials.userId)
                _state.update { it.copy(validation = null, message = UiNotice(CREDENTIALS_SAVED_MESSAGE)) }
                true
            }
        }

    /**
     * Forgets the credentials and returns to onboarding. The collection and the record of its last
     * sync stay on the device.
     */
    fun signOut() {
        credentials.clear()
        _state.update { it.copy(onboarded = false, validation = null) }
    }

    /**
     * Writes the raw database out and returns the file; the screen opens the share sheet (#548). A
     * failure shows a message and returns null.
     */
    suspend fun exportData(): File? {
        if (_state.value.exportingData) return null
        _state.update { it.copy(exportingData = true) }
        return try {
            runCatching { dataExport.write() }
                .onFailure { failure -> showMessage(dataExportFailure(failure.message)) }
                .getOrNull()
        } finally {
            _state.update { it.copy(exportingData = false) }
        }
    }

    /** Drops a stale form error so a screen is never entered with the last visit's complaint. */
    fun clearValidation() {
        _state.update { it.copy(validation = null) }
    }

    /**
     * Narrows a hierarchy's shelf and remembers it (ADR 0021 §1). Stored on every chip, since a
     * root has no exit to save on and the app can be killed from anywhere.
     */
    fun narrowIndex(shelf: IndexShelf) {
        shelves.index = shelf
        _state.update { it.copy(indexShelf = shelf) }
    }

    fun narrowCoins(shelf: CoinsShelf) {
        shelves.coins = shelf
        _state.update { it.copy(coinsShelf = shelf) }
    }

    /** Creates one of the collector's own boxes (ADR 0013, ADR 0021 §11), or says why not. */
    fun createOwnGrouping(name: String, typeIds: List<Int>) {
        when (val entry = boxToCreate(name, typeIds)) {
            is BoxEntry.Refused -> _state.update { it.copy(message = UiNotice(entry.message)) }
            is BoxEntry.Accepted -> viewModelScope.launch {
                repository.createOwnGrouping(entry.name, typeIds)
                _state.update { it.copy(message = UiNotice(boxCreatedMessage(entry.name))) }
            }
        }
    }

    fun addToOwnGrouping(groupingId: Long, typeIds: List<Int>) {
        if (typeIds.isEmpty()) return
        viewModelScope.launch { repository.addToOwnGrouping(groupingId, typeIds) }
    }

    fun renameOwnGrouping(groupingId: Long, name: String) {
        when (val entry = boxToRename(name)) {
            is BoxEntry.Refused -> _state.update { it.copy(message = UiNotice(entry.message)) }
            is BoxEntry.Accepted -> viewModelScope.launch {
                repository.renameOwnGrouping(groupingId, entry.name)
            }
        }
    }

    fun removeFromOwnGrouping(groupingId: Long, typeId: Int) {
        viewModelScope.launch { repository.removeFromOwnGrouping(groupingId, typeId) }
    }

    fun deleteOwnGrouping(groupingId: Long) {
        viewModelScope.launch { repository.deleteOwnGrouping(groupingId) }
    }

    /**
     * The client, or null after saying why there isn't one. Every gesture that spends API budget
     * goes through here, with the sentence [syncErrorLabel] gives for an empty key.
     */
    private fun clientOrComplain(): NumistaClient? {
        val ready = client()
        if (ready == null) {
            _state.update { it.copy(message = UiNotice(syncErrorLabel(NumistaException.EmptyApiKey()))) }
        }
        return ready
    }

    /**
     * The unrequested version of [sync]: brings the inventory up to date on launch if a day has
     * passed since the last one (#605). Because nobody asked for it, it:
     *
     * - shows no message, for what it found or for what failed;
     * - buys at most [com.jenarvaezg.coindex.data.AUTOMATIC_FICHA_LIMIT] fichas;
     * - gives up on a refusal already recorded, so the phone doesn't pay twice a day to hear it
     *   again (#579). A press still retries, which is how the collector learns the key works.
     *
     * Like [sync], it takes the network and budget from the photographs and the pass first, and
     * forces the pass afterwards: a pass that stood down recorded nothing as covered.
     */
    private fun refreshInventory() {
        if (_state.value.syncing || !inventoryRefresh.due()) return
        val ready = client() ?: return
        val userId = credentials.credentials()?.userId ?: return
        _state.update { it.copy(syncing = true) }
        viewModelScope.launch {
            photos.yieldNetwork()
            valuation.yieldNetwork()
            inventoryRefresh.run(ready, userId)
            _state.update { it.copy(syncing = false, lastSync = collectionSync.last) }
            prefetchPhotographs(force = true)
            valuePrices(force = true)
        }
    }

    fun sync() {
        if (_state.value.syncing) return
        val ready = clientOrComplain() ?: return
        val userId = credentials.credentials()?.userId ?: return
        _state.update { it.copy(syncing = true, message = null) }
        viewModelScope.launch {
            // The photographs yield the network, and the valuation the calls themselves: they come
            // out of the same monthly budget, and a pass in flight could make the sync fail with
            // `BudgetExhausted` (ADR 0028 §6).
            photos.yieldNetwork()
            valuation.yieldNetwork()
            val outcome = collectionSync.run(ready, userId)
            _state.update { state ->
                when (outcome) {
                    is SyncOutcome.Done -> state.copy(
                        syncing = false,
                        lastSync = outcome.record,
                        message = UiNotice(syncReportLabel(outcome.record)),
                    )
                    is SyncOutcome.Failed -> state.copy(
                        syncing = false,
                        message = UiNotice(syncErrorLabel(outcome.error)),
                    )
                }
            }
            // Forced: a sync that changed nothing emits nothing, and the pass it cancelled would
            // wait for the next launch. The end of a sync is also a valuation trigger
            // (ADR 0028 §3).
            prefetchPhotographs(force = true)
            valuePrices(force = true)
        }
    }

    /**
     * Asks Numista again for one type's ficha (#185, ADR 0025). The collector asked, so every
     * outcome gets a message. The new ficha reaches the screen through the collection flow, as
     * after a sync.
     */
    fun refreshFicha(typeId: Int) {
        if (typeId in _state.value.refreshingFichas) return
        val ready = clientOrComplain() ?: return
        _state.update { it.copy(refreshingFichas = it.refreshingFichas + typeId, message = null) }
        viewModelScope.launch {
            val outcome = runCatching { typeRefresh.refresh(ready, typeId) }
            _state.update { state ->
                state.copy(
                    refreshingFichas = state.refreshingFichas - typeId,
                    message = UiNotice(
                        outcome.fold(
                            onSuccess = ::fichaRefreshMessage,
                            onFailure = { error -> fichaRefreshErrorLabel(typeId, error) },
                        ),
                    ),
                )
            }
        }
    }

    /**
     * Looks for a newer APK, if it is time to look. Failures never reach the screen as errors: an
     * update check that cannot reach GitHub must never interrupt looking at the collection.
     */
    fun checkForUpdate(force: Boolean = false) {
        viewModelScope.launch {
            val status = updates.check(force) ?: return@launch
            _state.update { it.copy(update = status) }
        }
    }

    /**
     * Downloads the published APK and hands it to the system installer, asking for the
     * special install permission first if it has not been granted yet.
     */
    fun installUpdate() {
        val available = _state.value.update as? UpdateStatus.Available ?: return
        if (_state.value.updating) return
        viewModelScope.launch {
            val outcome = updates.install(available) {
                _state.update { it.copy(updating = true, message = UiNotice(UPDATE_DOWNLOADING_MESSAGE)) }
            }
            _state.update {
                it.copy(updating = false, message = installOutcomeMessage(outcome)?.let(::UiNotice))
            }
        }
    }

    /** Surfaces a one-off message in the snackbar (export outcomes, validation notes). */
    fun showMessage(message: String) {
        showMessage(UiNotice(message))
    }

    /** Same, when the notice may carry Abrir for a file in Descargas (#403). */
    fun showMessage(notice: UiNotice) {
        _state.update { it.copy(message = notice) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    /**
     * The pages of [subject] on the collector's configuration; the only way into the printer
     * (#169, #228, #539).
     *
     * Built on demand, never observed: what prints is what was on screen at the press, so a sync
     * landing mid-export can't change the paper. [subject] picks the sections and
     * [NotebookSubject.asSheet] the configuration; the rest is shared by all four subjects.
     *
     * [options] is a parameter because the export sheet recounts on every tap, before the
     * configuration is stored on «Exportar».
     */
    fun notebookPages(
        subject: NotebookSubject,
        options: NotebookOptions,
    ): List<PrintPage> {
        val chosen = if (subject.asSheet) options.forSheetExport() else options
        return printPages(
            sections = sectionsOf(subject, chosen),
            geometry = printGeometry(chosen),
        )
    }

    /**
     * The sections of one subject, the only part where the four subjects differ. Returns sections,
     * not pages, so pagination stays in `printPages`. Not an overload of `notebookSections`, which
     * it would shadow.
     */
    private fun sectionsOf(
        subject: NotebookSubject,
        options: NotebookOptions,
    ): List<PrintSection> {
        val state = _state.value
        // A plate prints through its card, so its page matches the card's destination
        // (ADR 0021 §9). No card, nothing to print, as on screen.
        val cards: List<IndexCard> = when (subject) {
            is NotebookSubject.Index -> subject.cards
            is NotebookSubject.Sheet -> listOf(subject.card)
            is NotebookSubject.Plate -> listOfNotNull(
                state.collection.index
                    .filterIsInstance<IndexCard.Derived>()
                    .firstOrNull { it.plateCatalogId == subject.catalogId },
            )
            NotebookSubject.Wishes -> return wishSections(
                state.collection,
                reading(state).livingWishes,
                options,
            )
        }
        return notebookSections(
            state = state.collection,
            cards = cards,
            // Only the whole notebook prints the unclaimed coins (#275); for a sheet,
            // `forSheetExport` has already turned that switch off.
            unclaimed = (subject as? NotebookSubject.Index)?.unclaimed.orEmpty(),
            curation = curation,
            options = options,
            // The money switch is applied here by passing a value or nothing (#228, ADR 0021 §13).
            // Nothing too while the market hasn't fully landed: a partial total is false on paper.
            plateValue = { resolved ->
                if (options.money) reading(state).plateValue(resolved.album) else null
            },
            // Not behind the money switch: a mark is a state, not an amount (ADR 0026 §4). The
            // table's keys rather than the living slots, because whether a casilla prints its mark
            // depends on it being empty, which the album the printer walks decides.
            wished = state.wishes.mapTo(mutableSetOf()) { it.key },
        )
    }

    /**
     * Stores the export's options so the next export opens with them. Written on export, not on
     * every toggle, so «Cancelar» discards.
     */
    fun notebookPrinted(options: NotebookOptions) {
        notebook.options = options
        _state.update { it.copy(notebookOptions = options) }
    }

    companion object {
        /**
         * `AppContainer` builds and owns the collaborators, so they outlive a rotation (a prefetch
         * loop remembers what it covered); this only hands them over.
         */
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = CoindexViewModel(
                    repository = { container.repository },
                    credentials = container.credentials,
                    shelves = container.shelves,
                    notebook = container.notebook,
                    collectionSync = container.collectionSync,
                    inventoryRefresh = container.inventoryRefresh,
                    typeRefresh = container.typeRefresh,
                    updates = container.updates,
                    photos = container.photos,
                    valuation = container.valuation,
                    client = container::numistaClient,
                    warmUpFichaCache = container::warmUpFichaCache,
                    dataExport = container.dataExport,
                    installedVersionName = container.installedVersionName(),
                ) as T
            }
    }
}
