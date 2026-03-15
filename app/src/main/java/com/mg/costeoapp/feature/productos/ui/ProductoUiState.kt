package com.mg.costeoapp.feature.productos.ui

import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.core.database.relation.PrecioConTienda
import com.mg.costeoapp.core.util.UnidadMedida

data class ProductoListUiState(
    val productos: List<Producto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ProductoFormUiState(
    val producto: Producto? = null,
    val nombre: String = "",
    val codigoBarras: String = "",
    val unidadMedida: UnidadMedida = UnidadMedida.UNIDAD,
    val cantidadPorEmpaque: String = "",
    val esServicio: Boolean = false,
    val notas: String = "",
    val factorMerma: String = "0",
    val nutricionPorcionG: String = "",
    val nutricionCalorias: String = "",
    val nutricionProteinasG: String = "",
    val nutricionCarbohidratosG: String = "",
    val nutricionGrasasG: String = "",
    val nutricionFibraG: String = "",
    val nutricionSodioMg: String = "",
    val nutricionFuente: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    val isEditMode: Boolean get() = producto != null
}

data class ProductoDetailUiState(
    val producto: Producto? = null,
    val preciosConTienda: List<PrecioConTienda> = emptyList(),
    val precioMasReciente: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ProductoPrecioFormUiState(
    val productoId: Long = 0,
    val productoNombre: String = "",
    val tiendasDisponibles: List<Tienda> = emptyList(),
    val tiendaSeleccionadaId: Long? = null,
    val precio: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
