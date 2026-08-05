package com.jenarvaezg.coindex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jenarvaezg.coindex.AppContainer
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.SyncRecord
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    /** The name of one curated catalog, for the masthead of its plate. */
    fun catalogName(catalogId: String?): String? =
        catalogs.firstOrNull { it.id == catalogId }?.name

    private var lastUpdateCheckMillis: Long? = null

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
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, fatalError = error.message ?: error.toString())
                }
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
     * Creates one of the collector's own groupings (ADR 0013).
     *
     * A name and at least one piece are required, and the refusal is a message rather than a
     * silent no-op: a heading over nothing is not something to store.
     */
    fun createOwnGrouping(name: String, typeIds: List<Int>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || typeIds.isEmpty()) {
            _state.update {
                it.copy(message = "Ponle un nombre a la agrupación y elige al menos una pieza.")
            }
            return
        }
        viewModelScope.launch {
            container.repository.createOwnGrouping(trimmed, typeIds)
            _state.update { it.copy(message = "Agrupación «$trimmed» creada.") }
        }
    }

    fun addToOwnGrouping(groupingId: Long, typeIds: List<Int>) {
        if (typeIds.isEmpty()) return
        viewModelScope.launch { container.repository.addToOwnGrouping(groupingId, typeIds) }
    }

    fun renameOwnGrouping(groupingId: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(message = "El nombre de la agrupación no puede estar vacío.") }
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

    fun sync() {
        if (_state.value.syncing) return
        val client = container.numistaClient()
        if (client == null) {
            _state.update { it.copy(message = "Falta la API key de Numista. Añádela en Ajustes.") }
            return
        }
        val userId = container.credentials.credentials()?.userId ?: return
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
