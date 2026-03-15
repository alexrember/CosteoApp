package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.inventario.data.CompraManager
import com.mg.costeoapp.feature.inventario.data.InventarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarritoViewModel @Inject constructor(
    private val inventarioRepository: InventarioRepository,
    private val compraManager: CompraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarritoUiState())
    val uiState: StateFlow<CarritoUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            compraManager.items.collect { items ->
                _uiState.update {
                    it.copy(tienda = compraManager.getTienda(), items = items)
                }
            }
        }
    }

    fun removerItem(index: Int) {
        compraManager.removerItem(index)
    }

    fun confirmarCompra() {
        if (_uiState.value.isConfirming || _uiState.value.isEmpty) return

        val tienda = compraManager.getTienda() ?: return

        _uiState.update { it.copy(isConfirming = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val inventarioItems = _uiState.value.items.map { item ->
                Inventario(
                    productoId = item.producto.id,
                    tiendaId = tienda.id,
                    cantidad = item.cantidad,
                    precioCompra = item.precioUnitario,
                    fechaCompra = now
                )
            }

            inventarioRepository.confirmarCompra(inventarioItems).fold(
                onSuccess = {
                    compraManager.limpiar()
                    _uiState.update { it.copy(isConfirming = false) }
                    _events.send(UiEvent.SaveSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isConfirming = false) }
                    _events.send(UiEvent.ShowError(e.message ?: "Error al confirmar compra"))
                }
            )
        }
    }
}
