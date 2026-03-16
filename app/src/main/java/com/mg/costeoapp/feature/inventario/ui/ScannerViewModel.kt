package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.inventario.data.CompraManager
import com.mg.costeoapp.feature.inventario.data.mapper.NutricionExterna
import com.mg.costeoapp.feature.inventario.data.repository.NutritionRepository
import com.mg.costeoapp.feature.inventario.data.repository.WalmartStoreRepository
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
    private val walmartRepository: WalmartStoreRepository,
    private val nutritionRepository: NutritionRepository,
    val compraManager: CompraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    var lastNutricion: NutricionExterna? = null
        private set

    @Volatile private var candidateBarcode: String? = null
    @Volatile private var candidateCount = 0
    private val requiredDetections = 3
    @Volatile private var processingBarcode: String? = null
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
        }
        processBarcode(barcode)
    }

    private fun isValidBarcode(barcode: String): Boolean =
        barcode.matches(Regex("^\\d{8,14}$"))

    private fun processBarcode(barcode: String) {
        if (!isValidBarcode(barcode)) return
        processingBarcode = barcode

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
                    // Buscar precio para esta tienda, si no hay buscar el más reciente de cualquier tienda
                    val precioTienda = productoTiendaDao.getPrecioActivo(productoLocal.id, tienda.id)
                    val precioReciente = precioTienda ?: productoTiendaDao.getPrecioMasReciente(productoLocal.id)
                    precio = precioReciente?.precio ?: 0L
                }

                compraManager.agregarProducto(productoLocal, 1.0, precio)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoLocal(productoLocal)
                    )
                }
                _events.send(UiEvent.ShowError("${productoLocal.nombre} agregado al carrito"))

                kotlinx.coroutines.delay(2000)
                processingBarcode = null
                _uiState.update { it.copy(lookupState = BarcodeLookupState.Idle) }
                return@launch
            }

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
            lastNutricion = deferredNutricion.await()

            // Cachear para que ProductoRegistroViewModel no tenga que buscar de nuevo
            compraManager.cacheSearchResults(walmartResults, lastNutricion)

            if (walmartResults.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoApi(barcode, walmartResults)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.NoEncontrado(barcode)
                    )
                }
            }
            // NO resetear processingBarcode aqui — se resetea cuando el usuario vuelve
        }
    }

    /** Llamar al volver del registro. Permite escanear otros codigos pero no el mismo. */
    fun onReturnFromRegistro() {
        _uiState.update {
            it.copy(lookupState = BarcodeLookupState.Idle, isProcessing = false)
        }
        // processingBarcode se mantiene para no re-procesar el mismo codigo
    }

    /** Reset completo para empezar de nuevo */
    fun resetScanner() {
        processingBarcode = null
        candidateBarcode = null
        candidateCount = 0
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
