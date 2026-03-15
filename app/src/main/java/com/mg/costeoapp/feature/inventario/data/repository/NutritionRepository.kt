package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.feature.inventario.data.mapper.NutricionExterna
import com.mg.costeoapp.feature.inventario.data.mapper.toNutricionExterna
import com.mg.costeoapp.feature.inventario.data.remote.OpenFoodFactsApi
import javax.inject.Inject

class NutritionRepository @Inject constructor(
    private val openFoodFactsApi: OpenFoodFactsApi
) {

    suspend fun searchByBarcode(barcode: String): NutricionExterna? {
        return try {
            val response = openFoodFactsApi.getProductByBarcode(barcode)
            if (response.isSuccessful && response.body()?.status == 1) {
                response.body()?.product?.toNutricionExterna()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
