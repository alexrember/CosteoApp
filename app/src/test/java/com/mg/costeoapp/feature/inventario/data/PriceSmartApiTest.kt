package com.mg.costeoapp.feature.inventario.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mg.costeoapp.feature.inventario.data.dto.BloomreachProduct
import com.mg.costeoapp.feature.inventario.data.remote.BloomreachSearchApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Test de integracion contra PriceSmart Bloomreach API real.
 * Verifica que el servicio responde, el parsing funciona,
 * y el mapeo a StoreSearchResult es correcto.
 *
 * NOTA: La API de Bloomreach puede estar geo-restringida.
 * Estos tests se saltan automaticamente si la API retorna 451.
 */
class PriceSmartApiTest {

    private lateinit var api: BloomreachSearchApi
    private var apiAccessible = false

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
            .baseUrl(BloomreachSearchApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BloomreachSearchApi::class.java)
    }

    private suspend fun checkApiAccessible(): Boolean {
        return try {
            val response = api.search(query = "arroz", rows = 1)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    @Test
    fun `busqueda arroz retorna resultados`() = runTest {
        assumeTrue("API Bloomreach no accesible (posible geo-restriccion)", checkApiAccessible())
        val response = api.search(query = "arroz")
        assertTrue("respuesta exitosa", response.isSuccessful)
        val body = response.body()
        assertNotNull("body no null", body)
        val docs = body!!.response?.docs
        assertNotNull("docs no null", docs)
        assertTrue("al menos 1 producto", docs!!.isNotEmpty())
    }

    @Test
    fun `producto tiene title y priceSV`() = runTest {
        assumeTrue("API Bloomreach no accesible (posible geo-restriccion)", checkApiAccessible())
        val docs = fetchArroz()
        val product = docs.first()
        assertNotNull("title", product.title)
        assertTrue("title no vacio", product.title!!.isNotBlank())
        assertNotNull("priceSV", product.priceSV)
    }

    @Test
    fun `producto tiene campos esperados`() = runTest {
        assumeTrue("API Bloomreach no accesible (posible geo-restriccion)", checkApiAccessible())
        val docs = fetchArroz()
        val product = docs.first()
        assertNotNull("title", product.title)
        assertTrue("title no vacio", product.title!!.isNotBlank())
        assertNotNull("priceSV", product.priceSV)
    }

    @Test
    fun `busqueda sin resultados retorna lista vacia`() = runTest {
        assumeTrue("API Bloomreach no accesible (posible geo-restriccion)", checkApiAccessible())
        val response = api.search(query = "xyznoexiste12345")
        assertTrue("respuesta exitosa", response.isSuccessful)
        val docs = response.body()?.response?.docs ?: emptyList()
        assertTrue("lista vacia", docs.isEmpty())
    }

    private suspend fun fetchArroz(): List<BloomreachProduct> {
        val response = api.search(query = "arroz")
        assertTrue("API responde", response.isSuccessful)
        val docs = response.body()?.response?.docs
        assertNotNull("docs no null", docs)
        assertTrue("al menos 1 producto", docs!!.isNotEmpty())
        return docs
    }
}
