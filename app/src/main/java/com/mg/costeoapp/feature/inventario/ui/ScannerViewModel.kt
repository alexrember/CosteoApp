package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.inventario.data.CompraManager
import com.mg.costeoapp.feature.inventario.data.mapper.NutricionExterna
import com.mg.costeoapp.feature.inventario.data.repository.CosteoBackendRepository
import com.mg.costeoapp.feature.inventario.data.repository.NutritionRepository
import com.mg.costeoapp.feature.inventario.data.repository.StoreSearchOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val productoDao: ProductoDao,
    private val productoTiendaDao: ProductoTiendaDao,
    private val tiendaDao: com.mg.costeoapp.core.database.dao.TiendaDao,
    private val searchOrchestrator: StoreSearchOrchestrator,
    private val nutritionRepository: NutritionRepository,
    private val backendRepository: CosteoBackendRepository,
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
    private var pendingEanForItemNumber: String? = null

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

        val currentState = _uiState.value.lookupState
        if (currentState is BarcodeLookupState.NeedItemNumber) {
            // Ignore barcode scans in NeedItemNumber state — user must enter Item# manually
            return
        }
        processBarcode(barcode)
    }

    private suspend fun getActiveStoreNames(): List<String> {
        return tiendaDao.getAllOnce().filter { it.activo }.map { it.nombre }
    }

    private fun isPriceSmart(): Boolean {
        val tienda = compraManager.getTienda() ?: return false
        return tienda.nombre.contains("PriceSmart", ignoreCase = true)
    }

    private fun isValidBarcode(barcode: String): Boolean =
        barcode.matches(Regex("^\\d{8,14}$"))

    private fun isValidItemNumber(code: String): Boolean =
        code.matches(Regex("^\\d{4,14}$"))

    fun onManualItemNumber(itemNumber: String) {
        val ean = pendingEanForItemNumber ?: return
        processItemNumber(itemNumber, ean)
    }

    private fun processItemNumber(itemNumber: String, originalEan: String) {
        if (!isValidItemNumber(itemNumber)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scannedBarcode = itemNumber,
                    isProcessing = true,
                    lookupState = BarcodeLookupState.Buscando
                )
            }

            // Link EAN to PriceSmart Item# via backend
            val linkResult = backendRepository.linkBarcodeToItemNumber(originalEan, itemNumber)
            val storeResults = linkResult.getOrDefault(emptyList())
            lastNutricion = nutritionRepository.searchByBarcode(originalEan)

            compraManager.cacheSearchResults(storeResults, lastNutricion)
            pendingEanForItemNumber = null

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    scannedBarcode = originalEan,
                    lookupState = if (storeResults.isNotEmpty())
                        BarcodeLookupState.EncontradoApi(originalEan, storeResults)
                    else
                        BarcodeLookupState.NoEncontrado(originalEan)
                )
            }
            _navEvents.send(ScannerNavEvent.GoToRegistro(originalEan))
        }
    }

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

            val completed = withTimeoutOrNull(15_000L) {
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

                    val preciosComparados = mutableListOf<PrecioComparado>()

                    if (precio > 0 && tienda != null) {
                        preciosComparados.add(PrecioComparado(
                            tiendaNombre = tienda.nombre,
                            precio = precio,
                            fecha = null,
                            fuente = "local"
                        ))
                    }

                    if (productoLocal.codigoBarras != null) {
                        val tiendaNombreLocal = tienda?.nombre?.lowercase() ?: ""
                        val orchestrated = searchOrchestrator.searchByBarcode(productoLocal.codigoBarras, getActiveStoreNames())
                        orchestrated.results.filter { it.isAvailable && it.price != null }.forEach { result ->
                            val yaExiste = preciosComparados.any { existing ->
                                existing.tiendaNombre.lowercase().contains(result.storeName.lowercase().take(8)) ||
                                result.storeName.lowercase().contains(existing.tiendaNombre.lowercase().take(8))
                            }
                            if (!yaExiste) {
                                preciosComparados.add(PrecioComparado(
                                    tiendaNombre = result.storeName,
                                    precio = result.price!!,
                                    fecha = null,
                                    fuente = result.source
                                ))
                            }
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

                    kotlinx.coroutines.delay(1500)
                    synchronized(scanLock) { processingBarcode = null }
                    if (preciosComparados.size <= 1) {
                        _uiState.update { it.copy(lookupState = BarcodeLookupState.Idle) }
                    }
                    return@withTimeoutOrNull
                }

                val deferredStores = async {
                    searchOrchestrator.searchByBarcode(barcode, getActiveStoreNames())
                }

                val deferredNutricion = async {
                    nutritionRepository.searchByBarcode(barcode)
                }

                val orchestrated = deferredStores.await()
                val storeResults = orchestrated.results
                lastNutricion = deferredNutricion.await()

                // PriceSmart: si no hay precio de PriceSmart, pedir Item#
                val priceSmartResults = storeResults.filter { it.source == "pricesmart_bloomreach" && it.price != null && it.price > 0 }
                if (isPriceSmart() && priceSmartResults.isEmpty()) {
                    pendingEanForItemNumber = barcode
                    synchronized(scanLock) { processingBarcode = null }
                    candidateBarcode = null
                    candidateCount = 0
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lookupState = BarcodeLookupState.NeedItemNumber(barcode)
                        )
                    }
                    return@withTimeoutOrNull
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
                _navEvents.send(ScannerNavEvent.GoToRegistro(barcode))
            }

            if (completed == null) {
                synchronized(scanLock) { processingBarcode = null }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.Idle
                    )
                }
                _events.send(UiEvent.ShowError("Busqueda agotada. Intenta de nuevo."))
            }
        }
    }

    fun dismissPriceComparison() {
        _uiState.update { it.copy(lookupState = BarcodeLookupState.Idle) }
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
