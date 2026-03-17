package com.mg.costeoapp.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.Producto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchResultItem {
    data class ProductoResult(val producto: Producto) : SearchResultItem()
    data class RecetaResult(val prefabricado: Prefabricado) : SearchResultItem()
    data class PlatoResult(val plato: Plato) : SearchResultItem()
}

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val isActive: Boolean = false
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val productoDao: ProductoDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val platoDao: PlatoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length < 2) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val productos = productoDao.search(query).first().take(10)
                    .map { SearchResultItem.ProductoResult(it) }
                val recetas = prefabricadoDao.search(query).first().take(10)
                    .map { SearchResultItem.RecetaResult(it) }
                val platos = platoDao.search(query).first().take(10)
                    .map { SearchResultItem.PlatoResult(it) }

                _uiState.update {
                    it.copy(results = productos + recetas + platos, isSearching = false)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun onActivate() { _uiState.update { it.copy(isActive = true) } }
    fun onDismiss() { _uiState.update { GlobalSearchUiState() } }
}
