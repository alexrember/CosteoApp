package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.feature.inventario.data.dto.OffProduct

data class NutricionExterna(
    val nombreProducto: String?,
    val cantidad: String?,
    val porcionG: Double?,
    val calorias: Double?,
    val proteinas: Double?,
    val carbohidratos: Double?,
    val grasas: Double?,
    val fibra: Double?,
    val sodioMg: Double?,
    val fuente: String
)

fun OffProduct.toNutricionExterna(): NutricionExterna {
    val porcionG = servingSize?.let { parseServingSizeToGrams(it) } ?: 100.0
    val factor = porcionG / 100.0

    return NutricionExterna(
        nombreProducto = productName,
        cantidad = quantity,
        porcionG = porcionG,
        calorias = nutriments?.energyKcal100g?.let { it * factor },
        proteinas = nutriments?.proteins100g?.let { it * factor },
        carbohidratos = nutriments?.carbohydrates100g?.let { it * factor },
        grasas = nutriments?.fat100g?.let { it * factor },
        fibra = nutriments?.fiber100g?.let { it * factor },
        sodioMg = nutriments?.sodium100g?.let { it * factor * 1000 },
        fuente = "open_food_facts"
    )
}

fun parseServingSizeToGrams(servingSize: String): Double? {
    val regex = Regex("""(\d+(?:\.\d+)?)\s*(?:g|ml)""", RegexOption.IGNORE_CASE)
    return regex.find(servingSize)?.groupValues?.get(1)?.toDoubleOrNull()
}
