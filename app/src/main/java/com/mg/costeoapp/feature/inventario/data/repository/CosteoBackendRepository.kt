package com.mg.costeoapp.feature.inventario.data.repository

import android.util.Log
import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.core.security.NativeSecrets
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Serializable
private data class BackendSearchRequest(
    val query: String? = null,
    val barcode: String? = null
)

@Serializable
private data class BackendSearchResultDto(
    val storeName: String,
    val productName: String,
    val brand: String? = null,
    val ean: String? = null,
    val price: Long? = null,
    val listPrice: Long? = null,
    val isAvailable: Boolean = true,
    val imageUrl: String? = null,
    val measurementUnit: String? = null,
    val unitMultiplier: Double? = null,
    val source: String
)

class CosteoBackendRepository @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "CosteoBackend"
        private const val EDGE_FUNCTION_PATH = "/functions/v1/search-products"
    }

    private val supabaseUrl: String by lazy { NativeSecrets.getSupabaseUrl() }
    private val anonKey: String by lazy { NativeSecrets.getSupabaseAnonKey() }

    suspend fun searchByBarcode(barcode: String): Result<List<StoreSearchResult>> {
        return executeSearch(BackendSearchRequest(barcode = barcode))
    }

    suspend fun searchByName(query: String): Result<List<StoreSearchResult>> {
        return executeSearch(BackendSearchRequest(query = query))
    }

    private suspend fun executeSearch(
        searchRequest: BackendSearchRequest
    ): Result<List<StoreSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val url = "${supabaseUrl}${EDGE_FUNCTION_PATH}"
            val body = json.encodeToString(BackendSearchRequest.serializer(), searchRequest)
                .toRequestBody("application/json".toMediaType())

            val bearerToken = supabaseClient.auth.currentSessionOrNull()?.accessToken ?: anonKey

            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer $bearerToken")
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Backend respondio con codigo ${resp.code}")
                    return@withContext Result.failure(
                        Exception("Backend respondio con codigo ${resp.code}")
                    )
                }

                val responseBody = resp.body.string()

                val dtos = json.decodeFromString<List<BackendSearchResultDto>>(responseBody)
                val results = dtos.map { dto ->
                    StoreSearchResult(
                        storeName = dto.storeName,
                        productName = dto.productName,
                        brand = dto.brand,
                        ean = dto.ean,
                        price = dto.price,
                        listPrice = dto.listPrice,
                        isAvailable = dto.isAvailable,
                        imageUrl = dto.imageUrl,
                        measurementUnit = dto.measurementUnit,
                        unitMultiplier = dto.unitMultiplier,
                        source = dto.source
                    )
                }

                Result.success(results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en busqueda backend: ${e.message}", e)
            Result.failure(e)
        }
    }
}
