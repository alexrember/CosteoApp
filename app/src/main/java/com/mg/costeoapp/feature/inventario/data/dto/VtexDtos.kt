package com.mg.costeoapp.feature.inventario.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VtexProduct(
    @SerialName("productName") val productName: String? = null,
    @SerialName("brand") val brand: String? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("items") val items: List<VtexItem>? = null
)

@Serializable
data class VtexItem(
    @SerialName("itemId") val itemId: String? = null,
    @SerialName("nameComplete") val nameComplete: String? = null,
    @SerialName("ean") val ean: String? = null,
    @SerialName("measurementUnit") val measurementUnit: String? = null,
    @SerialName("unitMultiplier") val unitMultiplier: Double? = null,
    @SerialName("images") val images: List<VtexImage>? = null,
    @SerialName("sellers") val sellers: List<VtexSeller>? = null
)

@Serializable
data class VtexImage(
    @SerialName("imageUrl") val imageUrl: String? = null
)

@Serializable
data class VtexSeller(
    @SerialName("sellerId") val sellerId: String? = null,
    @SerialName("sellerName") val sellerName: String? = null,
    @SerialName("commertialOffer") val commertialOffer: VtexCommertialOffer? = null
)

@Serializable
data class VtexCommertialOffer(
    @SerialName("Price") val price: Double? = null,
    @SerialName("ListPrice") val listPrice: Double? = null,
    @SerialName("IsAvailable") val isAvailable: Boolean? = null
)
