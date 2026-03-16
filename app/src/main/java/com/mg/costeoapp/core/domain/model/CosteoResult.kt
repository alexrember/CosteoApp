package com.mg.costeoapp.core.domain.model

enum class FuentePrecio { INVENTARIO, PRECIO_RECIENTE, SIN_PRECIO }

data class PrecioResuelto(
    val productoId: Long,
    val productoNombre: String,
    val precioUnitario: Long?,
    val fuente: FuentePrecio,
    val tiendaNombre: String? = null
)

data class CosteoResult(
    val costoTotal: Long = 0,
    val costoPorPorcion: Long? = null,
    val fuentesPrecio: Map<Long, FuentePrecio> = emptyMap(),
    val advertencias: List<Advertencia> = emptyList()
)

sealed class Advertencia {
    data class SinPrecio(val nombreProducto: String) : Advertencia()
    data class SinStock(val nombreProducto: String) : Advertencia()
    data class IngredienteInactivo(val nombreProducto: String) : Advertencia()
    data class NutricionIncompleta(val nombreProducto: String) : Advertencia()
}

data class NutricionResumen(
    val calorias: Double? = null,
    val proteinas: Double? = null,
    val carbohidratos: Double? = null,
    val grasas: Double? = null,
    val fibra: Double? = null,
    val sodioMg: Double? = null,
    val esCompleto: Boolean = false,
    val productosSinInfo: List<String> = emptyList()
)
