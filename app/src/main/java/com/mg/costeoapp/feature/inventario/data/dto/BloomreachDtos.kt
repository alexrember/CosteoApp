package com.mg.costeoapp.feature.inventario.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BloomreachSearchResponse(
    val response: BloomreachDocs? = null
)

@Serializable
data class BloomreachDocs(
    val numFound: Int? = null,
    val docs: List<BloomreachProduct>? = null
)

@Serializable
data class BloomreachProduct(
    val title: String? = null,
    val brand: String? = null,
    val pid: String? = null,
    @SerialName("price_SV") val priceSV: Double? = null,
    @SerialName("thumb_image") val thumbImage: String? = null,
    @SerialName("availability_SV") val availabilitySV: String? = null
)
