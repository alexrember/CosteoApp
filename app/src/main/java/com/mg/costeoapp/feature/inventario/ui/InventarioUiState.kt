package com.mg.costeoapp.feature.inventario.ui

import com.mg.costeoapp.core.database.dao.InventarioConDetalles
import com.mg.costeoapp.core.database.dao.ResumenInventarioTienda
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.core.domain.model.StoreSearchResult

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
    val carritoCount: Int = 0,
    val tiendaNombre: String = "",
    val error: String? = null
)

sealed class BarcodeLookupState {
    data object Idle : BarcodeLookupState()
    data object Buscando : BarcodeLookupState()
    data class EncontradoLocal(
        val producto: Producto,
        val preciosComparados: List<PrecioComparado> = emptyList()
    ) : BarcodeLookupState()
    data class EncontradoApi(val barcode: String, val resultados: List<StoreSearchResult>) : BarcodeLookupState()
    data class NoEncontrado(val barcode: String) : BarcodeLookupState()
    data class NeedItemNumber(val barcode: String, val lastScannedCode: String? = null) : BarcodeLookupState()
}

data class CarritoItem(
    val producto: Producto,
    val cantidad: Double,
    val precioUnitario: Long
) {
    val subtotal: Long get() = Math.round(precioUnitario * cantidad)
}

data class CarritoUiState(
    val tienda: Tienda? = null,
    val items: List<CarritoItem> = emptyList(),
    val isConfirming: Boolean = false
) {
    val total: Long get() = items.sumOf { it.subtotal }
    val isEmpty: Boolean get() = items.isEmpty()
    val itemCount: Int get() = items.size
}

data class SeleccionTiendaUiState(
    val tiendas: List<Tienda> = emptyList(),
    val isLoading: Boolean = true
)
