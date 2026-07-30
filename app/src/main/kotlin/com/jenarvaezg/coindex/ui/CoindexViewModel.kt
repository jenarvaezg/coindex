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
import kotlinx.coroutines.delay
import com.jenarvaezg.coindex.domain.CollectionProposalKey
import com.jenarvaezg.coindex.domain.ProposalDisposition
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
)

class CoindexViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Curated catalogs shipped with the app; constant for the process lifetime. */
    val catalogs get() = container.repository.catalogs

    /** The name of one curated catalog, for the masthead of its plate. */
    fun catalogName(catalogId: String?): String? =
        catalogs.firstOrNull { it.id == catalogId }?.name

    private var lastUpdateCheckMillis: Long? = null

    init {
        _state.update {
            it.copy(
                versionName = container.installedVersionName(),
                lastSync = container.syncLog.last,
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
                container.typeCacheSeed.seedIfNeeded()
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

    fun setDisposition(key: CollectionProposalKey, disposition: ProposalDisposition?) {
        viewModelScope.launch { container.repository.setDisposition(key, disposition) }
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

    fun plate(catalogId: String): PlateResult =
        resolvePlate(_state.value.collection, container.repository.catalogs, catalogId)

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
