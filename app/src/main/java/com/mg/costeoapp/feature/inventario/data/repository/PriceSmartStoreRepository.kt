package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.core.security.NativeSecrets
import com.mg.costeoapp.feature.inventario.data.remote.BloomreachSearchApi
import javax.inject.Inject
import kotlin.math.roundToLong

class PriceSmartStoreRepository @Inject constructor(
    private val searchApi: BloomreachSearchApi
) {

    suspend fun searchByBarcode(barcode: String): Result<List<StoreSearchResult>> {
        return searchByName(barcode)
    }

    suspend fun searchByName(query: String): Result<List<StoreSearchResult>> {
        val sanitized = query.take(100).replace(Regex("[^\\p{L}\\p{N}\\s]"), "").trim()
        if (sanitized.isBlank()) return Result.success(emptyList())
        return try {
            val response = searchApi.search(
                accountId = NativeSecrets.getBloomreachAccountId(),
                authKey = NativeSecrets.getBloomreachAuthKey(),
                query = sanitized
            )
            if (response.isSuccessful) {
                val results = response.body()
                    ?.response
                    ?.docs
                    ?.map { product ->
                        // price_SV is in cents (e.g., 1649.0 = $16.49)
                        val priceInCents = product.priceSV?.takeIf { it > 0 }?.roundToLong()
                        StoreSearchResult(
                            storeName = "PriceSmart",
                            productName = product.title ?: "Sin nombre",
                            brand = product.brand,
                            ean = null,
                            price = priceInCents,
                            listPrice = priceInCents,
                            isAvailable = product.availabilitySV == "true",
                            imageUrl = product.thumbImage,
                            measurementUnit = null,
                            unitMultiplier = null,
                            source = "pricesmart_bloomreach"
                        )
                    }
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
