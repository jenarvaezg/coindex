package com.jenarvaezg.coindex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jenarvaezg.coindex.AppContainer
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import com.jenarvaezg.coindex.data.photos.prefetchRefusal
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.data.startOfMonthMillis
import com.jenarvaezg.coindex.data.update.UPDATE_CHECK_INTERVAL_MILLIS
import com.jenarvaezg.coindex.data.update.UpdateStatus
import com.jenarvaezg.coindex.data.update.shouldCheckForUpdate
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.ui.print.PrintPage
import com.jenarvaezg.coindex.ui.shelf.CoinsShelf
import com.jenarvaezg.coindex.ui.shelf.IndexShelf
import com.jenarvaezg.coindex.ui.print.notebookSections
import com.jenarvaezg.coindex.ui.print.printPages
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How long the photograph prefetch lets the first screen have the phone to itself (#191). */
private const val PREFETCH_START_DELAY_MILLIS = 3_000L

data class BudgetStatus(val used: Int, val cap: Int) {
    val remaining: Int get() = (cap - used).coerceAtLeast(0)
}

/** What the settings screen edits, read from the credential store when it opens. */
data class SettingsValues(val apiKey: String, val userId: String, val budgetCap: Int)

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
    val budget: BudgetStatus = BudgetStatus(0, 0),
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
     * The types whose ficha is being asked for right now (#185).
     *
     * A set and not a flag: the two surfaces that carry the gesture are lists, and the collector who
     * taps two rows must see which two are working — one boolean would have greyed out every button
     * on screen to report one call.
     */
    val refreshingFichas: Set<Int> = emptySet(),
    /** What the phone holds of the catalog's photographs (#191). Read only in the settings screen. */
    val photoCache: PhotoCacheStatus = PhotoCacheStatus(),
)

class CoindexViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Curated catalogs shipped with the app; constant for the process lifetime. */
    val catalogs get() = container.repository.catalogs

    /**
     * The card-sized name of every curated collection (#22). The plate keeps the long [name] —
     * defining the editorial scope is its job — so this is the index's, and the ficha's.
     */
    val titles get() = container.repository.titles

    /**
     * Every name a curated file claims, so the one name a collector types cannot repeat one
     * (ADR 0021 §4). Constant for the process lifetime, like the seeds it comes from.
     */
    val curatedNames: Set<String> get() = container.repository.titles.curatedNames()

    /** The name of one curated catalog, for the masthead of its plate. */
    fun catalogName(catalogId: String?): String? =
        catalogs.firstOrNull { it.id == catalogId }?.name

    private var lastUpdateCheckMillis: Long? = null

    /** The background photograph prefetch, so a sync can take the network back off it (#191). */
    private var prefetchJob: Job? = null

    /** How many fichas the last prefetch pass covered; a different number means new ones arrived. */
    private var prefetchedFichas: Int? = null

    init {
        _state.update {
            it.copy(
                versionName = container.installedVersionName(),
                lastSync = container.syncLog.last,
                indexShelf = container.shelves.index,
                coinsShelf = container.shelves.coins,
            )
        }
        start()
        checkForUpdate(force = true)
        pollForUpdates()
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
            val stored = container.credentials.credentials()
            _state.update { it.copy(onboarded = stored != null) }
            try {
                container.typeCacheSeed.topUp(container.repository.curatedTypeIds())
                // A cache seeded before version 3 has no thumbnails, and a cached type is never
                // fetched again: without this the plate would keep asking for the heavy
                // originals for ever on the phones that already have the collection (#67).
                container.typeThumbnailBackfill.run()
                refreshBudget()
                container.repository.observeState().collect { collection ->
                    _state.update { it.copy(collection = collection, loading = false) }
                    prefetchPhotographs()
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, fatalError = error.message ?: error.toString())
                }
            }
        }
    }

    /**
     * Brings the catalog's photographs into the cache, quietly, once the collection has been read
     * (#191).
     *
     * Called on **every** emission of the collection and cheap to call twice: a launch's worth of
     * photographs is fetched once, and a later emission only starts a new pass when the number of
     * fichas has changed — which is what «a sync brought new types» looks like from here. Creating
     * a box or renaming one emits too, and none of those change what there is to fetch.
     *
     * It runs in [viewModelScope] rather than an application scope on purpose: what this saves is a
     * wait the collector would otherwise see **in this app**, and every photograph is independent,
     * so being cut short when they leave costs nothing but the ones that had not been asked for yet.
     * They are asked for on the next launch.
     */
    private fun prefetchPhotographs(force: Boolean = false) {
        if (prefetchJob?.isActive == true) return
        val images = _state.value.collection.images.values.toList()
        if (images.isEmpty()) return
        if (!force && prefetchedFichas == images.size) return
        prefetchJob = viewModelScope.launch {
            // The index is drawn first. This pass opens sixteen hundred cache snapshots before it
            // asks for anything, and doing that while the first screen is still laying itself out
            // is exactly the cold start the collector would feel.
            delay(PREFETCH_START_DELAY_MILLIS)
            val conditions = container.prefetchConditions.current(syncing = _state.value.syncing)
            val held = prefetchRefusal(conditions)
            val report = container.photoPrefetch.run(images, conditions) { missing ->
                _state.update { it.copy(photoCache = it.photoCache.copy(missing = missing)) }
            }
            prefetchedFichas = images.size
            _state.update {
                it.copy(
                    photoCache = PhotoCacheStatus(
                        wanted = report.wanted,
                        missing = report.missing,
                        bytes = report.cacheBytes,
                        held = held,
                    ),
                )
            }
        }
    }

    fun saveCredentials(apiKey: String, userId: String) {
        val parsedUserId = userId.trim().toLongOrNull()
        if (apiKey.isBlank() || parsedUserId == null || parsedUserId <= 0) {
            _state.update {
                it.copy(validation = "Introduce una API key y un identificador de usuario válidos.")
            }
            return
        }
        container.credentials.save(apiKey.trim(), parsedUserId)
        _state.update { it.copy(onboarded = true, validation = null) }
    }

    /** The stored credentials and budget, so the settings screen opens on what is in effect. */
    fun currentSettings(): SettingsValues {
        val stored = container.credentials.credentials()
        return SettingsValues(
            apiKey = stored?.apiKey.orEmpty(),
            userId = stored?.userId?.toString().orEmpty(),
            budgetCap = container.credentials.monthlyBudget,
        )
    }

    /**
     * Saves the settings form as one unit, reporting inline whether it took.
     *
     * @return true when everything was stored, so the caller can leave the screen.
     */
    fun saveSettings(apiKey: String, userId: String, budgetCap: String): Boolean {
        val parsedUserId = userId.trim().toLongOrNull()
        val parsedCap = budgetCap.trim().toIntOrNull()
        val problem = when {
            apiKey.isBlank() -> "La API key no puede estar vacía."
            parsedUserId == null || parsedUserId <= 0 ->
                "El identificador de usuario es el número de la URL de tu perfil de Numista."
            parsedCap == null || parsedCap <= 0 ->
                "El techo de presupuesto tiene que ser un número de llamadas mayor que cero."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(validation = problem) }
            return false
        }
        container.credentials.save(apiKey.trim(), parsedUserId!!)
        container.credentials.monthlyBudget = parsedCap!!
        _state.update { it.copy(validation = null, message = "Ajustes guardados.") }
        viewModelScope.launch { refreshBudget() }
        return true
    }

    /**
     * Forgets the credentials and returns to onboarding.
     *
     * The collection stays on the device, and so does the record of when it was last synced:
     * signing out is «these credentials are wrong», not «throw away what we already have».
     */
    fun signOut() {
        container.credentials.clear()
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
        container.shelves.index = shelf
        _state.update { it.copy(indexShelf = shelf) }
    }

    fun narrowCoins(shelf: CoinsShelf) {
        container.shelves.coins = shelf
        _state.update { it.copy(coinsShelf = shelf) }
    }

    /**
     * Creates one of the collector's own boxes (ADR 0013, ADR 0021 §11).
     *
     * A name and at least one coin are required, and the refusal is a message rather than a silent
     * no-op: a heading over nothing is not something to store. The name has already been read by
     * `boxName` — that is where the 40-character limit and the uniqueness live — so what arrives
     * here is only ever the last line of defence.
     *
     * It says «colección» and not «agrupación», because there is one species of collection and no
     * word of provenance telling a box from the rest (ADR 0021 §2).
     */
    fun createOwnGrouping(name: String, typeIds: List<Int>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || typeIds.isEmpty()) {
            _state.update {
                it.copy(message = "Ponle un nombre a la colección y elige al menos una moneda.")
            }
            return
        }
        viewModelScope.launch {
            container.repository.createOwnGrouping(trimmed, typeIds)
            _state.update { it.copy(message = "Colección «$trimmed» creada.") }
        }
    }

    fun addToOwnGrouping(groupingId: Long, typeIds: List<Int>) {
        if (typeIds.isEmpty()) return
        viewModelScope.launch { container.repository.addToOwnGrouping(groupingId, typeIds) }
    }

    fun renameOwnGrouping(groupingId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(message = "El nombre de la colección no puede estar vacío.") }
            return
        }
        viewModelScope.launch { container.repository.renameOwnGrouping(groupingId, trimmed) }
    }

    fun removeFromOwnGrouping(groupingId: Long, typeId: Int) {
        viewModelScope.launch { container.repository.removeFromOwnGrouping(groupingId, typeId) }
    }

    fun deleteOwnGrouping(groupingId: Long) {
        viewModelScope.launch { container.repository.deleteOwnGrouping(groupingId) }
    }

    /**
     * The client, or null having said why there isn't one.
     *
     * Every gesture that spends API budget goes through here, so «falta la API key» is one sentence
     * in one place — and it is the same sentence [syncErrorLabel] gives for an empty key.
     */
    private fun clientOrComplain(): NumistaClient? {
        val client = container.numistaClient()
        if (client == null) {
            _state.update { it.copy(message = syncErrorLabel(NumistaException.EmptyApiKey())) }
        }
        return client
    }

    fun sync() {
        if (_state.value.syncing) return
        val client = clientOrComplain() ?: return
        val userId = container.credentials.credentials()?.userId ?: return
        // The photographs give the network back to the sync, which is both spending API budget and
        // being waited for. Whatever had not been fetched is picked up again when it is over (#191).
        prefetchJob?.cancel()
        _state.update { it.copy(syncing = true, message = null) }
        viewModelScope.launch {
            val outcome = runCatching { container.syncService.run(client, userId) }
            refreshBudget()
            outcome.fold(
                onSuccess = { report ->
                    // Recorded before it is announced: the snackbar is the copy, not the original.
                    val record = SyncRecord(
                        atMillis = System.currentTimeMillis(),
                        collectionItems = report.collectionItems,
                        typesFetched = report.typesFetched,
                        callsSpent = report.callsSpent,
                        partialFailure = report.partialFailure,
                    )
                    container.syncLog.last = record
                    _state.update {
                        it.copy(
                            syncing = false,
                            lastSync = record,
                            message = syncReportLabel(record),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(syncing = false, message = syncErrorLabel(error)) }
                },
            )
            // Forced, because a sync that changed nothing emits nothing, and the pass it cancelled
            // would otherwise wait for the next launch to be picked up again.
            prefetchPhotographs(force = true)
        }
    }

    /**
     * Asks Numista again for one type's ficha (#185, ADR 0023).
     *
     * One call, and the collector asked for it, so unlike the update check every outcome is spoken:
     * what changed, that nothing did, or why it could not be asked. The corrected ficha reaches the
     * screen through the same flow a sync does — nothing here pushes it — so the card the collector
     * is looking at redraws itself with the family Numista now publishes.
     */
    fun refreshFicha(typeId: Int) {
        if (typeId in _state.value.refreshingFichas) return
        val client = clientOrComplain() ?: return
        _state.update { it.copy(refreshingFichas = it.refreshingFichas + typeId, message = null) }
        viewModelScope.launch {
            val outcome = runCatching { container.typeRefresh.refresh(client, typeId) }
            refreshBudget()
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
     * Looks for a newer APK. Failures are swallowed into [UpdateStatus.Unavailable]: an update
     * check that cannot reach GitHub must never interrupt looking at the collection.
     */
    fun checkForUpdate(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && !shouldCheckForUpdate(lastUpdateCheckMillis, now)) return
        lastUpdateCheckMillis = now
        viewModelScope.launch {
            val status = container.updateChecker.check()
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
        val installer = container.updateInstaller
        if (!installer.canInstall()) {
            val opened = installer.requestInstallPermission()
            _state.update {
                it.copy(
                    message = if (opened) {
                        "Concede a Coindex permiso para instalar aplicaciones y vuelve a " +
                            "pulsar Instalar."
                    } else {
                        "Este dispositivo no permite conceder el permiso de instalación: " +
                            "descarga el APK desde GitHub e instálalo a mano."
                    },
                )
            }
            return
        }
        _state.update { it.copy(updating = true, message = "Descargando la actualización…") }
        viewModelScope.launch {
            val outcome = runCatching {
                installer.download(available.apkUrl, available.manifest.versionCode)
            }
            outcome.fold(
                onSuccess = { apk ->
                    val launched = installer.install(apk)
                    _state.update {
                        it.copy(
                            updating = false,
                            message = if (launched) {
                                null
                            } else {
                                "No hay instalador de paquetes en este dispositivo: instala " +
                                    "el APK a mano."
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            updating = false,
                            message = "No se pudo descargar la actualización: ${error.message}",
                        )
                    }
                },
            )
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
     * The given cards as printable pages, in the order they arrive (#169).
     *
     * Built on demand and never observed: [cards] is what the index was showing when the button was
     * pressed, which is exactly what the collector chose to print. There is no `Notebook` behind it
     * — no table, no name, no second order (ADR 0021 §1).
     */
    fun notebookPages(cards: List<IndexCard>): List<PrintPage> = printPages(
        notebookSections(
            state = _state.value.collection,
            cards = cards,
            catalogs = container.repository.catalogs,
            programmes = container.repository.programmes,
        ),
    )

    fun plate(catalogId: String): PlateResult =
        resolvePlate(
            _state.value.collection,
            container.repository.catalogs,
            catalogId,
            container.repository.programmes,
        )

    // The pair that used to answer «is there a catalog for this key, and would its plate open?»
    // left with the screen that asked: a card with a reachable plate now *is* the plate (ADR 0021
    // §9), so nothing between the index and the plate needs to explain a jump it cannot make.

    private suspend fun refreshBudget() {
        val cap = container.credentials.monthlyBudget
        val used = container.database.apiCalls()
            .countSince(startOfMonthMillis(System.currentTimeMillis()))
        _state.update { it.copy(budget = BudgetStatus(used, cap)) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CoindexViewModel(container) as T
            }
    }
}
