package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.feature.inventario.data.mapper.toStoreSearchResults
import com.mg.costeoapp.feature.inventario.data.remote.WalmartVtexApi
import javax.inject.Inject

class WalmartStoreRepository @Inject constructor(
    private val walmartApi: WalmartVtexApi
) {

    suspend fun searchByBarcode(ean: String): Result<List<StoreSearchResult>> {
        return try {
            val response = walmartApi.searchByBarcode(fq = "alternateIds_Ean:$ean")
            if (response.isSuccessful) {
                val results = response.body()?.flatMap { it.toStoreSearchResults() } ?: emptyList()
                Result.success(results)
            } else {
                Result.success(emptyList())
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Sin conexion a internet"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Tiempo de espera agotado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByName(query: String): Result<List<StoreSearchResult>> {
        return try {
            val response = walmartApi.searchByName(ft = query)
            if (response.isSuccessful) {
                val results = response.body()?.flatMap { it.toStoreSearchResults() } ?: emptyList()
                Result.success(results)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
