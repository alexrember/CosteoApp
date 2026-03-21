package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.feature.inventario.data.dto.BloomreachProduct
import kotlin.math.roundToLong

fun BloomreachProduct.toStoreSearchResult(): StoreSearchResult {
    val effectivePrice = sale_price ?: price
    val priceInCents = when {
        effectivePrice == null || effectivePrice == 0.0 -> null
        else -> (effectivePrice * 100).roundToLong()
    }
    val listPriceInCents = when {
        price == null || price == 0.0 -> null
        else -> (price * 100).roundToLong()
    }

    return StoreSearchResult(
        storeName = "PriceSmart",
        productName = title ?: "Sin nombre",
        brand = brand,
        ean = null,
        price = priceInCents,
        listPrice = listPriceInCents,
        isAvailable = true,
        imageUrl = thumb_image,
        measurementUnit = null,
        unitMultiplier = null,
        source = "pricesmart_bloomreach"
    )
}
