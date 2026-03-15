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
import com.mg.costeoapp.feature.inventario.data.mapper.NutricionExterna
import com.mg.costeoapp.feature.inventario.data.mapper.parseContenidoFromName
import com.mg.costeoapp.feature.inventario.data.repository.NutritionRepository
import com.mg.costeoapp.feature.inventario.data.repository.WalmartStoreRepository
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    val unidadesPorEmpaque: String = "1",
    val precio: String = "",
    val tiendaNombre: String = "",
    val buscandoEnApi: Boolean = false,
    val fuenteApi: String? = null,
    val fuenteNutricion: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false
)

@HiltViewModel
class ProductoRegistroViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val inventarioRepository: InventarioRepository,
    private val walmartRepository: WalmartStoreRepository,
    private val nutritionRepository: NutritionRepository,
    private val compraManager: CompraManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoRegistroUiState())
    val uiState: StateFlow<ProductoRegistroUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nutricionExterna: NutricionExterna? = null

    init {
        val codigoBarras = savedStateHandle.get<String>("codigoBarras") ?: ""
        val tienda = compraManager.getTienda()

        _uiState.update {
            it.copy(
                codigoBarras = codigoBarras,
                tiendaNombre = tienda?.nombre ?: ""
            )
        }

        if (codigoBarras.isNotBlank()) {
            buscarEnApis(codigoBarras)
        }
    }

    private fun buscarEnApis(barcode: String) {
        _uiState.update { it.copy(buscandoEnApi = true) }

        viewModelScope.launch {
            // Llamadas PARALELAS: Walmart (precios) + Open Food Facts (nutricion + contenido)
            val deferredWalmart = async {
                withTimeoutOrNull(8000L) {
                    walmartRepository.searchByBarcode(barcode).getOrNull()
                }
            }

            val deferredNutricion = async {
                withTimeoutOrNull(8000L) {
                    nutritionRepository.searchByBarcode(barcode)
                }
            }

            val walmartResults = deferredWalmart.await()
            nutricionExterna = deferredNutricion.await()

            val best = walmartResults?.firstOrNull { it.isAvailable }
            val fuentes = mutableListOf<String>()

            // Pre-llenar desde Walmart (nombre, precio)
            if (best != null) {
                fuentes.add("Walmart SV")
                _uiState.update {
                    it.copy(
                        nombre = best.productName,
                        precio = best.price?.let { p -> CurrencyFormatter.fromCents(p).replace("$", "") } ?: ""
                    )
                }
            }

            // Pre-llenar contenido: Open Food Facts quantity > nombre parser > VTEX unit
            var contenidoLlenado = false

            // 1. Open Food Facts quantity (ej: "946 ml", "1 L")
            nutricionExterna?.cantidad?.let { qty ->
                val contenido = parseContenidoFromName(qty)
                if (contenido != null) {
                    fuentes.add("Open Food Facts")
                    _uiState.update {
                        it.copy(
                            unidadMedida = contenido.unidad,
                            cantidadPorEmpaque = formatCantidad(contenido.cantidad)
                        )
                    }
                    contenidoLlenado = true
                }
            }

            // 2. Parsear del nombre del producto
            if (!contenidoLlenado && best != null) {
                val contenido = parseContenidoFromName(best.productName)
                if (contenido != null) {
                    _uiState.update {
                        it.copy(
                            unidadMedida = contenido.unidad,
                            cantidadPorEmpaque = formatCantidad(contenido.cantidad)
                        )
                    }
                    contenidoLlenado = true
                }
            }

            // 3. VTEX measurementUnit como fallback
            if (!contenidoLlenado && best != null) {
                _uiState.update {
                    it.copy(
                        unidadMedida = mapVtexUnit(best.measurementUnit),
                        cantidadPorEmpaque = best.unitMultiplier?.let { formatCantidad(it) } ?: "1"
                    )
                }
            }

            _uiState.update {
                it.copy(
                    buscandoEnApi = false,
                    fuenteApi = if (fuentes.isNotEmpty()) fuentes.joinToString(" + ") else null,
                    fuenteNutricion = if (nutricionExterna != null) "Open Food Facts" else null
                )
            }
        }
    }

    private fun formatCantidad(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun mapVtexUnit(vtexUnit: String?): UnidadMedida {
        return when (vtexUnit?.lowercase()) {
            "kg" -> UnidadMedida.KILOGRAMO
            "g" -> UnidadMedida.GRAMO
            "l" -> UnidadMedida.LITRO
            "ml" -> UnidadMedida.MILILITRO
            "lb" -> UnidadMedida.LIBRA
            "oz" -> UnidadMedida.ONZA
            else -> UnidadMedida.UNIDAD
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

    fun onUnidadesPorEmpaqueChanged(value: String) {
        _uiState.update { it.copy(unidadesPorEmpaque = value, fieldErrors = it.fieldErrors - "unidadesPorEmpaque") }
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

            val producto = Producto(
                nombre = state.nombre.trim(),
                codigoBarras = state.codigoBarras.ifBlank { null },
                unidadMedida = state.unidadMedida.codigo,
                cantidadPorEmpaque = state.cantidadPorEmpaque.toDouble(),
                unidadesPorEmpaque = state.unidadesPorEmpaque.toIntOrNull() ?: 1,
                nutricionPorcionG = nutricionExterna?.porcionG,
                nutricionCalorias = nutricionExterna?.calorias,
                nutricionProteinasG = nutricionExterna?.proteinas,
                nutricionCarbohidratosG = nutricionExterna?.carbohidratos,
                nutricionGrasasG = nutricionExterna?.grasas,
                nutricionFibraG = nutricionExterna?.fibra,
                nutricionSodioMg = nutricionExterna?.sodioMg,
                nutricionFuente = nutricionExterna?.fuente
            )

            val productoResult = productoRepository.insert(producto)
            if (productoResult.isFailure) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(UiEvent.ShowError(productoResult.exceptionOrNull()?.message ?: "Error al crear producto"))
                return@launch
            }

            val productoId = productoResult.getOrThrow()
            val productoCreado = producto.copy(id = productoId)

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
