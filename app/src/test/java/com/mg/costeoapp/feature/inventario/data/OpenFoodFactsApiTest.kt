package com.mg.costeoapp.feature.inventario.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mg.costeoapp.feature.inventario.data.mapper.toNutricionExterna
import com.mg.costeoapp.feature.inventario.data.remote.OpenFoodFactsApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Test de integracion contra Open Food Facts API real.
 * Usa dos codigos:
 * - 7622210449283 (Prince/LU) - tiene nutricion completa
 * - 7441001601132 (Leche Dos Pinos SV) - producto existe pero sin nutricion
 */
class OpenFoodFactsApiTest {

    private lateinit var api: OpenFoodFactsApi

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
                    .build()
                chain.proceed(request)
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(OpenFoodFactsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    // --- Producto con nutricion completa (Prince/LU) ---

    @Test
    fun `producto con nutricion retorna status 1`() = runTest {
        val response = api.getProductByBarcode("7622210449283")
        assertTrue("respuesta exitosa", response.isSuccessful)
        assertEquals("status 1 = encontrado", 1, response.body()!!.status)
    }

    @Test
    fun `DTO product_name viene en la respuesta`() = runTest {
        val product = fetchPrince()
        assertNotNull("product_name", product.productName)
        assertTrue("nombre no vacio", product.productName!!.isNotBlank())
    }

    @Test
    fun `DTO quantity viene con contenido`() = runTest {
        val product = fetchPrince()
        assertNotNull("quantity", product.quantity)
        assertTrue("quantity contiene g", product.quantity!!.contains("g", ignoreCase = true))
    }

    @Test
    fun `DTO serving_size viene con porcion`() = runTest {
        val product = fetchPrince()
        assertNotNull("serving_size", product.servingSize)
    }

    @Test
    fun `DTO brands viene en la respuesta`() = runTest {
        val product = fetchPrince()
        assertNotNull("brands", product.brands)
    }

    @Test
    fun `DTO nutriments tiene calorias`() = runTest {
        val product = fetchPrince()
        assertNotNull("nutriments", product.nutriments)
        assertNotNull("energy-kcal_100g", product.nutriments!!.energyKcal100g)
        assertTrue("calorias > 0", product.nutriments.energyKcal100g!! > 0)
    }

    @Test
    fun `DTO nutriments tiene proteinas`() = runTest {
        val product = fetchPrince()
        assertNotNull("proteins_100g", product.nutriments!!.proteins100g)
    }

    @Test
    fun `DTO nutriments tiene carbohidratos`() = runTest {
        val product = fetchPrince()
        assertNotNull("carbohydrates_100g", product.nutriments!!.carbohydrates100g)
    }

    @Test
    fun `DTO nutriments tiene grasas`() = runTest {
        val product = fetchPrince()
        assertNotNull("fat_100g", product.nutriments!!.fat100g)
    }

    // --- Mapeo a NutricionExterna ---

    @Test
    fun `mapeo a NutricionExterna tiene todos los campos`() = runTest {
        val product = fetchPrince()
        val nutricion = product.toNutricionExterna()

        assertEquals("fuente", "open_food_facts", nutricion.fuente)
        assertNotNull("porcionG", nutricion.porcionG)
        assertNotNull("calorias", nutricion.calorias)
        assertNotNull("proteinas", nutricion.proteinas)
        assertNotNull("carbohidratos", nutricion.carbohidratos)
        assertNotNull("grasas", nutricion.grasas)
        assertNotNull("cantidad (quantity)", nutricion.cantidad)
    }

    // --- Producto salvadoreno sin nutricion ---

    @Test
    fun `leche salvadorena existe pero sin nutricion`() = runTest {
        val response = api.getProductByBarcode("7441001601132")
        assertTrue("respuesta exitosa", response.isSuccessful)
        assertEquals("status 1", 1, response.body()!!.status)

        val product = response.body()!!.product!!
        val nutricion = product.toNutricionExterna()
        // Nutricion es null porque OFF no tiene datos para este producto
        assertNull("calorias null para producto SV", nutricion.calorias)
    }

    // --- Producto inexistente ---

    @Test
    fun `codigo inexistente retorna status 0`() = runTest {
        val response = api.getProductByBarcode("0000000000000")
        assertTrue("respuesta exitosa", response.isSuccessful)
        assertEquals("status 0 = no encontrado", 0, response.body()!!.status)
    }

    // --- Helper ---

    private suspend fun fetchPrince(): com.mg.costeoapp.feature.inventario.data.dto.OffProduct {
        val response = api.getProductByBarcode("7622210449283")
        assertTrue("API responde", response.isSuccessful)
        val body = response.body()!!
        assertEquals("status 1", 1, body.status)
        return body.product!!
    }
}
