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

    // Estabilizacion: requiere N detecciones consecutivas del mismo codigo
    private var candidateBarcode: String? = null
    private var candidateCount = 0
    private val requiredDetections = 3

    // Cooldown: despues de procesar, ignorar por un tiempo
    private var processingBarcode: String? = null

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
        if (_uiState.value.isProcessing) return

        // Estabilizacion: contar detecciones consecutivas del mismo codigo
        if (barcode == candidateBarcode) {
            candidateCount++
        } else {
            candidateBarcode = barcode
            candidateCount = 1
        }

        // No procesar hasta tener suficientes detecciones consecutivas
        if (candidateCount < requiredDetections) return

        // Cooldown: si acabamos de procesar este codigo, ignorar
        if (barcode == processingBarcode) return

        // Reset candidate para la siguiente deteccion
        candidateCount = 0
        processBarcode(barcode)
    }

    private fun processBarcode(barcode: String) {
        processingBarcode = barcode

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scannedBarcode = barcode,
                    isProcessing = true,
                    lookupState = BarcodeLookupState.Buscando
                )
            }

            // 1. Buscar en DB local
            val productoLocal = productoDao.getByCodigoBarras(barcode)

            if (productoLocal != null) {
                val tienda = compraManager.getTienda()
                var precio = 0L
                if (tienda != null) {
                    val ultimoPrecio = productoTiendaDao.getPrecioActivo(productoLocal.id, tienda.id)
                    precio = ultimoPrecio?.precio ?: 0L
                }

                compraManager.agregarProducto(productoLocal, 1.0, precio)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoLocal(productoLocal)
                    )
                }
                _events.send(UiEvent.ShowError("${productoLocal.nombre} agregado al carrito"))

                // Cooldown: permitir re-escanear despues de 2 segundos
                kotlinx.coroutines.delay(2000)
                processingBarcode = null
                _uiState.update { it.copy(lookupState = BarcodeLookupState.Idle) }
                return@launch
            }

            // 2. No encontrado → llamadas PARALELAS a Walmart + Open Food Facts
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
        }
    }

    fun resetScanner() {
        processingBarcode = null
        candidateBarcode = null
        candidateCount = 0
        _uiState.update {
            it.copy(
                scannedBarcode = null,
                isProcessing = false,
                lookupState = BarcodeLookupState.Idle,
                error = null
            )
        }
    }

    fun readyForNewScan() {
        lastNutricion = null
        resetScanner()
    }
}
