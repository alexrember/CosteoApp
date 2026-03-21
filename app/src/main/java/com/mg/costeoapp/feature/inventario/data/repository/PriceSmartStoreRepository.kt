package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.feature.inventario.data.mapper.toStoreSearchResult
import com.mg.costeoapp.feature.inventario.data.remote.BloomreachApi
import javax.inject.Inject

class PriceSmartStoreRepository @Inject constructor(
    private val bloomreachApi: BloomreachApi
) {

    suspend fun searchByBarcode(barcode: String): Result<List<StoreSearchResult>> {
        return searchByName(barcode)
    }

    suspend fun searchByName(query: String): Result<List<StoreSearchResult>> {
        return try {
            val response = bloomreachApi.search(query = query)
            if (response.isSuccessful) {
                val results = response.body()
                    ?.response
                    ?.docs
                    ?.map { it.toStoreSearchResult() }
                    ?: emptyList()
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
}
