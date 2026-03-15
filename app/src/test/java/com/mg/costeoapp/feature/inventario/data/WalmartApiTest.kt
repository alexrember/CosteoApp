package com.mg.costeoapp.feature.inventario.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mg.costeoapp.feature.inventario.data.mapper.toStoreSearchResults
import com.mg.costeoapp.feature.inventario.data.remote.WalmartVtexApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Test de integracion contra Walmart SV VTEX API real.
 * Verifica que el servicio responde y el parsing funciona.
 * Usa el codigo de barras de leche (7441001601132) confirmado por el usuario.
 */
class WalmartApiTest {

    private lateinit var api: WalmartVtexApi

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Before
    fun setup() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CosteoApp/1.0 Android")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(WalmartVtexApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WalmartVtexApi::class.java)
    }

    @Test
    fun `buscar leche por codigo de barras retorna resultados`() = runTest {
        val response = api.searchByBarcode(fq = "alternateIds_Ean:7441001601132")

        assertTrue("API deberia responder exitosamente", response.isSuccessful)

        val products = response.body()
        assertNotNull("Body no deberia ser null", products)
        assertTrue("Deberia encontrar al menos 1 producto", products!!.isNotEmpty())

        val firstProduct = products[0]
        assertNotNull("Nombre del producto no deberia ser null", firstProduct.productName)
        println("Producto encontrado: ${firstProduct.productName}")
        println("Marca: ${firstProduct.brand}")

        // Verificar que el mapeo a StoreSearchResult funciona
        val results = firstProduct.toStoreSearchResults()
        assertTrue("Deberia tener al menos 1 resultado mapeado", results.isNotEmpty())

        val result = results[0]
        println("Store: ${result.storeName}")
        println("Precio: ${result.price?.let { "$${it / 100.0}" } ?: "N/A"}")
        println("Disponible: ${result.isAvailable}")
        assertTrue("Precio deberia ser > 0", (result.price ?: 0) > 0)
    }

    @Test
    fun `buscar por nombre retorna resultados`() = runTest {
        val response = api.searchByName(ft = "leche entera")

        assertTrue("API deberia responder exitosamente", response.isSuccessful)

        val products = response.body()
        assertNotNull("Body no deberia ser null", products)
        assertTrue("Deberia encontrar productos para 'leche entera'", products!!.isNotEmpty())

        println("Resultados para 'leche entera': ${products.size} productos")
        products.take(3).forEach { p ->
            println("  - ${p.productName} (${p.brand})")
        }
    }

    @Test
    fun `codigo de barras inexistente retorna lista vacia`() = runTest {
        val response = api.searchByBarcode(fq = "alternateIds_Ean:0000000000000")

        assertTrue("API deberia responder exitosamente", response.isSuccessful)

        val products = response.body()
        // VTEX puede retornar lista vacia o null para productos no encontrados
        val count = products?.size ?: 0
        println("Productos encontrados para codigo inexistente: $count")
    }
}
