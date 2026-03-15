package com.mg.costeoapp.feature.tiendas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
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
class TiendaListViewModel @Inject constructor(
    private val repository: TiendaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TiendaListUiState())
    val uiState: StateFlow<TiendaListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTiendas()
    }

    private fun loadTiendas(debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            val flow = if (_uiState.value.searchQuery.isBlank()) {
                repository.getAll()
            } else {
                repository.search(_uiState.value.searchQuery)
            }
            flow.collect { tiendas ->
                _uiState.update { it.copy(tiendas = tiendas, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadTiendas(debounce = true)
    }

    fun softDelete(id: Long) {
        viewModelScope.launch {
            repository.softDelete(id)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
