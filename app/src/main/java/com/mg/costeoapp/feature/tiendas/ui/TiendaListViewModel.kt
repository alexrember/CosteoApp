package com.mg.costeoapp.feature.tiendas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.sync.data.SyncManager
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TiendaListViewModel @Inject constructor(
    private val repository: TiendaRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TiendaListUiState())
    val uiState: StateFlow<TiendaListUiState> = _uiState.asStateFlow()

    init {
        loadTiendas()
    }

    private fun loadTiendas() {
        viewModelScope.launch {
            repository.getAllIncludingInactive().collect { tiendas ->
                _uiState.update { it.copy(tiendas = tiendas, isLoading = false) }
            }
        }
    }

    fun toggleTienda(id: Long) {
        viewModelScope.launch {
            val tienda = _uiState.value.tiendas.find { it.id == id } ?: return@launch
            repository.update(tienda.copy(activo = !tienda.activo, updatedAt = System.currentTimeMillis()))
            syncManager.pushInBackground()
        }
    }
}
