package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.inventario.data.InventarioRepository
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CarritoViewModel @Inject constructor(
    private val inventarioRepository: InventarioRepository,
    private val tiendaRepository: TiendaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarritoUiState())
    val uiState: StateFlow<CarritoUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val tiendas = tiendaRepository.getAll().first()
            _uiState.update { it.copy(tiendasDisponibles = tiendas) }
        }
    }

    fun seleccionarTienda(tienda: Tienda) {
        _uiState.update { it.copy(tiendaSeleccionada = tienda) }
    }

    fun agregarItem(producto: Producto, cantidad: Double, precioUnitario: Long) {
        val tienda = _uiState.value.tiendaSeleccionada ?: return
        val item = CarritoItem(
            producto = producto,
            tienda = tienda,
            cantidad = cantidad,
            precioUnitario = precioUnitario
        )
        _uiState.update { it.copy(items = it.items + item) }
    }

    fun removerItem(index: Int) {
        _uiState.update {
            it.copy(items = it.items.toMutableList().apply { removeAt(index) })
        }
    }

    fun confirmarCompra() {
        if (_uiState.value.isConfirming || _uiState.value.isEmpty) return

        _uiState.update { it.copy(isConfirming = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val inventarioItems = _uiState.value.items.map { item ->
                Inventario(
                    productoId = item.producto.id,
                    tiendaId = item.tienda.id,
                    cantidad = item.cantidad,
                    precioCompra = item.precioUnitario,
                    fechaCompra = now
                )
            }

            inventarioRepository.confirmarCompra(inventarioItems).fold(
                onSuccess = {
                    _uiState.update { it.copy(items = emptyList(), isConfirming = false) }
                    _events.send(UiEvent.SaveSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isConfirming = false) }
                    _events.send(UiEvent.ShowError(e.message ?: "Error al confirmar compra"))
                }
            )
        }
    }

    fun limpiarCarrito() {
        _uiState.update { it.copy(items = emptyList()) }
    }
}
