package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.feature.inventario.data.CompraManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
    val compraManager: CompraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastNavigatedBarcode: String? = null

    init {
        _uiState.update {
            it.copy(
                tiendaNombre = compraManager.getTienda()?.nombre ?: "",
                carritoCount = compraManager.itemCount
            )
        }
    }

    fun onBarcodeDetected(barcode: String) {
        if (_uiState.value.isProcessing) return
        if (barcode == lastNavigatedBarcode) return

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
                // Producto existe → agregar al carrito con ultimo precio conocido
                val tienda = compraManager.getTienda()
                var precio = 0L
                if (tienda != null) {
                    val ultimoPrecio = productoTiendaDao.getPrecioActivo(productoLocal.id, tienda.id)
                    precio = ultimoPrecio?.precio ?: 0L
                }

                compraManager.agregarProducto(productoLocal, 1.0, precio)

                lastNavigatedBarcode = barcode
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoLocal(productoLocal),
                        carritoCount = compraManager.itemCount
                    )
                }
                _events.send(UiEvent.ShowError("${productoLocal.nombre} agregado al carrito"))

                // Reset para seguir escaneando
                kotlinx.coroutines.delay(1500)
                lastNavigatedBarcode = null
                _uiState.update { it.copy(lookupState = BarcodeLookupState.Idle) }
            } else {
                lastNavigatedBarcode = barcode
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
        _uiState.update {
            it.copy(
                scannedBarcode = null,
                isProcessing = false,
                lookupState = BarcodeLookupState.Idle,
                error = null,
                carritoCount = compraManager.itemCount
            )
        }
    }

    fun readyForNewScan() {
        lastNavigatedBarcode = null
        resetScanner()
    }
}
