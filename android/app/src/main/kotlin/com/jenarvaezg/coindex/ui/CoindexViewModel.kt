package com.jenarvaezg.coindex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jenarvaezg.coindex.AppContainer
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.resolvePlate
import com.jenarvaezg.coindex.data.startOfMonthMillis
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

data class UiState(
    val onboarded: Boolean = false,
    val loading: Boolean = true,
    val syncing: Boolean = false,
    val collection: CollectionState = CollectionState(),
    val budget: BudgetStatus = BudgetStatus(0, 0),
    val message: String? = null,
    val fatalError: String? = null,
)

class CoindexViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Curated catalogs shipped with the app; constant for the process lifetime. */
    val catalogs get() = container.repository.catalogs

    init {
        start()
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
                it.copy(message = "Introduce una API key y un identificador de usuario válidos.")
            }
            return
        }
        container.credentials.save(apiKey.trim(), parsedUserId)
        _state.update { it.copy(onboarded = true, message = null) }
    }

    fun signOut() {
        container.credentials.clear()
        _state.update { it.copy(onboarded = false) }
    }

    fun setBudgetCap(cap: Int) {
        container.credentials.monthlyBudget = cap
        viewModelScope.launch { refreshBudget() }
    }

    fun setDisposition(key: CollectionProposalKey, disposition: ProposalDisposition?) {
        viewModelScope.launch { container.repository.setDisposition(key, disposition) }
    }

    fun sync() {
        if (_state.value.syncing) return
        val client = container.numistaClient()
        if (client == null) {
            _state.update { it.copy(message = "Falta la API key: vuelve al alta.") }
            return
        }
        val userId = container.credentials.credentials()?.userId ?: return
        _state.update { it.copy(syncing = true, message = null) }
        viewModelScope.launch {
            val outcome = runCatching { container.syncService.run(client, userId) }
            refreshBudget()
            val message = outcome.fold(
                onSuccess = { report ->
                    buildString {
                        append("${report.collectionItems} piezas · ")
                        append("${report.typesFetched} fichas nuevas · ")
                        append("${report.callsSpent} llamadas")
                        report.partialFailure?.let { append(" · incompleto: $it") }
                    }
                },
                onFailure = { error -> error.message ?: error.toString() },
            )
            _state.update { it.copy(syncing = false, message = message) }
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
