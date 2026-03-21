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

    val caloriasRaw = nutriments?.energyKcal100g?.let { it * factor }
    val proteinasRaw = nutriments?.proteins100g?.let { it * factor }
    val carbohidratosRaw = nutriments?.carbohydrates100g?.let { it * factor }
    val grasasRaw = nutriments?.fat100g?.let { it * factor }
    val fibraRaw = nutriments?.fiber100g?.let { it * factor }
    val sodioRaw = nutriments?.sodium100g?.let { it * factor * 1000 }

    return NutricionExterna(
        nombreProducto = productName,
        cantidad = quantity,
        porcionG = porcionG,
        calorias = caloriasRaw?.takeIf { it in 0.0..10000.0 },
        proteinas = proteinasRaw?.takeIf { it in 0.0..1000.0 },
        carbohidratos = carbohidratosRaw?.takeIf { it in 0.0..1000.0 },
        grasas = grasasRaw?.takeIf { it in 0.0..1000.0 },
        fibra = fibraRaw?.takeIf { it in 0.0..500.0 },
        sodioMg = sodioRaw?.takeIf { it in 0.0..50000.0 },
        fuente = "open_food_facts"
    )
}

fun parseServingSizeToGrams(servingSize: String): Double? {
    val gMlRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:g|ml)""", RegexOption.IGNORE_CASE)
    gMlRegex.find(servingSize)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }

    val ozRegex = Regex("""(\d+(?:\.\d+)?)\s*oz""", RegexOption.IGNORE_CASE)
    ozRegex.find(servingSize)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it * 28.3495 }

    val cupRegex = Regex("""(\d+(?:\.\d+)?)\s*cup""", RegexOption.IGNORE_CASE)
    cupRegex.find(servingSize)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it * 240.0 }

    val tbspRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:tbsp|tablespoon)""", RegexOption.IGNORE_CASE)
    tbspRegex.find(servingSize)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it * 15.0 }

    return null
}
