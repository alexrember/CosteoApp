package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.core.util.UnidadMedida

data class ContenidoProducto(
    val cantidad: Double,
    val unidad: UnidadMedida
)

/**
 * Extrae la cantidad y unidad de contenido del nombre del producto.
 * Ejemplos:
 *   "Leche Entera 946ml" → 946ml
 *   "Arroz 5lb" → 5lb
 *   "Aceite 1L" → 1L
 *   "Azucar 2.5kg" → 2.5kg
 *   "Harina 500g" → 500g
 *   "Queso 12oz" → 12oz
 */
fun parseContenidoFromName(nombre: String): ContenidoProducto? {
    val patterns = listOf(
        Regex("""(\d+(?:[.,]\d+)?)\s*ml\b""", RegexOption.IGNORE_CASE) to UnidadMedida.MILILITRO,
        Regex("""(\d+(?:[.,]\d+)?)\s*l\b""", RegexOption.IGNORE_CASE) to UnidadMedida.LITRO,
        Regex("""(\d+(?:[.,]\d+)?)\s*kg\b""", RegexOption.IGNORE_CASE) to UnidadMedida.KILOGRAMO,
        Regex("""(\d+(?:[.,]\d+)?)\s*g\b""", RegexOption.IGNORE_CASE) to UnidadMedida.GRAMO,
        Regex("""(\d+(?:[.,]\d+)?)\s*lb\b""", RegexOption.IGNORE_CASE) to UnidadMedida.LIBRA,
        Regex("""(\d+(?:[.,]\d+)?)\s*oz\b""", RegexOption.IGNORE_CASE) to UnidadMedida.ONZA,
    )

    for ((regex, unidad) in patterns) {
        val match = regex.find(nombre)
        if (match != null) {
            val cantidad = match.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (cantidad != null && cantidad > 0) {
                return ContenidoProducto(cantidad, unidad)
            }
        }
    }

    return null
}
