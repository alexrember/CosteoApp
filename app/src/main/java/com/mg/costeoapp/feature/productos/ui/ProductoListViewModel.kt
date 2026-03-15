package com.mg.costeoapp.feature.productos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductoListViewModel @Inject constructor(
    private val repository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoListUiState())
    val uiState: StateFlow<ProductoListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadProductos()
    }

    private fun loadProductos() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val flow = if (_uiState.value.searchQuery.isBlank()) {
                repository.getAll()
            } else {
                repository.search(_uiState.value.searchQuery)
            }
            flow.collect { productos ->
                _uiState.update { it.copy(productos = productos, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadProductos()
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
