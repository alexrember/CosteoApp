package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.UnidadMedida
import com.mg.costeoapp.core.util.ValidationUtils
import com.mg.costeoapp.feature.inventario.data.CompraManager
import com.mg.costeoapp.feature.inventario.data.InventarioRepository
import com.mg.costeoapp.feature.inventario.data.repository.WalmartStoreRepository
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class ProductoRegistroUiState(
    val codigoBarras: String = "",
    val nombre: String = "",
    val unidadMedida: UnidadMedida = UnidadMedida.LIBRA,
    val cantidadPorEmpaque: String = "",
    val precio: String = "",
    val tiendaNombre: String = "",
    val buscandoEnApi: Boolean = false,
    val fuenteApi: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false
)

@HiltViewModel
class ProductoRegistroViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val inventarioRepository: InventarioRepository,
    private val walmartRepository: WalmartStoreRepository,
    private val compraManager: CompraManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoRegistroUiState())
    val uiState: StateFlow<ProductoRegistroUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val codigoBarras = savedStateHandle.get<String>("codigoBarras") ?: ""
        val tienda = compraManager.getTienda()

        _uiState.update {
            it.copy(
                codigoBarras = codigoBarras,
                tiendaNombre = tienda?.nombre ?: ""
            )
        }

        // Buscar en Walmart si tenemos codigo de barras
        if (codigoBarras.isNotBlank()) {
            buscarEnApi(codigoBarras)
        }
    }

    private fun buscarEnApi(barcode: String) {
        _uiState.update { it.copy(buscandoEnApi = true) }

        viewModelScope.launch {
            val results = withTimeoutOrNull(8000L) {
                walmartRepository.searchByBarcode(barcode).getOrNull()
            }

            val best = results?.firstOrNull { it.isAvailable }

            if (best != null) {
                _uiState.update {
                    it.copy(
                        buscandoEnApi = false,
                        nombre = best.productName,
                        precio = best.price?.let { p -> CurrencyFormatter.fromCents(p).replace("$", "") } ?: "",
                        fuenteApi = "Walmart SV"
                    )
                }
            } else {
                _uiState.update { it.copy(buscandoEnApi = false) }
            }
        }
    }

    fun onNombreChanged(value: String) {
        _uiState.update { it.copy(nombre = value, fieldErrors = it.fieldErrors - "nombre") }
    }

    fun onUnidadMedidaChanged(value: UnidadMedida) {
        _uiState.update { it.copy(unidadMedida = value) }
    }

    fun onCantidadPorEmpaqueChanged(value: String) {
        _uiState.update { it.copy(cantidadPorEmpaque = value, fieldErrors = it.fieldErrors - "cantidadPorEmpaque") }
    }

    fun onPrecioChanged(value: String) {
        _uiState.update { it.copy(precio = value, fieldErrors = it.fieldErrors - "precio") }
    }

    fun save() {
        if (!validate()) return
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val precioCents = CurrencyFormatter.toCents(state.precio)!!
            val tienda = compraManager.getTienda()
            val now = System.currentTimeMillis()

            // 1. Crear producto
            val producto = Producto(
                nombre = state.nombre.trim(),
                codigoBarras = state.codigoBarras.ifBlank { null },
                unidadMedida = state.unidadMedida.codigo,
                cantidadPorEmpaque = state.cantidadPorEmpaque.toDouble()
            )

            val productoResult = productoRepository.insert(producto)
            if (productoResult.isFailure) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(UiEvent.ShowError(productoResult.exceptionOrNull()?.message ?: "Error al crear producto"))
                return@launch
            }

            val productoId = productoResult.getOrThrow()
            val productoCreado = producto.copy(id = productoId)

            // 2. Agregar al carrito si estamos en flujo de compras
            if (tienda != null) {
                compraManager.agregarProducto(productoCreado, state.cantidadPorEmpaque.toDouble(), precioCents)
            }

            _uiState.update { it.copy(isSaving = false) }
            _events.send(UiEvent.SaveSuccess)
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        if (!ValidationUtils.isValidName(state.nombre)) {
            errors["nombre"] = "El nombre debe tener al menos 2 caracteres"
        }
        if (!ValidationUtils.isPositiveNumber(state.cantidadPorEmpaque)) {
            errors["cantidadPorEmpaque"] = "Debe ser mayor a 0"
        }
        val cents = CurrencyFormatter.toCents(state.precio)
        if (cents == null || cents <= 0) {
            errors["precio"] = "Ingresa un precio valido"
        }

        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
