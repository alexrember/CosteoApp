package com.mg.costeoapp.core.domain.model

data class StoreSearchResult(
    val storeName: String,
    val productName: String,
    val brand: String?,
    val ean: String?,
    val price: Long?,
    val listPrice: Long?,
    val isAvailable: Boolean,
    val imageUrl: String?,
    val source: String
)
