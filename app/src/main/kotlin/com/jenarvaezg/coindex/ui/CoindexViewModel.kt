package com.jenarvaezg.coindex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jenarvaezg.coindex.AppContainer
import com.jenarvaezg.coindex.data.CoindexRepository
import com.jenarvaezg.coindex.data.CollectionSync
import com.jenarvaezg.coindex.data.CredentialStore
import com.jenarvaezg.coindex.data.NotebookStore
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
 * The one state the screens read, and the gestures that move it.
 *
 * What is left here is **[UiState] and nothing else**: every gesture below either writes a field or
 * hands the work to the module whose subject it is — [CollectionSync] for a sync, [PhotoPrefetchLoop]
 * for the photographs, [UpdateFlow] for the APK, [credentialsEntry] and [boxToCreate] for what was
 * typed into a form. And what is **read** off that state is [reading]'s and not this class's (#542):
 * the shelf window and the living marks used to be computed here and again in the root composable,
 * with a comment on each side promising the two agreed.
 *
 * There is one clock in this file and it stamps one field — the arrival of a price book, which is the
 * «now» every age on screen is measured against. The three `System.currentTimeMillis()` that used to
 * be read in place still belong to the modules that stamp with them, and this one arrives the same
 * way: as a parameter a test can hold still (#220).
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
    /** A checkpointed copy of the base, for whatever the share sheet hands it to (#548). */
    private val dataExport: DatabaseExport,
    private val installedVersionName: String,
    /**
     * The one clock this class reads for itself, and it reads it for one thing: stamping the arrival
     * of a price book (see [UiState.pricesArrivedAt]). Every other clock in the app belongs to the
     * collaborator that needs it, and a test can hold each of them still (#220).
     */
    private val now: () -> Long = System::currentTimeMillis,
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
     * The last reading handed out, kept so the next caller gets **the same object** (#542).
     *
     * A [ScreenReading] is eight references and builds nothing, so this is not about the cost of
     * making one: it is about the walks hanging off it, which are `by lazy` and therefore belong to
     * an instance. Held here and not in the composition because the ViewModel reads the same
     * derivations the screens do — the pass's plan and the notebook's pages are made of the wishes
     * the annex draws — and two instances is exactly the arrangement this ticket removed.
     *
     * **One slot, and nothing depends on it holding.** A miss costs the walks again and never a wrong
     * answer, because what comes back is built from the state that was asked about; the composition
     * and this class's own commands both read it on the main thread, which is where `viewModelScope`
     * runs.
     */
    private var lastReading: ScreenReading? = null

    /**
     * And the collection's own half of it, kept apart because it moves far less often (#218).
     *
     * A price lands row by row while a pass runs, so a single memo would rebuild the shelf window and
     * re-resolve every open plate once per row — for a reading none of them is made of.
     */
    private var lastCollectionReading: CollectionReading? = null

    /**
     * Everything the screens read, derived from one state and the curated files.
     *
     * The state comes in rather than being taken off [_state], so the reading a composition draws is
     * the reading **of the state it is drawing**: a root that asked for the current one could paint a
     * frame of two moments. The ViewModel's own callers pass nothing and get the state of right now,
     * which is what a command acts on.
     *
     * **The same instance comes back while nothing it is made of has moved.** That is the whole of
     * the memoisation: `equals` over the slices in [ScreenReading]'s constructor is what decides,
     * and a screen keys its `remember` on the value instead of listing the fields of the state that
     * feed it.
     *
     * With the curated files unreadable there are none, and the reading says so rather than raising
     * the same fatal error a second time: what is on screen then is [UiState.fatalError] itself, and
     * the masthead and the sewn edge above it still have to draw.
     */
    fun reading(state: UiState = _state.value): ScreenReading {
        val next = state.reading(collectionReading(state))
        return lastReading?.takeIf { it == next } ?: next.also { lastReading = it }
    }

    /** The same trade one level in, over the half of a reading a price cannot move. */
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
                // Launch, so that an age is never measured against 1970 on the frames before the
                // first book lands. What it dates then is an empty book, which fetches nothing.
                pricesArrivedAt = now(),
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
        watchWishes()
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

    /**
     * The prices themselves, which change while a pass runs and never rebuild the index.
     *
     * The arrival is stamped here, and only when the book is actually a different one: what the stamp
     * dates is a figure that never expires (ADR 0030 §4), so re-reading the clock for a book that has
     * not changed would move every age on screen for nothing.
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
     * A mark is also the app's first **elastic** spend, so a new one starts a pass: what the collector
     * asked for by marking is exactly that price, and waiting for the next launch to fetch it would
     * make the gesture's «+2 consultas al mes» a promise about some other day.
     *
     * **Forced, and it costs nothing when there is nothing new.** A mark usually does move the plan, so
     * the loop would start a pass on its own — but not always: a mark on a casilla that is already full
     * moves nothing, and the loop remembers the plan it covered. A pass over an unchanged plan asks for
     * whatever the phone does not already hold, which in that case is nothing at all (ADR 0028 §1).
     *
     * **And the first emission of a launch cannot get ahead of the collection.** This flow may well emit
     * before the snapshot has been read, and then the plan is empty — `ValuationLoop.start` returns on
     * `plan.isEmpty` before it launches anything, so nothing is started and nothing is recorded as
     * covered. What arrives second is the collection, with the plan the pass is actually for.
     */
    private fun watchWishes() {
        viewModelScope.launch {
            repository.observeWishes().collect { wishes ->
                _state.update { it.copy(wishes = wishes) }
                valuePrices(force = true)
            }
        }
    }

    /**
     * Marks an empty casilla, or takes the mark off it (ADR 0029 §5).
     *
     * One gesture for both directions, because that is what the casilla is: a press on a hole that is
     * already marked unmarks it, and the state it is toggling is the one on screen. Silent — no
     * snackbar — because the mark itself is the answer, and a notice per press on a plate of ten holes
     * would be ten notices.
     */
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
     * Asks Numista what entering one plate of the shelf window costs (ADR 0030 §3).
     *
     * The app's **only** gesture that spends the budget on purpose besides the ficha's own refresh, and
     * the one place a plate that is not the collector's ever costs a call. What it asks for is that
     * plate's holes and nothing else, so the spend is the number the gesture printed.
     *
     * **A plate whose prices are all fresh asks for nothing and says so** (ADR 0028 §5): the pass's
     * thirty days decide whether an issue is worth a second call, and buying the same answer twice
     * because a button was pressed is the one thing a gesture that names its spend must not do.
     *
     * Silent about success, like the mark: what the collector sees is the figure appearing in the header
     * with its date. What is spoken is the two cases where **nothing** happened — a refusal, or nothing
     * left to ask — because both leave the screen looking exactly as it did.
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
            // The pass the gesture displaced starts again: what it had covered was forgotten when the
            // budget was handed over, so whatever it had not asked for is asked for on this call and not
            // on the next launch.
            valuePrices(force = true)
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
            plan = valuationPlan(
                items = state.items,
                curation = curation,
                albums = state.albums,
                evidencedCatalogIds = state.evidencedCatalogIds,
                // A marked casilla is priced whatever its plate's shape (ADR 0029 §4), which is what
                // makes the month's spend a function of what the collector marked.
                wishes = reading().livingWishes,
            ),
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
     * Forgets the credentials and returns to onboarding.
     *
     * The collection stays on the device, and so does the record of when it was last synced:
     * signing out is «these credentials are wrong», not «throw away what we already have».
     */
    fun signOut() {
        credentials.clear()
        _state.update { it.copy(onboarded = false, validation = null) }
    }

    /**
     * Writes the raw base out and hands back the file for the share sheet (#548).
     *
     * Returns the file rather than sending it: the chooser is an `Intent` and belongs to the screen,
     * as every other export in the app does. A failure is a message and null — nothing here is worth
     * taking the app down for, and the collector who reads «no se pudieron exportar los datos» knows
     * as much as a stack trace would tell them.
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
     * The client, or null having said why there isn't one.
     *
     * Every gesture that spends API budget goes through here, so «falta la API key» is one sentence
     * in one place — and it is the same sentence [syncErrorLabel] gives for an empty key.
     */
    private fun clientOrComplain(): NumistaClient? {
        val ready = client()
        if (ready == null) {
            _state.update { it.copy(message = UiNotice(syncErrorLabel(NumistaException.EmptyApiKey()))) }
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
                        message = UiNotice(syncReportLabel(outcome.record)),
                    )
                    is SyncOutcome.Failed -> state.copy(
                        syncing = false,
                        message = UiNotice(syncErrorLabel(outcome.error)),
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
     * The one door into the printer: whatever [subject] is, on the configuration the collector chose
     * (#169, #228, #539).
     *
     * Built on demand and never observed. What is printed is what was on screen when the button was
     * pressed — the index hands over its own cards, a plate hands over its id — so a sync landing
     * mid-export cannot change the paper. There is no `Notebook` behind it: no table, no name, no
     * second order (ADR 0021 §1).
     *
     * **One producer and not four** (#539). The wish list used to reach `printPages` through a second
     * call of its own, and the plate and the loose card each spelled `forSheetExport()` again; the
     * three switches that make the paper what it is — the geometry, the money and the marks — were
     * therefore threaded twice and could have drifted without a test noticing. Now [subject] says
     * which sections, [NotebookSubject.asSheet] says which configuration, and everything after that
     * is the same machine for all four doors.
     *
     * [options] comes in rather than being read off the state, because the export sheet recounts on
     * every tap and what it is counting is the configuration **under the collector's thumb** — which
     * is only stored once they press «Exportar».
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
     * The sections of one subject, which is the only thing the four doors disagree about.
     *
     * Named apart from `notebookSections` rather than overloading it, because a member shadowing the
     * printer's own function would be told apart by argument count alone. It returns sections and not
     * pages, so exactly one place turns a section into a folio: this decides *what is on the paper*
     * and `printPages` decides how much of it fits.
     */
    private fun sectionsOf(
        subject: NotebookSubject,
        options: NotebookOptions,
    ): List<PrintSection> {
        val state = _state.value
        // The card of a plate is looked up here and not by the screen: the index is what draws cards,
        // and `página(tarjeta) = su destino` has to go through a card to hold (ADR 0021 §9). Nothing
        // to print if the plate has no card, which is the same silence the screen shows.
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
            // Only the whole notebook has coins outside its cards (#275): the lámina of the
            // unclaimed is measured against the whole index and narrowed by the shelf on screen,
            // and a sheet of one collection has no such neighbours — `forSheetExport` has already
            // cleared the switch that would draw it.
            unclaimed = (subject as? NotebookSubject.Index)?.unclaimed.orEmpty(),
            curation = curation,
            options = options,
            // The money switch is answered once, here, by handing the printer either a value or
            // nothing (#228, ADR 0021 §13). And nothing is also what it gets while the market has
            // not landed: a total at 60 % is false on paper too, and paper cannot be taken back.
            plateValue = { resolved ->
                if (options.money) reading(state).plateValue(resolved.album) else null
            },
            // Not behind a switch: a wish mark is a state at rest and travels by ADR 0026 §4, and what
            // the money switch withholds is an amount. The keys are the table's own and not the living
            // slots, because what decides whether a casilla prints its mark is the casilla being empty
            // — which is the album's answer, and the album is what the printer is walking.
            wished = state.wishes.mapTo(mutableSetOf()) { it.key },
        )
    }

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
                    dataExport = container.dataExport,
                    installedVersionName = container.installedVersionName(),
                ) as T
            }
    }
}
