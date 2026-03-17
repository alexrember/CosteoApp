package com.mg.costeoapp.feature.platos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.platos.data.PlatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlatoListViewModel @Inject constructor(
    private val repository: PlatoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlatoListUiState())
    val uiState: StateFlow<PlatoListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init { loadPlatos() }

    private fun loadPlatos(debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                if (debounce) delay(300)
                val flow = if (_uiState.value.searchQuery.isBlank()) {
                    repository.getAllActive()
                } else {
                    repository.search(_uiState.value.searchQuery)
                }
                flow.distinctUntilChanged().collect { platos ->
                    val items = platos.map { plato ->
                        async {
                            val costeo = repository.calculateCost(plato.id)
                            val precioVenta = repository.calculatePrecioVenta(plato, costeo.costoTotal)
                            PlatoConCosto(
                                plato = plato,
                                costoTotal = costeo.costoTotal,
                                precioVenta = precioVenta,
                                tieneAdvertencias = costeo.advertencias.isNotEmpty()
                            )
                        }
                    }.awaitAll()
                    _uiState.update { it.copy(items = items, isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadPlatos(debounce = true)
    }

    fun softDelete(id: Long) {
        viewModelScope.launch { repository.softDelete(id) }
    }
}
