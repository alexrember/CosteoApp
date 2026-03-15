package com.mg.costeoapp.feature.inventario.ui

import com.mg.costeoapp.core.database.dao.InventarioConDetalles
import com.mg.costeoapp.core.database.dao.ResumenInventarioTienda
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda

data class InventarioListUiState(
    val items: List<InventarioConDetalles> = emptyList(),
    val resumenPorTienda: List<ResumenInventarioTienda> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ScannerUiState(
    val isCameraActive: Boolean = true,
    val isProcessing: Boolean = false,
    val scannedBarcode: String? = null,
    val lookupState: BarcodeLookupState = BarcodeLookupState.Idle,
    val error: String? = null
)

sealed class BarcodeLookupState {
    data object Idle : BarcodeLookupState()
    data object Buscando : BarcodeLookupState()
    data class EncontradoLocal(val producto: Producto) : BarcodeLookupState()
    data class NoEncontrado(val barcode: String) : BarcodeLookupState()
}

data class CarritoItem(
    val producto: Producto,
    val tienda: Tienda,
    val cantidad: Double,
    val precioUnitario: Long
) {
    val subtotal: Long get() = (precioUnitario * cantidad).toLong()
}

data class CarritoUiState(
    val tiendaSeleccionada: Tienda? = null,
    val tiendasDisponibles: List<Tienda> = emptyList(),
    val items: List<CarritoItem> = emptyList(),
    val isConfirming: Boolean = false
) {
    val total: Long get() = items.sumOf { it.subtotal }
    val isEmpty: Boolean get() = items.isEmpty()
}
