package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.feature.inventario.data.CompraManager
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeleccionTiendaViewModel @Inject constructor(
    private val tiendaRepository: TiendaRepository,
    private val compraManager: CompraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeleccionTiendaUiState())
    val uiState: StateFlow<SeleccionTiendaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tiendas = tiendaRepository.getAll().first()
            _uiState.update { it.copy(tiendas = tiendas, isLoading = false) }
        }
    }

    fun seleccionarTienda(tienda: Tienda) {
        viewModelScope.launch { compraManager.iniciarCompra(tienda) }
    }
}
