package com.mg.costeoapp.feature.prefabricados.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrefabricadoDetailViewModel @Inject constructor(
    private val repository: PrefabricadoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrefabricadoDetailUiState())
    val uiState: StateFlow<PrefabricadoDetailUiState> = _uiState.asStateFlow()

    private val prefabricadoId: Long = savedStateHandle.get<Long>("recetaId") ?: 0L
    private var observeJob: Job? = null

    init {
        if (prefabricadoId != 0L) {
            loadDetail()
        }
    }

    fun refresh() {
        loadDetail()
    }

    private fun loadDetail() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeConIngredientes(prefabricadoId).collect { data ->
                if (data != null) {
                    val costeo = repository.calculateCost(prefabricadoId)
                    val nutricion = repository.calculateNutricion(prefabricadoId)

                    _uiState.update {
                        it.copy(
                            prefabricado = data.prefabricado,
                            ingredientes = data.ingredientes,
                            costeo = costeo,
                            nutricion = nutricion,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            repository.getVariantes(prefabricadoId).collect { variantes ->
                _uiState.update { it.copy(variantes = variantes) }
            }
        }
    }

    fun softDelete() {
        viewModelScope.launch { repository.softDelete(prefabricadoId) }
    }
}
