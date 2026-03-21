package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.inventario.data.CompraManager
import com.mg.costeoapp.feature.inventario.data.mapper.NutricionExterna
import com.mg.costeoapp.feature.inventario.data.mapper.SmartDefaults
import com.mg.costeoapp.feature.inventario.data.repository.NutritionRepository
import com.mg.costeoapp.feature.inventario.data.repository.StoreSearchOrchestrator
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

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val productoDao: ProductoDao,
    private val productoTiendaDao: ProductoTiendaDao,
    private val searchOrchestrator: StoreSearchOrchestrator,
    private val nutritionRepository: NutritionRepository,
    val compraManager: CompraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface ScannerNavEvent {
        data class GoToRegistro(val barcode: String?) : ScannerNavEvent
    }
    private val _navEvents = Channel<ScannerNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    var lastNutricion: NutricionExterna? = null
        private set

    private var candidateBarcode: String? = null
    private var candidateCount = 0
    private val requiredDetections = 3
    private var processingBarcode: String? = null
    private val scanLock = Any()

    init {
        _uiState.update {
            it.copy(tiendaNombre = compraManager.getTienda()?.nombre ?: "")
        }
        viewModelScope.launch {
            compraManager.items.collect { items ->
                _uiState.update { it.copy(carritoCount = items.size) }
            }
        }
    }

    fun onBarcodeDetected(barcode: String) {
        synchronized(scanLock) {
            if (_uiState.value.isProcessing) return
            if (barcode == candidateBarcode) {
                candidateCount++
            } else {
                candidateBarcode = barcode
                candidateCount = 1
            }
            if (candidateCount < requiredDetections) return
            if (barcode == processingBarcode) return
            candidateCount = 0
            processingBarcode = barcode
        }
        processBarcode(barcode)
    }

    private fun isValidBarcode(barcode: String): Boolean =
        barcode.matches(Regex("^\\d{8,14}$"))

    private fun processBarcode(barcode: String) {
        if (!isValidBarcode(barcode)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scannedBarcode = barcode,
                    isProcessing = true,
                    lookupState = BarcodeLookupState.Buscando
                )
            }

            val productoLocal = productoDao.getByCodigoBarras(barcode)

            if (productoLocal != null) {
                val tienda = compraManager.getTienda()
                var precio = 0L
                if (tienda != null) {
                    val precioTienda = productoTiendaDao.getPrecioActivo(productoLocal.id, tienda.id)
                    val precioReciente = precioTienda ?: productoTiendaDao.getPrecioMasReciente(productoLocal.id)
                    precio = precioReciente?.precio ?: 0L
                }

                compraManager.agregarProducto(productoLocal, 1.0, precio)

                // Buscar precios de comparacion en paralelo
                val preciosComparados = mutableListOf<PrecioComparado>()

                // Precio local registrado
                if (precio > 0 && tienda != null) {
                    preciosComparados.add(PrecioComparado(
                        tiendaNombre = tienda.nombre,
                        precio = precio,
                        fecha = null,
                        fuente = "local"
                    ))
                }

                // Buscar precios en todas las tiendas en paralelo
                if (productoLocal.codigoBarras != null) {
                    val orchestrated = searchOrchestrator.searchByBarcode(productoLocal.codigoBarras)
                    orchestrated.results.filter { it.isAvailable && it.price != null }.forEach { result ->
                        preciosComparados.add(PrecioComparado(
                            tiendaNombre = result.storeName,
                            precio = result.price!!,
                            fecha = null,
                            fuente = result.source
                        ))
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoLocal(
                            productoLocal, preciosComparados
                        )
                    )
                }
                _events.send(UiEvent.ShowError("${productoLocal.nombre} agregado al carrito"))

                kotlinx.coroutines.delay(2000)
                synchronized(scanLock) { processingBarcode = null }
                _uiState.update { it.copy(lookupState = BarcodeLookupState.Idle) }
                return@launch
            }

            val deferredStores = async {
                searchOrchestrator.searchByBarcode(barcode)
            }

            val deferredNutricion = async {
                nutritionRepository.searchByBarcode(barcode)
            }

            val orchestrated = deferredStores.await()
            var storeResults = orchestrated.results
            lastNutricion = deferredNutricion.await()

            // PriceSmart no indexa por barcode — siempre necesita nombre.
            // Buscar nombre de cualquier fuente disponible (autosuficiente):
            // 1. Walmart (si respondio), 2. Open Food Facts, 3. BD local (productos previos)
            val hasPriceSmartResult = storeResults.any { it.storeName == "PriceSmart" }
            if (!hasPriceSmartResult) {
                val productName = storeResults.firstOrNull()?.productName
                    ?: lastNutricion?.nombreProducto
                    ?: productoDao.getByCodigoBarras(barcode)?.nombre
                if (!productName.isNullOrBlank()) {
                    // Traducir ingles→español para mejorar match en tiendas SV
                    val searchName = SmartDefaults.translateForSearch(productName)
                    val nameResults = withTimeoutOrNull(5000L) {
                        searchOrchestrator.searchPriceSmartByName(searchName)
                    }
                    if (!nameResults.isNullOrEmpty()) {
                        storeResults = storeResults + nameResults
                    }
                }
            }

            compraManager.cacheSearchResults(storeResults, lastNutricion)

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    lookupState = if (storeResults.isNotEmpty())
                        BarcodeLookupState.EncontradoApi(barcode, storeResults)
                    else
                        BarcodeLookupState.NoEncontrado(barcode)
                )
            }
            // Navegar al registro via channel one-shot
            _navEvents.send(ScannerNavEvent.GoToRegistro(barcode))
        }
    }

    /** Llamar al volver del registro. Permite re-escanear cualquier codigo. */
    fun onReturnFromRegistro() {
        synchronized(scanLock) {
            processingBarcode = null
        }
        _uiState.update {
            it.copy(lookupState = BarcodeLookupState.Idle, isProcessing = false)
        }
    }

    /** Reset completo para empezar de nuevo */
    fun resetScanner() {
        synchronized(scanLock) {
            processingBarcode = null
            candidateBarcode = null
            candidateCount = 0
        }
        lastNutricion = null
        _uiState.update {
            it.copy(
                scannedBarcode = null,
                isProcessing = false,
                lookupState = BarcodeLookupState.Idle,
                error = null
            )
        }
    }
}
