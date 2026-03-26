package com.mg.costeoapp.feature.sync.data

import android.util.Log
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.security.NativeSecrets
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductContributionService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val supabaseClient: SupabaseClient
) {

    suspend fun contribute(producto: Producto): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val ean = producto.codigoBarras
            if (ean.isNullOrBlank()) {
                return@withContext Result.success(false)
            }

            val accessToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
            if (accessToken.isNullOrBlank()) {
                Log.d(TAG, "No auth session, skipping contribution")
                return@withContext Result.success(false)
            }

            val body = buildJsonObject {
                put("ean", ean)
                put("nombre", producto.nombre)
                put("unidad_medida", producto.unidadMedida)
                put("cantidad_por_empaque", producto.cantidadPorEmpaque)
            }

            val url = "${NativeSecrets.getSupabaseUrl()}/functions/v1/contribute-product"

            val request = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .header("Authorization", "Bearer $accessToken")
                .header("apikey", NativeSecrets.getSupabaseAnonKey())
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val responseBody = it.body.string()
                    val promoted = responseBody.contains("\"promoted\":true", ignoreCase = true)
                    Log.d(TAG, "Contribution OK for EAN=$ean, promoted=$promoted")
                    Result.success(promoted)
                } else {
                    Log.w(TAG, "Contribution failed: ${it.code} ${it.message}")
                    Result.success(false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Contribution error", e)
            Result.success(false)
        }
    }

    companion object {
        private const val TAG = "ProductContribution"
    }
}
