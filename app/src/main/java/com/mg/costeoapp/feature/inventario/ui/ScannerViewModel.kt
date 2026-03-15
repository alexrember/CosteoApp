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

    fun onBarcodeDetected(barcode: String) {
        if (barcode == _uiState.value.scannedBarcode && _uiState.value.isProcessing) return

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
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lookupState = BarcodeLookupState.EncontradoLocal(productoLocal)
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
        _uiState.update {
            it.copy(
                scannedBarcode = null,
                isProcessing = false,
                lookupState = BarcodeLookupState.Idle,
                error = null
            )
        }
    }

    fun toggleCamera(active: Boolean) {
        _uiState.update { it.copy(isCameraActive = active) }
    }
}
