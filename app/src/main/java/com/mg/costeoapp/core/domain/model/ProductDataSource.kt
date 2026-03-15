package com.mg.costeoapp.core.domain.model

import com.mg.costeoapp.core.util.UnidadMedida

/**
 * Datos de un producto provenientes de una fuente externa.
 * Cada tienda/API que responda genera uno de estos.
 * Futuro: PriceSmart, Super Selectos API, etc.
 */
data class ProductDataSource(
    val sourceName: String,
    val nombre: String? = null,
    val precio: Long? = null,
    val unidadMedida: UnidadMedida? = null,
    val cantidadPorEmpaque: Double? = null,
    val unidadesPorEmpaque: Int? = null
)

/**
 * Resolucion de un campo: resuelto, en conflicto, o vacio.
 */
sealed class FieldResolution<out T> {
    data class Resolved<T>(val value: T, val source: String) : FieldResolution<T>()
    data class Conflict<T>(val options: List<FieldOption<T>>) : FieldResolution<T>()
    data object Empty : FieldResolution<Nothing>()
}

data class FieldOption<T>(
    val value: T,
    val source: String
)

/**
 * Resultado de combinar multiples fuentes de datos.
 * Cada campo es una FieldResolution que puede estar resuelto, en conflicto, o vacio.
 */
data class MergedProductData(
    val nombre: FieldResolution<String> = FieldResolution.Empty,
    val precio: FieldResolution<Long> = FieldResolution.Empty,
    val unidadMedida: FieldResolution<UnidadMedida> = FieldResolution.Empty,
    val cantidadPorEmpaque: FieldResolution<Double> = FieldResolution.Empty,
    val unidadesPorEmpaque: FieldResolution<Int> = FieldResolution.Empty,
    val sources: List<String> = emptyList()
)
