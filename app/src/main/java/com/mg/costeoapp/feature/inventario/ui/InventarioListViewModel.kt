package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.inventario.data.InventarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventarioListViewModel @Inject constructor(
    private val repository: InventarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventarioListUiState())
    val uiState: StateFlow<InventarioListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInventario()
        loadResumen()
    }

    private fun loadInventario(debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            val flow = if (_uiState.value.searchQuery.isBlank()) {
                repository.getInventarioDisponible()
            } else {
                repository.buscarInventario(_uiState.value.searchQuery)
            }
            flow.collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    private fun loadResumen() {
        viewModelScope.launch {
            repository.getResumenPorTienda().collect { resumen ->
                _uiState.update { it.copy(resumenPorTienda = resumen) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadInventario(debounce = true)
    }

    fun marcarAgotado(inventarioId: Long) {
        viewModelScope.launch { repository.marcarAgotado(inventarioId) }
    }

    fun softDelete(inventarioId: Long) {
        viewModelScope.launch { repository.softDelete(inventarioId) }
    }
}
