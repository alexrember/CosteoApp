package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.PrecioHistoricoRaw
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
import com.mg.costeoapp.feature.inventario.data.mapper.SmartDefaults
import com.mg.costeoapp.feature.inventario.data.mapper.parseContenidoFromName
import com.mg.costeoapp.feature.inventario.data.repository.NutritionRepository
import com.mg.costeoapp.feature.inventario.data.repository.StoreSearchOrchestrator
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import com.mg.costeoapp.feature.sync.data.ProductContributionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    val isSaving: Boolean = false,
    val productosFrequentes: List<Producto> = emptyList(),
    val sugerencias: List<Producto> = emptyList(),
    val historialPrecios: List<PrecioHistoricoRaw> = emptyList(),
    val duplicateProducto: Producto? = null
)

@HiltViewModel
class ProductoRegistroViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val inventarioRepository: InventarioRepository,
    private val searchOrchestrator: StoreSearchOrchestrator,
    private val nutritionRepository: NutritionRepository,
    private val compraManager: CompraManager,
    private val productContributionService: ProductContributionService,
    private val syncManager: com.mg.costeoapp.feature.sync.data.SyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoRegistroUiState())
    val uiState: StateFlow<ProductoRegistroUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var nutricionExterna: NutricionExterna? = null
    private var cachedGlobalProductId: String? = null
    private var searchJob: Job? = null

    init {
        loadFrequentProducts()

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
            cachedGlobalProductId = compraManager.lastGlobalProductId
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
            val deferredStores = async {
                withTimeoutOrNull(8000L) {
                    searchOrchestrator.searchByBarcode(barcode)
                }
            }

            val deferredNutricion = async {
                withTimeoutOrNull(8000L) {
                    nutritionRepository.searchByBarcode(barcode)
                }
            }

            val storeResults = deferredStores.await()?.results ?: emptyList()
            val nutricion = deferredNutricion.await()
            if (cachedGlobalProductId == null) {
                cachedGlobalProductId = storeResults.firstNotNullOfOrNull { it.globalProductId }
            }
            aplicarResultados(storeResults, nutricion)
        }
    }

    private fun aplicarResultados(
        storeResults: List<com.mg.costeoapp.core.domain.model.StoreSearchResult>,
        nutricion: NutricionExterna?
    ) {
        nutricionExterna = nutricion
        val dataSources = mutableListOf<ProductDataSource>()

        val best = storeResults.firstOrNull { it.isAvailable }
        if (best != null) {
            val contenido = parseContenidoFromName(best.productName)
            dataSources.add(
                ProductDataSource(
                    sourceName = best.storeName,
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
        _uiState.update {
            val shouldSuggestUnit = it.mergedData == null || it.mergedData.unidadMedida is FieldResolution.Empty
            it.copy(
                nombre = value,
                fieldErrors = it.fieldErrors - "nombre",
                unidadMedida = if (shouldSuggestUnit) SmartDefaults.suggestUnit(value) else it.unidadMedida
            )
        }
    }

    fun onNombreChanged(value: String) {
        _uiState.update {
            val shouldSuggestUnit = it.mergedData == null || it.mergedData.unidadMedida is FieldResolution.Empty
            it.copy(
                nombre = value,
                fieldErrors = it.fieldErrors - "nombre",
                unidadMedida = if (shouldSuggestUnit && value.length >= 3) SmartDefaults.suggestUnit(value) else it.unidadMedida,
                historialPrecios = emptyList()
            )
        }
        debounceSuggestions(value)
    }

    private fun debounceSuggestions(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(sugerencias = emptyList(), historialPrecios = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val results = productoRepository.searchSuggestions(query)
            _uiState.update { it.copy(sugerencias = results) }
        }
    }

    fun onSugerenciaSelected(producto: Producto) {
        val unidad = UnidadMedida.fromCodigo(producto.unidadMedida) ?: UnidadMedida.UNIDAD
        _uiState.update {
            it.copy(
                nombre = producto.nombre,
                unidadMedida = unidad,
                cantidadPorEmpaque = formatCantidad(producto.cantidadPorEmpaque),
                unidadesPorEmpaque = producto.unidadesPorEmpaque.toString(),
                codigoBarras = producto.codigoBarras ?: it.codigoBarras,
                sugerencias = emptyList()
            )
        }
        loadPriceHistory(producto.id)
    }

    fun onSelectFrequentProduct(producto: Producto) {
        val unidad = UnidadMedida.fromCodigo(producto.unidadMedida) ?: UnidadMedida.UNIDAD
        _uiState.update {
            it.copy(
                nombre = producto.nombre,
                unidadMedida = unidad,
                cantidadPorEmpaque = formatCantidad(producto.cantidadPorEmpaque),
                unidadesPorEmpaque = producto.unidadesPorEmpaque.toString(),
                codigoBarras = producto.codigoBarras ?: it.codigoBarras,
                sugerencias = emptyList()
            )
        }
        loadPriceHistory(producto.id)
    }

    private fun loadPriceHistory(productoId: Long) {
        viewModelScope.launch {
            val history = productoRepository.getRecentPriceHistory(productoId)
            _uiState.update { it.copy(historialPrecios = history) }
        }
    }

    fun dismissSugerencias() {
        _uiState.update { it.copy(sugerencias = emptyList()) }
    }

    private fun loadFrequentProducts() {
        viewModelScope.launch {
            val frequent = productoRepository.getFrequentProducts()
            _uiState.update { it.copy(productosFrequentes = frequent) }
        }
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
            val codigoBarras = state.codigoBarras.ifBlank { null }

            if (codigoBarras != null) {
                val existente = productoRepository.getByCodigoBarras(codigoBarras)
                if (existente != null) {
                    _uiState.update { it.copy(isSaving = false, duplicateProducto = existente) }
                    return@launch
                }
            }

            createNewProduct()
        }
    }

    fun onDuplicateDismiss() {
        _uiState.update { it.copy(duplicateProducto = null) }
    }

    fun onDuplicateCreateNew() {
        _uiState.update { it.copy(duplicateProducto = null, isSaving = true) }
        viewModelScope.launch { createNewProduct() }
    }

    fun onDuplicateUpdatePrice() {
        val existente = _uiState.value.duplicateProducto ?: return
        _uiState.update { it.copy(duplicateProducto = null, isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val precioCents = CurrencyFormatter.toCents(state.precio)
            if (precioCents == null || precioCents <= 0) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(UiEvent.ShowError("Precio invalido"))
                return@launch
            }
            val tienda = compraManager.getTienda()

            if (tienda != null) {
                productoRepository.desactivarPrecios(existente.id, tienda.id)
                productoRepository.insertPrecio(
                    ProductoTienda(
                        productoId = existente.id,
                        tiendaId = tienda.id,
                        precio = precioCents
                    )
                )
                compraManager.agregarProducto(existente, 1.0, precioCents)
            }

            _uiState.update { it.copy(isSaving = false) }
            _events.send(UiEvent.SaveSuccess)
        }
    }

    private suspend fun createNewProduct() {
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
            return
        }

        val productoId = productoResult.getOrThrow()
        val productoCreado = producto.copy(id = productoId)

        if (!productoCreado.codigoBarras.isNullOrBlank()) {
            val gpId = cachedGlobalProductId
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
                try {
                    val token = productContributionService.getAccessToken()
                    productContributionService.contribute(productoCreado, gpId, token)
                } catch (_: Exception) { }
            }
        }

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
        syncManager.pushInBackground()
        _events.send(UiEvent.SaveSuccess)
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
