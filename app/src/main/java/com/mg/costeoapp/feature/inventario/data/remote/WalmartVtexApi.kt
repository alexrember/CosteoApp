package com.mg.costeoapp.feature.inventario.data.remote

import com.mg.costeoapp.feature.inventario.data.dto.VtexProduct
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WalmartVtexApi {

    companion object {
        const val BASE_URL = "https://www.walmart.com.sv/"
    }

    @GET("api/catalog_system/pub/products/search")
    suspend fun searchByBarcode(
        @Query("fq") fq: String
    ): Response<List<VtexProduct>>

    @GET("api/catalog_system/pub/products/search")
    suspend fun searchByName(
        @Query("ft") ft: String
    ): Response<List<VtexProduct>>
}
