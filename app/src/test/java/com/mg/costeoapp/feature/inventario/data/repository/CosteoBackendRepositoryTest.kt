package com.mg.costeoapp.feature.inventario.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.booleanOrNull
import com.mg.costeoapp.core.domain.model.StoreSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del parsing de respuestas del backend sin dependencias Android.
 * Valida que el mapeo JSON → StoreSearchResult funcione correctamente
 * para todos los formatos de respuesta que el backend puede enviar.
 */
class CosteoBackendRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    // DTOs duplicados del repositorio (son private en el original)
    @Serializable
    private data class TestSearchResultDto(
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
    private data class TestGlobalResponseDto(
        val globalProductId: String? = null,
        val product: TestProductDto? = null,
        val prices: List<TestSearchResultDto> = emptyList(),
        val results: List<TestSearchResultDto> = emptyList(),
        val fromCache: Boolean? = null
    )

    @Serializable
    private data class TestProductDto(
        val name: String? = null,
        val brand: String? = null,
        val unit: String? = null,
        val ean: String? = null
    )

    private fun parseResponse(responseBody: String): List<StoreSearchResult> {
        val trimmed = responseBody.trimStart()
        if (trimmed.startsWith("{")) {
            return try {
                val global = json.decodeFromString<TestGlobalResponseDto>(responseBody)
                val allResults = global.results.ifEmpty { global.prices }
                allResults.map { mapDto(it, global.globalProductId) }
            } catch (e: Exception) {
                parseLegacyArray(responseBody)
            }
        }
        return parseLegacyArray(responseBody)
    }

    private fun parseLegacyArray(responseBody: String): List<StoreSearchResult> {
        val dtos = json.decodeFromString<List<TestSearchResultDto>>(responseBody)
        return dtos.map { mapDto(it) }
    }

    private fun mapDto(dto: TestSearchResultDto, globalProductId: String? = null) = StoreSearchResult(
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

    // --- Formato global con prices ---

    @Test
    fun `parsea formato global con prices`() {
        val response = """
        {
            "globalProductId": "abc-123",
            "product": {"name": "Leche UHT", "brand": "Australian"},
            "prices": [
                {
                    "storeName": "Walmart SV",
                    "productName": "Leche UHT 1L",
                    "price": 199,
                    "isAvailable": true,
                    "source": "walmart_vtex"
                }
            ],
            "results": []
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(1, results.size)
        assertEquals("Walmart SV", results[0].storeName)
        assertEquals(199L, results[0].price)
        assertEquals("abc-123", results[0].globalProductId)
    }

    @Test
    fun `parsea formato global con results`() {
        val response = """
        {
            "globalProductId": "def-456",
            "results": [
                {
                    "storeName": "PriceSmart SV",
                    "productName": "Agua con Gas 24u",
                    "price": 1129,
                    "isAvailable": true,
                    "source": "pricesmart_bloomreach",
                    "fetchUrl": "https://api.example.com/price/123"
                }
            ]
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(1, results.size)
        assertEquals("PriceSmart SV", results[0].storeName)
        assertEquals(1129L, results[0].price)
        assertEquals("https://api.example.com/price/123", results[0].fetchUrl)
        assertEquals("def-456", results[0].globalProductId)
    }

    @Test
    fun `results tiene prioridad sobre prices`() {
        val response = """
        {
            "globalProductId": "g1",
            "prices": [
                {"storeName": "Old", "productName": "Old Item", "price": 100, "isAvailable": true, "source": "old"}
            ],
            "results": [
                {"storeName": "New", "productName": "New Item", "price": 200, "isAvailable": true, "source": "new"}
            ]
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(1, results.size)
        assertEquals("New", results[0].storeName)
        assertEquals(200L, results[0].price)
    }

    @Test
    fun `prices se usa cuando results esta vacio`() {
        val response = """
        {
            "globalProductId": "g2",
            "prices": [
                {"storeName": "Walmart", "productName": "Arroz", "price": 350, "isAvailable": true, "source": "walmart"}
            ],
            "results": []
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(1, results.size)
        assertEquals("Walmart", results[0].storeName)
    }

    // --- Formato legacy ---

    @Test
    fun `parsea formato legacy array`() {
        val response = """
        [
            {
                "storeName": "Open Food Facts",
                "productName": "Chicle Extra",
                "price": null,
                "isAvailable": true,
                "source": "open_food_facts"
            }
        ]
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(1, results.size)
        assertEquals("open_food_facts", results[0].source)
        assertNull(results[0].price)
    }

    @Test
    fun `parsea array con multiples tiendas`() {
        val response = """
        [
            {"storeName": "Walmart SV", "productName": "Leche", "price": 199, "isAvailable": true, "source": "walmart_vtex"},
            {"storeName": "PriceSmart SV", "productName": "Leche UHT", "price": 189, "isAvailable": true, "source": "pricesmart_bloomreach"},
            {"storeName": "Open Food Facts", "productName": "Leche", "price": null, "isAvailable": true, "source": "open_food_facts"}
        ]
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(3, results.size)
        assertEquals("Walmart SV", results[0].storeName)
        assertEquals("PriceSmart SV", results[1].storeName)
        assertEquals("Open Food Facts", results[2].storeName)
    }

    // --- Casos borde ---

    @Test
    fun `respuesta global vacia retorna lista vacia`() {
        val response = """{"globalProductId": null, "prices": [], "results": []}"""

        val results = parseResponse(response)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `array vacio retorna lista vacia`() {
        val results = parseResponse("[]")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `globalProductId se hereda del padre a cada resultado`() {
        val response = """
        {
            "globalProductId": "parent-uuid",
            "results": [
                {"storeName": "T1", "productName": "P1", "price": 100, "isAvailable": true, "source": "s1"},
                {"storeName": "T2", "productName": "P2", "price": 200, "isAvailable": true, "source": "s2"}
            ]
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals("parent-uuid", results[0].globalProductId)
        assertEquals("parent-uuid", results[1].globalProductId)
    }

    @Test
    fun `globalProductId del dto tiene prioridad sobre el padre`() {
        val response = """
        {
            "globalProductId": "parent-uuid",
            "results": [
                {"storeName": "T1", "productName": "P1", "price": 100, "isAvailable": true, "source": "s1", "globalProductId": "child-uuid"}
            ]
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals("child-uuid", results[0].globalProductId)
    }

    @Test
    fun `producto no disponible se parsea correctamente`() {
        val response = """
        [{"storeName": "Walmart SV", "productName": "Agotado", "price": 100, "isAvailable": false, "source": "walmart_vtex"}]
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals(false, results[0].isAvailable)
    }

    @Test
    fun `fetchUrl se mapea correctamente`() {
        val response = """
        {
            "results": [
                {
                    "storeName": "PriceSmart SV",
                    "productName": "Agua",
                    "price": 1129,
                    "isAvailable": true,
                    "source": "pricesmart",
                    "fetchUrl": "https://brm-core.pricesmart.com/api/v1/core/?q=agua"
                }
            ]
        }
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals("https://brm-core.pricesmart.com/api/v1/core/?q=agua", results[0].fetchUrl)
    }

    @Test
    fun `brand e imageUrl se parsean`() {
        val response = """
        [
            {
                "storeName": "Walmart",
                "productName": "Leche",
                "brand": "Australian",
                "imageUrl": "https://img.walmart.com/leche.jpg",
                "price": 199,
                "isAvailable": true,
                "source": "walmart_vtex"
            }
        ]
        """.trimIndent()

        val results = parseResponse(response)

        assertEquals("Australian", results[0].brand)
        assertEquals("https://img.walmart.com/leche.jpg", results[0].imageUrl)
    }

    @Test
    fun `precio PriceSmart en centavos no se multiplica`() {
        val response = """
        {
            "results": [
                {"storeName": "PriceSmart SV", "productName": "Agua 24u", "price": 1129, "isAvailable": true, "source": "pricesmart_bloomreach"}
            ]
        }
        """.trimIndent()

        val results = parseResponse(response)

        // 1129 centavos = $11.29 — NO multiplicar por 100
        assertEquals(1129L, results[0].price)
    }

    // --- link-barcode response parsing ---

    @Test
    fun `parsea respuesta link-barcode con result`() {
        val response = """
        {
            "result": {
                "storeName": "PriceSmart SV",
                "productName": "Agua con Gas 24u",
                "price": 1129,
                "isAvailable": true,
                "imageUrl": "https://img.example.com/agua.jpg"
            }
        }
        """.trimIndent()

        val parsed = json.parseToJsonElement(response).jsonObject
        val resultObj = parsed["result"]?.jsonObject

        assertNotNull(resultObj)
        assertEquals("PriceSmart SV", resultObj!!["storeName"]?.jsonPrimitive?.contentOrNull)
        assertEquals(1129L, resultObj["price"]?.jsonPrimitive?.longOrNull)
        assertEquals(true, resultObj["isAvailable"]?.jsonPrimitive?.booleanOrNull)
    }

    @Test
    fun `parsea respuesta link-barcode sin result`() {
        val response = """{"message": "No encontrado"}"""

        val parsed = json.parseToJsonElement(response).jsonObject
        val resultObj = parsed["result"]?.jsonObject

        assertNull(resultObj)
    }
}
