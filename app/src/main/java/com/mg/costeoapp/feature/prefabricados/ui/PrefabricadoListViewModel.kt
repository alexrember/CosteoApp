package com.mg.costeoapp.feature.prefabricados.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrefabricadoListViewModel @Inject constructor(
    private val repository: PrefabricadoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrefabricadoListUiState())
    val uiState: StateFlow<PrefabricadoListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPrefabricados()
    }

    private fun loadPrefabricados(debounce: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            val flow = if (_uiState.value.searchQuery.isBlank()) {
                repository.getAll()
            } else {
                repository.search(_uiState.value.searchQuery)
            }
            flow.collect { prefabricados ->
                try {
                    val items = prefabricados.map { pref ->
                        async {
                            val costeo = repository.calculateCost(pref.id)
                            PrefabricadoConCosto(
                                prefabricado = pref,
                                costoPorPorcion = costeo.costoPorPorcion,
                                tieneAdvertencias = costeo.advertencias.isNotEmpty()
                            )
                        }
                    }.awaitAll()
                    _uiState.update { it.copy(items = items, isLoading = false, error = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar recetas") }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadPrefabricados(debounce = true)
    }

    fun softDelete(id: Long) {
        viewModelScope.launch { repository.softDelete(id) }
    }
}
