package com.mg.costeoapp.feature.inventario.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mg.costeoapp.feature.inventario.data.mapper.parseContenidoFromName
import com.mg.costeoapp.feature.inventario.data.mapper.toStoreSearchResults
import com.mg.costeoapp.feature.inventario.data.remote.WalmartVtexApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Test de integracion contra Walmart SV VTEX API real.
 * Verifica que el servicio responde, el parsing funciona,
 * y todos los campos que necesitamos estan presentes.
 *
 * Codigo de barras de referencia: 7441001601132 (Leche Dos Pinos 946ml)
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

    // --- DTO: campos crudos de la API ---

    @Test
    fun `DTO productName viene en la respuesta`() = runTest {
        val products = fetchLeche()
        assertNotNull("productName", products[0].productName)
        assertTrue("productName no vacio", products[0].productName!!.isNotBlank())
    }

    @Test
    fun `DTO brand viene en la respuesta`() = runTest {
        val products = fetchLeche()
        assertNotNull("brand", products[0].brand)
    }

    @Test
    fun `DTO items tiene al menos un item`() = runTest {
        val products = fetchLeche()
        val items = products[0].items
        assertNotNull("items", items)
        assertTrue("items no vacio", items!!.isNotEmpty())
    }

    @Test
    fun `DTO item tiene ean`() = runTest {
        val products = fetchLeche()
        val item = products[0].items!!.first()
        assertNotNull("ean", item.ean)
        assertEquals("7441001601132", item.ean)
    }

    @Test
    fun `DTO item tiene measurementUnit`() = runTest {
        val products = fetchLeche()
        val item = products[0].items!!.first()
        assertNotNull("measurementUnit", item.measurementUnit)
    }

    @Test
    fun `DTO item tiene sellers con precio`() = runTest {
        val products = fetchLeche()
        val sellers = products[0].items!!.first().sellers
        assertNotNull("sellers", sellers)
        assertTrue("sellers no vacio", sellers!!.isNotEmpty())

        val offer = sellers[0].commertialOffer
        assertNotNull("commertialOffer", offer)
        assertNotNull("price", offer!!.price)
        assertTrue("price > 0", offer.price!! > 0)
    }

    @Test
    fun `DTO seller tiene sellerName`() = runTest {
        val products = fetchLeche()
        val seller = products[0].items!!.first().sellers!!.first()
        assertNotNull("sellerName", seller.sellerName)
    }

    @Test
    fun `DTO offer tiene isAvailable`() = runTest {
        val products = fetchLeche()
        val offer = products[0].items!!.first().sellers!!.first().commertialOffer!!
        assertNotNull("isAvailable", offer.isAvailable)
    }

    // --- Mapeo: StoreSearchResult ---

    @Test
    fun `mapeo a StoreSearchResult tiene todos los campos`() = runTest {
        val products = fetchLeche()
        val results = products[0].toStoreSearchResults()
        assertTrue("resultados no vacio", results.isNotEmpty())

        val r = results[0]
        assertTrue("storeName no vacio", r.storeName.isNotBlank())
        assertTrue("productName no vacio", r.productName.isNotBlank())
        assertNotNull("price", r.price)
        assertTrue("price > 0", r.price!! > 0)
        assertNotNull("ean", r.ean)
        assertEquals("source", "walmart_vtex", r.source)
    }

    // --- ContentParser: extrae contenido del nombre ---

    @Test
    fun `contenido se parsea del nombre del producto`() = runTest {
        val products = fetchLeche()
        val nombre = products[0].productName!!
        val contenido = parseContenidoFromName(nombre)
        assertNotNull("contenido parseado del nombre: $nombre", contenido)
        assertEquals("cantidad 946", 946.0, contenido!!.cantidad, 0.01)
        assertEquals("unidad ml", com.mg.costeoapp.core.util.UnidadMedida.MILILITRO, contenido.unidad)
    }

    // --- Busqueda por nombre ---

    @Test
    fun `busqueda por nombre retorna resultados`() = runTest {
        val response = api.searchByName(ft = "leche entera")
        assertTrue("respuesta exitosa", response.isSuccessful)
        val products = response.body()
        assertNotNull("body", products)
        assertTrue("productos encontrados", products!!.isNotEmpty())
    }

    // --- Codigo inexistente ---

    @Test
    fun `codigo inexistente retorna lista vacia`() = runTest {
        val response = api.searchByBarcode(fq = "alternateIds_Ean:0000000000000")
        assertTrue("respuesta exitosa", response.isSuccessful)
        val products = response.body() ?: emptyList()
        assertTrue("lista vacia o sin resultados", products.isEmpty())
    }

    // --- Helper ---

    private suspend fun fetchLeche(): List<com.mg.costeoapp.feature.inventario.data.dto.VtexProduct> {
        val response = api.searchByBarcode(fq = "alternateIds_Ean:7441001601132")
        assertTrue("API responde", response.isSuccessful)
        val products = response.body()
        assertNotNull("body no null", products)
        assertTrue("al menos 1 producto", products!!.isNotEmpty())
        return products
    }
}
