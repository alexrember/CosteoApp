package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.feature.inventario.data.dto.VtexProduct
import kotlin.math.roundToLong

fun VtexProduct.toStoreSearchResults(): List<StoreSearchResult> {
    val results = mutableListOf<StoreSearchResult>()

    items?.forEach { item ->
        item.sellers?.forEach { seller ->
            val offer = seller.commertialOffer
            if (offer != null) {
                results.add(
                    StoreSearchResult(
                        storeName = seller.sellerName ?: "Walmart SV",
                        productName = productName ?: item.nameComplete ?: "Sin nombre",
                        brand = brand,
                        ean = item.ean,
                        price = offer.price?.let { (it * 100).roundToLong() },
                        listPrice = offer.listPrice?.let { (it * 100).roundToLong() },
                        isAvailable = offer.isAvailable ?: false,
                        imageUrl = item.images?.firstOrNull()?.imageUrl,
                        measurementUnit = item.measurementUnit,
                        unitMultiplier = item.unitMultiplier,
                        source = "walmart_vtex"
                    )
                )
            }
        }
    }

    return results
}
