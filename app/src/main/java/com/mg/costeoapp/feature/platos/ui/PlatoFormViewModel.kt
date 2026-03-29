package com.mg.costeoapp.feature.platos.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.ErrorMapper
import com.mg.costeoapp.core.util.ValidationUtils
import com.mg.costeoapp.core.domain.engine.PricingEngine
import com.mg.costeoapp.feature.platos.data.PlatoRepository
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import com.mg.costeoapp.feature.sync.data.SyncManager
import kotlin.math.roundToLong
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
class PlatoFormViewModel @Inject constructor(
    private val platoRepository: PlatoRepository,
    private val prefabricadoRepository: PrefabricadoRepository,
    private val productoRepository: ProductoRepository,
    private val pricingEngine: PricingEngine,
    private val syncManager: SyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlatoFormUiState())
    val uiState: StateFlow<PlatoFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val platoId: Long = savedStateHandle.get<Long>("platoId") ?: 0L

    init {
        if (platoId != 0L) loadForEdit(platoId)
    }

    private fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val plato = platoRepository.getById(id) ?: return@launch
            val componentes = platoRepository.getComponentes(id)

            val items = componentes.map { comp ->
                when {
                    comp.prefabricadoId != null -> {
                        val pref = prefabricadoRepository.getConIngredientes(comp.prefabricadoId)
                        ComponenteFormItem(
                            prefabricado = pref?.prefabricado,
                            cantidad = comp.cantidad.toString(),
                            nombre = pref?.prefabricado?.nombre ?: "?"
                        )
                    }
                    comp.productoId != null -> {
                        val prod = productoRepository.getById(comp.productoId)
                        ComponenteFormItem(
                            producto = prod,
                            cantidad = comp.cantidad.toString(),
                            nombre = prod?.nombre ?: "?"
                        )
                    }
                    else -> ComponenteFormItem(nombre = "?")
                }
            }

            _uiState.update {
                it.copy(
                    plato = plato,
                    nombre = plato.nombre,
                    descripcion = plato.descripcion ?: "",
                    margenPorcentaje = plato.margenPorcentaje?.toString() ?: "35",
                    precioVentaManual = plato.precioVentaManual?.let { p -> CurrencyFormatter.fromCents(p).replace("$", "") } ?: "",
                    componentes = items
                )
            }
        }
    }

    fun onNombreChanged(v: String) { _uiState.update { it.copy(nombre = v, fieldErrors = it.fieldErrors - "nombre") } }
    fun onDescripcionChanged(v: String) { _uiState.update { it.copy(descripcion = v) } }
    fun onMargenChanged(v: String) { _uiState.update { it.copy(margenPorcentaje = v) }; recalculateCost() }
    fun onPrecioVentaManualChanged(v: String) { _uiState.update { it.copy(precioVentaManual = v) } }

    fun onShowComponentePicker() {
        viewModelScope.launch {
            val prefs = prefabricadoRepository.getAll().first()
            val prods = productoRepository.getAll().first()
            _uiState.update {
                it.copy(
                    showComponentePicker = true,
                    prefabricadosDisponibles = prefs,
                    productosDisponibles = prods,
                    pickerSearchQuery = ""
                )
            }
        }
    }

    fun onDismissComponentePicker() { _uiState.update { it.copy(showComponentePicker = false) } }
    fun onPickerSearchChanged(q: String) { _uiState.update { it.copy(pickerSearchQuery = q) } }
    fun onPickerTabChanged(tab: Int) { _uiState.update { it.copy(pickerTab = tab) } }

    fun onAddPrefabricado(pref: com.mg.costeoapp.core.database.entity.Prefabricado) {
        _uiState.update {
            if (it.componentes.any { c -> c.prefabricado?.id == pref.id }) return@update it
            it.copy(
                componentes = it.componentes + ComponenteFormItem(prefabricado = pref, nombre = pref.nombre),
                showComponentePicker = false
            )
        }
        recalculateCost()
    }

    fun onAddProducto(prod: com.mg.costeoapp.core.database.entity.Producto) {
        _uiState.update {
            if (it.componentes.any { c -> c.producto?.id == prod.id }) return@update it
            it.copy(
                componentes = it.componentes + ComponenteFormItem(producto = prod, nombre = prod.nombre),
                showComponentePicker = false
            )
        }
        recalculateCost()
    }

    fun onUpdateCantidad(index: Int, cantidad: String) {
        _uiState.update {
            it.copy(componentes = it.componentes.toMutableList().apply {
                this[index] = this[index].copy(cantidad = cantidad)
            })
        }
        recalculateCost()
    }

    fun onRemoveComponente(index: Int) {
        _uiState.update {
            it.copy(componentes = it.componentes.toMutableList().apply { removeAt(index) })
        }
        recalculateCost()
    }

    private var costJob: kotlinx.coroutines.Job? = null

    private fun recalculateCost() {
        costJob?.cancel()
        costJob = viewModelScope.launch {
            val state = _uiState.value
            if (state.componentes.isEmpty()) {
                _uiState.update { it.copy(costoEnVivo = null, precioVentaSugerido = null) }
                return@launch
            }

            var costoTotal = 0L
            val updatedComponentes = state.componentes.map { item ->
                val cantidad = item.cantidad.toDoubleOrNull()
                if (cantidad == null || cantidad <= 0) return@map item.copy(costoCalculado = null)

                val costoItem = if (item.prefabricado != null) {
                    val result = pricingEngine.calculatePrefabricadoCost(item.prefabricado.id)
                    val costoPorPorcion = result.costoPorPorcion ?: result.costoTotal
                    (costoPorPorcion * cantidad).roundToLong()
                } else if (item.producto != null) {
                    val precio = pricingEngine.resolvePrice(item.producto.id)
                    val precioUnitario = precio.precioUnitario ?: 0L
                    val contenidoTotal = item.producto.cantidadPorEmpaque * maxOf(item.producto.unidadesPorEmpaque, 1)
                    if (contenidoTotal <= 0) 0L
                    else (precioUnitario.toDouble() / contenidoTotal * cantidad).roundToLong()
                } else 0L

                costoTotal += costoItem
                item.copy(costoCalculado = costoItem)
            }

            val margen = state.margenPorcentaje.toDoubleOrNull()
            val precioVenta = if (margen != null && margen > 0 && margen < 100 && costoTotal > 0) {
                (costoTotal.toDouble() / (1.0 - margen / 100.0)).roundToLong()
            } else null

            _uiState.update { it.copy(componentes = updatedComponentes, costoEnVivo = costoTotal, precioVentaSugerido = precioVenta) }
        }
    }

    fun save() {
        if (!validate()) return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val plato = Plato(
                id = if (state.isEditMode) state.plato!!.id else 0,
                nombre = state.nombre.trim(),
                descripcion = state.descripcion.trim().ifBlank { null },
                margenPorcentaje = state.margenPorcentaje.toDoubleOrNull(),
                precioVentaManual = CurrencyFormatter.toCents(state.precioVentaManual),
                createdAt = state.plato?.createdAt ?: System.currentTimeMillis()
            )

            val componentes = state.componentes
                .filter { item ->
                    val hasPref = item.prefabricado != null
                    val hasProd = item.producto != null
                    (hasPref xor hasProd) && (item.cantidad.toDoubleOrNull() ?: 0.0) > 0
                }
                .map { item ->
                    PlatoComponente(
                        platoId = 0,
                        prefabricadoId = item.prefabricado?.id,
                        productoId = item.producto?.id,
                        cantidad = item.cantidad.toDoubleOrNull() ?: 1.0
                    )
                }

            if (state.isEditMode) {
                platoRepository.updatePlato(plato, componentes).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSaving = false) }
                        syncManager.pushInBackground()
                        _events.send(UiEvent.SaveSuccess)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(UiEvent.ShowError(ErrorMapper.toUserMessage(e)))
                    }
                )
            } else {
                platoRepository.createPlato(plato, componentes).fold(
                    onSuccess = { newId ->
                        _uiState.update { it.copy(isSaving = false) }
                        syncManager.pushInBackground()
                        _events.send(UiEvent.SaveSuccessWithId(newId))
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(UiEvent.ShowError(ErrorMapper.toUserMessage(e)))
                    }
                )
            }
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value
        if (!ValidationUtils.isValidName(state.nombre)) errors["nombre"] = "Nombre requerido (min 2 chars)"
        if (state.componentes.isEmpty()) errors["componentes"] = "Agrega al menos un componente"
        val margen = state.margenPorcentaje.toDoubleOrNull()
        if (margen != null && (margen <= 0 || margen >= 100)) errors["margen"] = "Margen debe ser 1-99%"
        val invalidXor = state.componentes.any { item ->
            val hasPref = item.prefabricado != null
            val hasProd = item.producto != null
            (hasPref && hasProd) || (!hasPref && !hasProd)
        }
        if (invalidXor) errors["componentes"] = "Cada componente debe tener un prefabricado o un producto (no ambos)"
        val invalidCantidad = state.componentes.any { item ->
            val c = item.cantidad.toDoubleOrNull()
            c == null || c <= 0
        }
        if (invalidCantidad) errors["cantidad"] = "Todas las cantidades deben ser mayores a 0"
        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
