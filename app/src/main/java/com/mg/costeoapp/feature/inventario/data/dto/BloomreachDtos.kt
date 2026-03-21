package com.mg.costeoapp.feature.inventario.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BloomreachResponse(
    val response: BloomreachDocs? = null
)

@Serializable
data class BloomreachDocs(
    val docs: List<BloomreachProduct>? = null
)

@Serializable
data class BloomreachProduct(
    val title: String? = null,
    val brand: String? = null,
    val pid: String? = null,
    val price: Double? = null,
    val sale_price: Double? = null,
    val thumb_image: String? = null,
    val url: String? = null
)
