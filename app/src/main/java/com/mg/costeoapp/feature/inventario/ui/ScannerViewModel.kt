package com.mg.costeoapp.feature.inventario.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.dao.ProductoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val productoDao: ProductoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastNavigatedBarcode: String? = null

    fun onBarcodeDetected(barcode: String) {
        // Ignorar si ya estamos procesando o si acabamos de navegar con este codigo
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
                lastNavigatedBarcode = barcode
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoLocal(productoLocal)
                    )
                }
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
                error = null
            )
        }
        // No limpiamos lastNavigatedBarcode para evitar re-navegacion al volver
    }

    fun readyForNewScan() {
        lastNavigatedBarcode = null
        resetScanner()
    }

    fun toggleCamera(active: Boolean) {
        _uiState.update { it.copy(isCameraActive = active) }
    }
}
