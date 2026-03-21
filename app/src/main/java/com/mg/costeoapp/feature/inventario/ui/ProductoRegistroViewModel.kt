package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.domain.model.FieldResolution
import com.mg.costeoapp.core.domain.model.MergedProductData
import com.mg.costeoapp.core.domain.model.ProductDataMerger
import com.mg.costeoapp.core.domain.model.ProductDataSource
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.ErrorMapper
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
    val mergedData: MergedProductData? = null,
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
            // Usar cache del scanner si disponible (evita doble llamada API)
            val cachedResults = compraManager.lastSearchResults
            val cachedNutricion = compraManager.lastNutricion
            if (cachedResults != null) {
                aplicarResultados(cachedResults, cachedNutricion)
                compraManager.clearSearchCache()
            } else {
                buscarEnApis(codigoBarras)
            }
        }
    }

    private fun buscarEnApis(barcode: String) {
        _uiState.update { it.copy(buscandoEnApi = true) }

        viewModelScope.launch {
            val deferredWalmart = async {
                withTimeoutOrNull(8000L) {
                    walmartRepository.searchByBarcode(barcode).getOrNull()
                } ?: emptyList()
            }

            val deferredNutricion = async {
                withTimeoutOrNull(8000L) {
                    nutritionRepository.searchByBarcode(barcode)
                }
            }

            val walmartResults = deferredWalmart.await()
            val nutricion = deferredNutricion.await()
            aplicarResultados(walmartResults, nutricion)
        }
    }

    private fun aplicarResultados(
        walmartResults: List<com.mg.costeoapp.core.domain.model.StoreSearchResult>,
        nutricion: NutricionExterna?
    ) {
        nutricionExterna = nutricion
        val dataSources = mutableListOf<ProductDataSource>()

        val best = walmartResults.firstOrNull { it.isAvailable }
        if (best != null) {
            val contenido = parseContenidoFromName(best.productName)
            dataSources.add(
                ProductDataSource(
                    sourceName = "Walmart SV",
                    nombre = best.productName,
                    precio = best.price,
                    unidadMedida = contenido?.unidad ?: mapVtexUnit(best.measurementUnit),
                    cantidadPorEmpaque = contenido?.cantidad ?: best.unitMultiplier ?: 1.0
                )
            )
        }

        if (nutricionExterna != null) {
            val offContenido = nutricionExterna!!.cantidad?.let { parseContenidoFromName(it) }
            val hasData = !nutricionExterna!!.nombreProducto.isNullOrBlank() || offContenido != null
            if (hasData) {
                dataSources.add(
                    ProductDataSource(
                        sourceName = "Open Food Facts",
                        nombre = nutricionExterna!!.nombreProducto,
                        unidadMedida = offContenido?.unidad,
                        cantidadPorEmpaque = offContenido?.cantidad
                    )
                )
            }
        }

        val merged = ProductDataMerger.merge(dataSources)

        _uiState.update {
            it.copy(
                buscandoEnApi = false,
                mergedData = merged,
                nombre = autoFill(merged.nombre, it.nombre),
                precio = when (val p = merged.precio) {
                    is FieldResolution.Resolved -> CurrencyFormatter.fromCents(p.value).replace("$", "")
                    else -> it.precio
                },
                unidadMedida = when (val u = merged.unidadMedida) {
                    is FieldResolution.Resolved -> u.value
                    is FieldResolution.Conflict -> u.options[0].value
                    else -> it.unidadMedida
                },
                cantidadPorEmpaque = when (val c = merged.cantidadPorEmpaque) {
                    is FieldResolution.Resolved -> formatCantidad(c.value)
                    is FieldResolution.Conflict -> formatCantidad(c.options[0].value)
                    else -> it.cantidadPorEmpaque
                }
            )
        }
    }

    private fun <T> autoFill(resolution: FieldResolution<T>, current: String): String {
        return when (resolution) {
            is FieldResolution.Resolved -> resolution.value.toString()
            is FieldResolution.Conflict -> resolution.options[0].value.toString()
            is FieldResolution.Empty -> current
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

    fun onOcrResult(result: com.mg.costeoapp.feature.inventario.data.ocr.NutricionOcrResult) {
        nutricionExterna = result.toNutricionExterna()
        viewModelScope.launch {
            _events.send(UiEvent.ShowError(
                "Nutricion escaneada (${(result.confidence * 100).toInt()}% confianza)"
            ))
        }
    }

    fun onNombreSelected(value: String) {
        _uiState.update { it.copy(nombre = value, fieldErrors = it.fieldErrors - "nombre") }
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
                _events.send(UiEvent.ShowError(productoResult.exceptionOrNull()?.let { ErrorMapper.toUserMessage(it) } ?: "Error al crear producto"))
                return@launch
            }

            val productoId = productoResult.getOrThrow()
            val productoCreado = producto.copy(id = productoId)

            // Guardar precio en producto_tienda para futuras consultas
            if (tienda != null) {
                productoRepository.insertPrecio(
                    ProductoTienda(
                        productoId = productoId,
                        tiendaId = tienda.id,
                        precio = precioCents
                    )
                )
                compraManager.agregarProducto(productoCreado, 1.0, precioCents)
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
