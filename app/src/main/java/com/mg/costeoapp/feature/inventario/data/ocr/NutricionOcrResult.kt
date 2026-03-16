package com.mg.costeoapp.feature.inventario.data.ocr

import com.mg.costeoapp.feature.inventario.data.mapper.NutricionExterna

data class NutricionOcrResult(
    val porcionG: Double?,
    val calorias: Double?,
    val proteinas: Double?,
    val carbohidratos: Double?,
    val grasas: Double?,
    val fibra: Double?,
    val sodioMg: Double?,
    val rawText: String,
    val confidence: Float
) {
    fun toNutricionExterna(): NutricionExterna = NutricionExterna(
        nombreProducto = null,
        cantidad = null,
        porcionG = porcionG,
        calorias = calorias,
        proteinas = proteinas,
        carbohidratos = carbohidratos,
        grasas = grasas,
        fibra = fibra,
        sodioMg = sodioMg,
        fuente = "ocr"
    )
}
