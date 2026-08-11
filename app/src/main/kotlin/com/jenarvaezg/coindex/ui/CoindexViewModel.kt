package com.jenarvaezg.coindex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jenarvaezg.coindex.AppContainer
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.CredentialStore
import com.jenarvaezg.coindex.data.NotebookStore
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.SyncOutcome
import com.jenarvaezg.coindex.data.TypeRefresh
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import com.jenarvaezg.coindex.data.photos.PhotoPrefetchLoop
import com.jenarvaezg.coindex.data.prices.ValuationLoop
import com.jenarvaezg.coindex.data.prices.valuationPlan
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.data.update.UPDATE_CHECK_INTERVAL_MILLIS
import com.jenarvaezg.coindex.data.update.UpdateFlow
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.print.forSheetExport
import com.jenarvaezg.coindex.ui.print.notebookSections
import com.jenarvaezg.coindex.ui.print.printGeometry
import com.jenarvaezg.coindex.ui.print.printPages
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.shelf.ShelfStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one state the screens read, and the gestures that move it.
 *
 * What is left here is **[UiState] and nothing else**: every gesture below either writes a field or
 * hands the work to the module whose subject it is — [CollectionSync] for a sync, [PhotoPrefetchLoop]
 * for the photographs, [UpdateFlow] for the APK, [settingsEntry] and [boxToCreate] for what was
 * typed into a form. There is no clock in this file, and that is the measure of it: the three
 * `System.currentTimeMillis()` that used to be read in place now belong to the three modules that
 * stamp with them, each with a clock of its own that a test can hold still (#220).
 *
 * The collaborators arrive one by one rather than as an `AppContainer`, which is what makes any of
 * this readable: a container is not something a test can substitute, and «no hay nada que sustituir»
 * was the reason the largest file in the app had not a single test.
 */
class CoindexViewModel(
    /**
     * Resolved on first use rather than in the factory: the curated files are parsed the first time
     * anybody asks for them, and a file that fails to parse has to reach the collector as
     * [UiState.fatalError] — which is inside [start]'s `try` — instead of as a crash at launch.
     */
    repository: () -> CoindexRepository,
    private val credentials: CredentialStore,
    private val shelves: ShelfStore,
    private val notebook: NotebookStore,
    private val collectionSync: CollectionSync,
    private val typeRefresh: TypeRefresh,
    private val updates: UpdateFlow,
    private val photos: PhotoPrefetchLoop,
    /** When the catalog prices are asked for, and who yields to whom (ADR 0028). */
    private val valuation: ValuationLoop,
    /** A client bound to the stored API key, or null while onboarding is pending. */
    private val client: () -> NumistaClient?,
    /**
     * Tops the shipped ficha cache up before the collection is read for the first time.
     *
     * Awaited rather than launched beside the collection: a plate drawn before its fichas exist is
     * the plate with holes in it of #67.
     */
    private val warmUpFichaCache: suspend () -> Unit,
    private val installedVersionName: String,
) : ViewModel() {
    private val repository by lazy(repository)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /**
     * The curated files shipped with the app; constant for the process lifetime (#217).
     *
     * Private on purpose: the screens ask this class for a name or a plate, and what they are
     * given is the answer and not the files it came from.
     */
    private val curation get() = repository.curation

    /**
     * The curated catalogs the country and year axes walk (ADR 0026 §9).
     *
     * The index of cards is not enough: a member's country is not the card's (#170), and the year
     * axis needs every measurable slot, not only the ones that opened a plate today.
     */
    val catalogs get() = curation.catalogs

    /**
     * The card-sized name of every curated collection (#22). The plate keeps the long [name] —
     * defining the editorial scope is its job — so this is the index's, and the ficha's.
     */
    val titles get() = curation.titles

    /**
     * Every name a curated file claims, so the one name a collector types cannot repeat one
     * (ADR 0021 §4). Constant for the process lifetime, like the seeds it comes from.
     */
    val curatedNames: Set<String> get() = curation.titles.curatedNames()

    /** The name of one curated catalog, for the masthead of its plate. */
    fun catalogName(catalogId: String?): String? =
        catalogs.firstOrNull { it.id == catalogId }?.name

    init {
        _state.update {
            it.copy(
                versionName = installedVersionName,
                lastSync = collectionSync.last,
                indexShelf = shelves.index,
                coinsShelf = shelves.coins,
                notebookOptions = notebook.options,
            )
        }
        start()
        watchPhotoCache()
        watchValuation()
        watchPrices()
        checkForUpdate(force = true)
        pollForUpdates()
    }

    /**
     * Mirrors how far the valuation pass has got (ADR 0028).
     *
     * Observed for the same reason the photograph count is: the pass outlives the screen that started it,
     * and on the second launch of a month there is no new pass at all — the plan has not changed — so a
     * status that travelled with the pass would leave the money section absent over a phone that holds
     * every price it needs.
     */
    private fun watchValuation() {
        viewModelScope.launch {
            valuation.status.collect { status -> _state.update { it.copy(valuation = status) } }
        }
    }

    /** The prices themselves, which change while a pass runs and never rebuild the index. */
    private fun watchPrices() {
        viewModelScope.launch {
            repository.observePrices().collect { book -> _state.update { it.copy(prices = book) } }
        }
    }

    /**
     * Mirrors what the phone holds of the photographs into the state (#191).
     *
     * Observed and not written by [prefetchPhotographs], because the pass outlives the screen that
     * started it: a collector who comes back to a new ViewModel gets no new pass — the fichas have
     * not changed — and the settings line still has to say what is there.
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
     * Asks for the catalog's photographs, quietly, once the collection has been read (#191).
     *
     * Called on **every** emission and cheap to call twice: [PhotoPrefetchLoop] holds the rules about
     * what a second call is worth. Creating a box or renaming one emits too, and neither changes what
     * there is to fetch.
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
     * Asks Numista for the prices of what is owned and of the holes within reach (ADR 0028).
     *
     * On every emission and cheap to call twice, like the photographs: [ValuationLoop] holds the rules
     * about what a second call is worth, and the plan it compares is the plan itself rather than a count
     * two changes could cancel out.
     */
    private fun valuePrices(force: Boolean = false) {
        val state = _state.value.collection
        valuation.start(
            scope = viewModelScope,
            plan = valuationPlan(state.items, curation, state.evidencedCatalogIds),
            force = force,
        )
    }

    /**
     * Gives the network back while the notebook is being exported, and takes it up again after.
     *
     * The export takes all four of the loader's slots and the collector is watching it happen; two
     * of those four held by pictures nobody has asked for is exactly the theft this prefetch was
     * designed not to commit (#191). The valuation stands down for the same reason and one more of its
     * own: an export is what the collector is waiting for, and the pass is not.
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
     * Tries the photographs again when the app comes back to the front.
     *
     * The conditions are read once, when a pass starts, so a phone that walks into a wifi while the
     * app is open would otherwise wait for the next launch — and the settings line says «se traerán
     * cuando haya wifi», which has to be true. Coming back from the background is the moment that
     * costs nothing to check, and the guard keeps it from rescanning sixteen hundred cache entries
     * every time the collector glances at another app.
     */
    fun retryPhotoPrefetch() {
        if (_state.value.photoCache.missing == 0) return
        prefetchPhotographs(force = true)
    }

    fun saveCredentials(apiKey: String, userId: String) {
        when (val entry = onboardingEntry(apiKey, userId)) {
            is SettingsEntry.Refused -> _state.update { it.copy(validation = entry.problem) }
            is SettingsEntry.Accepted -> {
                credentials.save(entry.credentials.apiKey, entry.credentials.userId)
                _state.update { it.copy(onboarded = true, validation = null) }
            }
        }
    }

    /** The stored credentials, so the settings screen opens on what is in effect. */
    fun currentSettings(): SettingsValues {
        val stored = credentials.credentials()
        return SettingsValues(
            apiKey = stored?.apiKey.orEmpty(),
            userId = stored?.userId?.toString().orEmpty(),
        )
    }

    /**
     * Saves the settings form as one unit, reporting inline whether it took.
     *
     * @return true when everything was stored, so the caller can leave the screen.
     */
    fun saveSettings(apiKey: String, userId: String): Boolean =
        when (val entry = settingsEntry(apiKey, userId)) {
            is SettingsEntry.Refused -> {
                _state.update { it.copy(validation = entry.problem) }
                false
            }
            is SettingsEntry.Accepted -> {
                credentials.save(entry.credentials.apiKey, entry.credentials.userId)
                _state.update { it.copy(validation = null, message = SETTINGS_SAVED_MESSAGE) }
                true
            }
        }

    /**
     * Forgets the credentials and returns to onboarding.
     *
     * The collection stays on the device, and so does the record of when it was last synced:
     * signing out is «these credentials are wrong», not «throw away what we already have».
     */
    fun signOut() {
        credentials.clear()
        _state.update { it.copy(onboarded = false, validation = null) }
    }

    /** Drops a stale form error so a screen is never entered with the last visit's complaint. */
    fun clearValidation() {
        _state.update { it.copy(validation = null) }
    }

    /**
     * Narrows one of the two hierarchies, and remembers it (ADR 0021 §1).
     *
     * Written through on every chip rather than saved on the way out: there is no «way out» of a
     * root destination — the bottom bar crosses to the other one and the app is killed from wherever
     * it happens to be — so a shelf saved on exit is a shelf that survives only some of the time.
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
            is BoxEntry.Refused -> _state.update { it.copy(message = entry.message) }
            is BoxEntry.Accepted -> viewModelScope.launch {
                repository.createOwnGrouping(entry.name, typeIds)
                _state.update { it.copy(message = boxCreatedMessage(entry.name)) }
            }
        }
    }

    fun addToOwnGrouping(groupingId: Long, typeIds: List<Int>) {
        if (typeIds.isEmpty()) return
        viewModelScope.launch { repository.addToOwnGrouping(groupingId, typeIds) }
    }

    fun renameOwnGrouping(groupingId: Long, name: String) {
        when (val entry = boxToRename(name)) {
            is BoxEntry.Refused -> _state.update { it.copy(message = entry.message) }
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
     * The client, or null having said why there isn't one.
     *
     * Every gesture that spends API budget goes through here, so «falta la API key» is one sentence
     * in one place — and it is the same sentence [syncErrorLabel] gives for an empty key.
     */
    private fun clientOrComplain(): NumistaClient? {
        val ready = client()
        if (ready == null) {
            _state.update { it.copy(message = syncErrorLabel(NumistaException.EmptyApiKey())) }
        }
        return ready
    }

    fun sync() {
        if (_state.value.syncing) return
        val ready = clientOrComplain() ?: return
        val userId = credentials.credentials()?.userId ?: return
        _state.update { it.copy(syncing = true, message = null) }
        viewModelScope.launch {
            // The photographs give the network back to the sync, which is both spending API budget
            // and being waited for. The valuation gives back something graver — the **calls
            // themselves**, out of the same monthly bote — so a pass in flight could otherwise eat
            // what the sync needs and make it fail with `BudgetExhausted` (ADR 0028 §6).
            photos.yieldNetwork()
            valuation.yieldNetwork()
            val outcome = collectionSync.run(ready, userId)
            _state.update { state ->
                when (outcome) {
                    is SyncOutcome.Done -> state.copy(
                        syncing = false,
                        lastSync = outcome.record,
                        message = syncReportLabel(outcome.record),
                    )
                    is SyncOutcome.Failed -> state.copy(
                        syncing = false,
                        message = syncErrorLabel(outcome.error),
                    )
                }
            }
            // Forced, because a sync that changed nothing emits nothing, and the pass it cancelled
            // would otherwise wait for the next launch to be picked up again. The end of a sync is
            // also the valuation's second trigger: it is the ceremony that already spends budget and
            // already says what it spent (ADR 0028 §3).
            prefetchPhotographs(force = true)
            valuePrices(force = true)
        }
    }

    /**
     * Asks Numista again for one type's ficha (#185, ADR 0025).
     *
     * One call, and the collector asked for it, so unlike the update check every outcome is spoken:
     * what changed, that nothing did, or why it could not be asked. The corrected ficha reaches the
     * screen through the same flow a sync does — nothing here pushes it — so the card the collector
     * is looking at redraws itself with the family Numista now publishes.
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
                    message = outcome.fold(
                        onSuccess = ::fichaRefreshMessage,
                        onFailure = { error -> fichaRefreshErrorLabel(typeId, error) },
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
                _state.update { it.copy(updating = true, message = UPDATE_DOWNLOADING_MESSAGE) }
            }
            _state.update {
                it.copy(updating = false, message = installOutcomeMessage(outcome))
            }
        }
    }

    /** Surfaces a one-off message in the snackbar (export outcomes, validation notes). */
    fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    /**
     * The given cards as printable pages, on the configuration the collector chose (#169, #228).
     *
     * Built on demand and never observed: [cards] is what the index was showing when the button was
     * pressed, which is exactly what the collector chose to print. There is no `Notebook` behind it
     * — no table, no name, no second order (ADR 0021 §1).
     *
     * [options] comes in rather than being read off the state, because the export sheet recounts on
     * every tap and what it is counting is the configuration **under the collector's thumb** — which
     * is only stored once they press «Exportar».
     *
     * [unclaimed] arrives the same way and for the same reason (#275): the coins no collection claims
     * are measured against the whole index and then narrowed by the shelf on screen, and both of
     * those are the index's answers. What this adds is nothing — [notebookSections] decides whether
     * the lámina is drawn, from the switch.
     */
    fun notebookPages(
        cards: List<IndexCard>,
        unclaimed: List<CollectedItem>,
        options: NotebookOptions,
    ): List<PrintPage> = printPages(
        sections = notebookSections(
            state = _state.value.collection,
            cards = cards,
            unclaimed = unclaimed,
            curation = curation,
            options = options,
            // The money switch is answered once, here, by handing the printer either a value or
            // nothing (#228, ADR 0021 §13). And nothing is also what it gets while the market has
            // not landed: a total at 60 % is false on paper too, and paper cannot be taken back.
            plateValue = { resolved ->
                if (!options.money || !_state.value.valuation.settled) {
                    null
                } else {
                    plateValue(
                        resolved.album,
                        _state.value.collection,
                        _state.value.prices.spot,
                        _state.value.prices::of,
                    )
                }
            },
        ),
        geometry = printGeometry(options),
    )

    /**
     * One curated plate as printable pages, under the configuration the collector chose for this
     * lámina (#401).
     *
     * The card is looked up by [catalogId] rather than passed in, because the plate screen already
     * resolved the album and does not hold the [IndexCard] the index drew — and `página(tarjeta) =
     * su destino` still has to go through [notebookSections].
     */
    fun notebookPagesForPlate(catalogId: String, options: NotebookOptions): List<PrintPage> {
        val card = _state.value.collection.index
            .filterIsInstance<IndexCard.Derived>()
            .firstOrNull { it.plateCatalogId == catalogId }
            ?: return emptyList()
        return notebookPages(listOf(card), emptyList(), options.forSheetExport())
    }

    /**
     * One collection without a plate, or a box, as printable pages (#401).
     *
     * Same door as the index: [destinationOf] decides the pieces section, and packing / the loose
     * plate are cleared by [forSheetExport] so a leftover switch from the notebook cannot thin a
     * heading that has nothing to share a folio with.
     */
    fun notebookPagesForCard(card: IndexCard, options: NotebookOptions): List<PrintPage> =
        notebookPages(listOf(card), emptyList(), options.forSheetExport())

    /**
     * Remembers how the notebook was printed, so the next export opens where this one left off.
     *
     * Written on the export and not on every toggle: a sheet the collector opened, played with and
     * dismissed has not changed how they print, and storing each tap would make «Cancelar» a lie.
     */
    fun notebookPrinted(options: NotebookOptions) {
        notebook.options = options
        _state.update { it.copy(notebookOptions = options) }
    }

    fun plate(catalogId: String): PlateResult =
        resolvePlate(_state.value.collection, curation, catalogId)

    // The pair that used to answer «is there a catalog for this key, and would its plate open?»
    // left with the screen that asked: a card with a reachable plate now *is* the plate (ADR 0021
    // §9), so nothing between the index and the plate needs to explain a jump it cannot make.

    companion object {
        /**
         * The one place the collaborators above are named twice.
         *
         * `AppContainer` builds and holds them — a prefetch loop that outlives a rotation keeps
         * knowing which photographs it already covered — and this only picks the ones the screens'
         * state is made of.
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
                    typeRefresh = container.typeRefresh,
                    updates = container.updates,
                    photos = container.photos,
                    valuation = container.valuation,
                    client = container::numistaClient,
                    warmUpFichaCache = container::warmUpFichaCache,
                    installedVersionName = container.installedVersionName(),
                ) as T
            }
    }
}
