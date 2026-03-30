package com.mg.costeoapp.feature.inventario.data.repository

import android.util.Log
import com.mg.costeoapp.core.domain.model.StoreSearchResult
import com.mg.costeoapp.core.security.NativeSecrets
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Serializable
private data class LinkBarcodeRequest(val ean: String, val itemNumber: String)

@Serializable
private data class BackendSearchRequest(
    val query: String? = null,
    val barcode: String? = null,
    val stores: List<String>? = null
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
    val source: String,
    val globalProductId: String? = null,
    val fetchUrl: String? = null
)

@Serializable
private data class BackendGlobalResponseDto(
    val globalProductId: String? = null,
    val product: BackendProductDto? = null,
    val prices: List<BackendSearchResultDto> = emptyList(),
    val results: List<BackendSearchResultDto> = emptyList(),
    val fromCache: Boolean? = null
)

@Serializable
private data class BackendProductDto(
    val name: String? = null,
    val brand: String? = null,
    val unit: String? = null,
    val ean: String? = null
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

    suspend fun searchByBarcode(barcode: String, stores: List<String>? = null): Result<List<StoreSearchResult>> {
        return executeSearch(BackendSearchRequest(barcode = barcode, stores = stores))
    }

    suspend fun searchByName(query: String, stores: List<String>? = null): Result<List<StoreSearchResult>> {
        return executeSearch(BackendSearchRequest(query = query, stores = stores))
    }

    suspend fun linkBarcodeToItemNumber(ean: String, itemNumber: String): Result<List<StoreSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val url = "${supabaseUrl}/functions/v1/link-barcode"
            val bodyJson = json.encodeToString(LinkBarcodeRequest.serializer(), LinkBarcodeRequest(ean, itemNumber))
            val session = supabaseClient.auth.currentSessionOrNull()
            val requestBuilder = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .header("apikey", anonKey)
            if (session?.accessToken != null) {
                requestBuilder.header("Authorization", "Bearer ${session.accessToken}")
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("link-barcode respondio ${resp.code}"))
                }
                val respBody = resp.body.string()
                val parsed = json.parseToJsonElement(respBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                if (resultObj != null) {
                    val r = StoreSearchResult(
                        storeName = resultObj["storeName"]?.jsonPrimitive?.contentOrNull ?: "PriceSmart SV",
                        productName = resultObj["productName"]?.jsonPrimitive?.contentOrNull ?: "Sin nombre",
                        brand = resultObj["brand"]?.jsonPrimitive?.contentOrNull,
                        ean = resultObj["ean"]?.jsonPrimitive?.contentOrNull,
                        price = resultObj["price"]?.jsonPrimitive?.longOrNull,
                        listPrice = null,
                        isAvailable = resultObj["isAvailable"]?.jsonPrimitive?.booleanOrNull ?: true,
                        imageUrl = resultObj["imageUrl"]?.jsonPrimitive?.contentOrNull,
                        measurementUnit = null,
                        unitMultiplier = null,
                        source = "pricesmart_bloomreach"
                    )
                    Result.success(listOf(r))
                } else {
                    Result.success(emptyList())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "link-barcode error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun executeSearch(
        searchRequest: BackendSearchRequest
    ): Result<List<StoreSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val url = "${supabaseUrl}${EDGE_FUNCTION_PATH}"
            val body = json.encodeToString(BackendSearchRequest.serializer(), searchRequest)
                .toRequestBody("application/json".toMediaType())

            // Try to get current session, refresh if needed
            val session = supabaseClient.auth.currentSessionOrNull()
                ?: try { supabaseClient.auth.refreshCurrentSession(); supabaseClient.auth.currentSessionOrNull() } catch (_: Exception) { null }
            if (session == null) Log.w(TAG, "No auth session, using anon key")

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
            if (session?.accessToken != null) {
                requestBuilder.header("Authorization", "Bearer ${session.accessToken}")
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Backend respondio con codigo ${resp.code}")
                    return@withContext Result.failure(
                        Exception("Backend respondio con codigo ${resp.code}")
                    )
                }

                val responseBody = resp.body.string()
                val results = parseResponse(responseBody)
                Result.success(results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en busqueda backend: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseResponse(responseBody: String): List<StoreSearchResult> {
        val trimmed = responseBody.trimStart()

        // Nuevo formato: objeto con globalProductId + prices array
        if (trimmed.startsWith("{")) {
            return try {
                val global = json.decodeFromString<BackendGlobalResponseDto>(responseBody)
                val allResults = global.results.ifEmpty { global.prices }
                allResults.map { dto ->
                    mapDto(dto, globalProductId = global.globalProductId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parseando formato global, intentando formato legacy: ${e.message}")
                parseLegacyArray(responseBody)
            }
        }

        // Formato legacy: array de resultados
        return parseLegacyArray(responseBody)
    }

    private fun parseLegacyArray(responseBody: String): List<StoreSearchResult> {
        val dtos = json.decodeFromString<List<BackendSearchResultDto>>(responseBody)
        return dtos.map { mapDto(it) }
    }

    private fun mapDto(
        dto: BackendSearchResultDto,
        globalProductId: String? = null
    ): StoreSearchResult {
        return StoreSearchResult(
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
            source = dto.source,
            globalProductId = dto.globalProductId ?: globalProductId,
            fetchUrl = dto.fetchUrl
        )
    }
}
