package com.mg.costeoapp.feature.platos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.domain.engine.PricingEngine
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.platos.data.PlatoRepository
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import com.mg.costeoapp.feature.productos.data.ProductoRepository
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
import kotlin.math.roundToLong

@HiltViewModel
class SimuladorViewModel @Inject constructor(
    private val pricingEngine: PricingEngine,
    private val platoRepository: PlatoRepository,
    private val prefabricadoRepository: PrefabricadoRepository,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimuladorUiState())
    val uiState: StateFlow<SimuladorUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Picker state
    private val _showPicker = MutableStateFlow(false)
    val showPicker: StateFlow<Boolean> = _showPicker.asStateFlow()

    private val _prefabricados = MutableStateFlow<List<Prefabricado>>(emptyList())
    val prefabricados: StateFlow<List<Prefabricado>> = _prefabricados.asStateFlow()

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos.asStateFlow()

    fun onMargenChanged(v: String) {
        _uiState.update { it.copy(margenPorcentaje = v) }
        recalcular()
    }

    fun onShowPicker() {
        viewModelScope.launch {
            _prefabricados.value = prefabricadoRepository.getAll().first()
            _productos.value = productoRepository.getAll().first()
            _showPicker.value = true
        }
    }

    fun onDismissPicker() { _showPicker.value = false }

    fun onAddPrefabricado(pref: Prefabricado) {
        _uiState.update {
            it.copy(componentes = it.componentes + ComponenteFormItem(prefabricado = pref, nombre = pref.nombre))
        }
        _showPicker.value = false
        recalcular()
    }

    fun onAddProducto(prod: Producto) {
        _uiState.update {
            it.copy(componentes = it.componentes + ComponenteFormItem(producto = prod, nombre = prod.nombre))
        }
        _showPicker.value = false
        recalcular()
    }

    fun onUpdateCantidad(index: Int, cantidad: String) {
        _uiState.update {
            it.copy(componentes = it.componentes.toMutableList().apply {
                this[index] = this[index].copy(cantidad = cantidad)
            })
        }
        recalcular()
    }

    fun onRemoveComponente(index: Int) {
        _uiState.update {
            it.copy(componentes = it.componentes.toMutableList().apply { removeAt(index) })
        }
        recalcular()
    }

    fun onLimpiar() {
        _uiState.value = SimuladorUiState()
    }

    fun onShowGuardarDialog() {
        _uiState.update { it.copy(showGuardarDialog = true) }
    }

    fun onDismissGuardarDialog() {
        _uiState.update { it.copy(showGuardarDialog = false) }
    }

    fun onNombreParaGuardarChanged(v: String) {
        _uiState.update { it.copy(nombreParaGuardar = v) }
    }

    fun guardarComoPlato() {
        val state = _uiState.value
        if (state.nombreParaGuardar.isBlank()) return

        viewModelScope.launch {
            val margen = state.margenPorcentaje.toDoubleOrNull()
            val plato = Plato(
                nombre = state.nombreParaGuardar.trim(),
                margenPorcentaje = margen
            )

            val componentes = state.componentes.map { item ->
                PlatoComponente(
                    platoId = 0,
                    prefabricadoId = item.prefabricado?.id,
                    productoId = item.producto?.id,
                    cantidad = item.cantidad.toDoubleOrNull() ?: 1.0
                )
            }

            platoRepository.createPlato(plato, componentes).fold(
                onSuccess = {
                    _uiState.update { it.copy(showGuardarDialog = false) }
                    _events.send(UiEvent.ShowError("Plato guardado exitosamente"))
                    onLimpiar()
                },
                onFailure = { e ->
                    _events.send(UiEvent.ShowError(e.message ?: "Error al guardar"))
                }
            )
        }
    }

    private fun recalcular() {
        viewModelScope.launch {
            var costoTotal = 0L

            for (item in _uiState.value.componentes) {
                val cantidad = item.cantidad.toDoubleOrNull() ?: 0.0
                when {
                    item.prefabricado != null -> {
                        val costeo = pricingEngine.calculatePrefabricadoCost(item.prefabricado.id)
                        val costoPorPorcion = costeo.costoPorPorcion ?: 0L
                        costoTotal += (costoPorPorcion * cantidad).roundToLong()
                    }
                    item.producto != null -> {
                        val precio = pricingEngine.resolvePrice(item.producto.id)
                        if (precio.precioUnitario != null) {
                            val ppu = precio.precioUnitario.toDouble() / item.producto.cantidadPorEmpaque
                            costoTotal += (ppu * cantidad).roundToLong()
                        }
                    }
                }
            }

            val margen = _uiState.value.margenPorcentaje.toDoubleOrNull()
            val precioVenta = if (margen != null && margen > 0 && margen < 100) {
                (costoTotal.toDouble() / (1.0 - margen / 100.0)).roundToLong()
            } else null

            _uiState.update {
                it.copy(costoTotal = costoTotal, precioVentaSugerido = precioVenta)
            }
        }
    }
}
