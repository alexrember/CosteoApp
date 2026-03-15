package com.mg.costeoapp.feature.inventario.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsResponse(
    val status: Int = 0,
    val product: OffProduct? = null
)

@Serializable
data class OffProduct(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    val nutriments: OffNutriments? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
    @SerialName("sodium_100g") val sodium100g: Double? = null
)
