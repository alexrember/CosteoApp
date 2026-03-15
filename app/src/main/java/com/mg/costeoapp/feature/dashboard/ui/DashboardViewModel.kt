package com.mg.costeoapp.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalTiendas: Int = 0,
    val totalProductos: Int = 0,
    val totalPrecios: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tiendaRepository: TiendaRepository,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tiendaRepository.getAll().collect { tiendas ->
                _uiState.update { it.copy(totalTiendas = tiendas.size) }
            }
        }
        viewModelScope.launch {
            productoRepository.getAll().collect { productos ->
                _uiState.update { it.copy(totalProductos = productos.size) }
            }
        }
    }
}
