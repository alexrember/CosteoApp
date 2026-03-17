package com.mg.costeoapp.feature.platos.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.platos.data.PlatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlatoDetailViewModel @Inject constructor(
    private val repository: PlatoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlatoDetailUiState())
    val uiState: StateFlow<PlatoDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val platoId: Long = savedStateHandle.get<Long>("platoId") ?: 0L
    private var loadJob: Job? = null

    init {
        if (platoId != 0L) refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val plato = repository.getById(platoId) ?: return@launch
                val componentes = repository.getComponentesDetalle(platoId)
                val costeo = repository.calculateCost(platoId)
                val precioVenta = repository.calculatePrecioVenta(plato, costeo.costoTotal)

                _uiState.update {
                    it.copy(
                        plato = plato,
                        componentesDetalle = componentes,
                        costeo = costeo,
                        precioVenta = precioVenta,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun softDelete() {
        viewModelScope.launch {
            repository.softDelete(platoId)
            _events.send(UiEvent.NavigateBack)
        }
    }
}
