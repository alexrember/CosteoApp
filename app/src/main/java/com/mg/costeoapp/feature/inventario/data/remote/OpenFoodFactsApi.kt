package com.mg.costeoapp.feature.inventario.data.remote

import com.mg.costeoapp.feature.inventario.data.dto.OpenFoodFactsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>
}
