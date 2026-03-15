package com.mg.costeoapp.feature.productos.ui

import androidx.lifecycle.SavedStateHandle
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
class ProductoDetailViewModel @Inject constructor(
    private val repository: ProductoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoDetailUiState())
    val uiState: StateFlow<ProductoDetailUiState> = _uiState.asStateFlow()

    private val productoId: Long = savedStateHandle.get<Long>("productoId") ?: 0L
    private var preciosJob: Job? = null

    init {
        if (productoId != 0L) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val producto = repository.getById(productoId)
            val precioReciente = repository.getPrecioMasReciente(productoId)
            _uiState.update {
                it.copy(
                    producto = producto,
                    precioMasReciente = precioReciente?.precio,
                    isLoading = false
                )
            }
        }
        preciosJob?.cancel()
        preciosJob = viewModelScope.launch {
            repository.getPreciosConTienda(productoId).collect { precios ->
                _uiState.update { it.copy(preciosConTienda = precios) }
            }
        }
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
