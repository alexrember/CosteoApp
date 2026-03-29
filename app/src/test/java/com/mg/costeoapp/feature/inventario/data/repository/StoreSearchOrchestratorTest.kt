package com.mg.costeoapp.feature.inventario.data.repository

import com.mg.costeoapp.core.domain.model.StoreSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la logica del OrchestratedSearchResult sin instanciar el Orchestrator
 * (que depende de android.util.Log). Se testea el modelo de datos y la logica
 * de clasificacion de resultados.
 */
class StoreSearchOrchestratorTest {

    private fun makeResult(
        storeName: String = "Walmart SV",
        productName: String = "Leche UHT",
        price: Long? = 199,
        source: String = "walmart_vtex",
        isAvailable: Boolean = true,
        fetchUrl: String? = null,
        globalProductId: String? = null
    ) = StoreSearchResult(
        storeName = storeName,
        productName = productName,
        brand = null,
        ean = "7406249000413",
        price = price,
        listPrice = null,
        isAvailable = isAvailable,
        imageUrl = null,
        measurementUnit = null,
        unitMultiplier = null,
        source = source,
        globalProductId = globalProductId,
        fetchUrl = fetchUrl
    )

    @Test
    fun `OrchestratedSearchResult con resultados exitosos`() {
        val results = listOf(
            makeResult("Walmart SV", "Leche", 199),
            makeResult("PriceSmart SV", "Leche UHT", 189)
        )
        val orchestrated = OrchestratedSearchResult(
            results = results,
            storesSearched = listOf("Backend Costeo"),
            storesTimedOut = emptyList(),
            storesFailed = emptyList(),
            totalTimeMs = 1500
        )

        assertEquals(2, orchestrated.results.size)
        assertTrue(orchestrated.storesTimedOut.isEmpty())
        assertTrue(orchestrated.storesFailed.isEmpty())
    }

    @Test
    fun `OrchestratedSearchResult con timeout`() {
        val orchestrated = OrchestratedSearchResult(
            results = emptyList(),
            storesSearched = listOf("Backend Costeo"),
            storesTimedOut = listOf("Backend Costeo"),
            storesFailed = emptyList(),
            totalTimeMs = 8000
        )

        assertTrue(orchestrated.results.isEmpty())
        assertTrue(orchestrated.storesTimedOut.contains("Backend Costeo"))
    }

    @Test
    fun `OrchestratedSearchResult con fallo`() {
        val orchestrated = OrchestratedSearchResult(
            results = emptyList(),
            storesSearched = listOf("Backend Costeo"),
            storesTimedOut = emptyList(),
            storesFailed = listOf("Backend Costeo"),
            totalTimeMs = 500
        )

        assertTrue(orchestrated.results.isEmpty())
        assertTrue(orchestrated.storesFailed.contains("Backend Costeo"))
    }

    // --- Filtrado de resultados (logica del ScannerViewModel) ---

    @Test
    fun `filtrar resultados con precio de tiendas ignora Open Food Facts`() {
        val results = listOf(
            makeResult("PriceSmart SV", "Agua", 1129, "pricesmart_bloomreach"),
            makeResult("Open Food Facts", "Agua Mineral", null, "open_food_facts")
        )

        val storeResultsWithPrice = results.filter {
            it.source != "open_food_facts" && it.price != null && it.price > 0
        }

        assertEquals(1, storeResultsWithPrice.size)
        assertEquals("PriceSmart SV", storeResultsWithPrice[0].storeName)
    }

    @Test
    fun `filtrar solo Open Food Facts sin precio no tiene resultados de tienda`() {
        val results = listOf(
            makeResult("Open Food Facts", "Chicle Extra", null, "open_food_facts")
        )

        val storeResultsWithPrice = results.filter {
            it.source != "open_food_facts" && it.price != null && it.price > 0
        }

        assertTrue(storeResultsWithPrice.isEmpty())
    }

    @Test
    fun `filtrar resultados no disponibles`() {
        val results = listOf(
            makeResult("Walmart SV", "Leche", 199, isAvailable = true),
            makeResult("PriceSmart SV", "Leche", 189, isAvailable = false)
        )

        val available = results.filter { it.isAvailable && it.price != null }

        assertEquals(1, available.size)
        assertEquals("Walmart SV", available[0].storeName)
    }

    // --- Deduplicacion de tiendas (logica del ScannerViewModel) ---

    @Test
    fun `deduplicacion detecta PriceSmart y PriceSmart SV como misma tienda`() {
        val existing = "PriceSmart"
        val incoming = "PriceSmart SV"

        val isDuplicate = existing.lowercase().contains(incoming.lowercase().take(8)) ||
            incoming.lowercase().contains(existing.lowercase().take(8))

        assertTrue(isDuplicate)
    }

    @Test
    fun `deduplicacion no confunde Walmart con PriceSmart`() {
        val existing = "Walmart SV"
        val incoming = "PriceSmart SV"

        val isDuplicate = existing.lowercase().contains(incoming.lowercase().take(8)) ||
            incoming.lowercase().contains(existing.lowercase().take(8))

        assertTrue(!isDuplicate)
    }

    @Test
    fun `resultado con fetchUrl para actualizacion de precio`() {
        val result = makeResult(
            "PriceSmart SV", "Agua 24u", 1129,
            fetchUrl = "https://brm-core.pricesmart.com/api/v1/core/?q=agua"
        )

        assertEquals("https://brm-core.pricesmart.com/api/v1/core/?q=agua", result.fetchUrl)
    }

    @Test
    fun `globalProductId se propaga correctamente`() {
        val result = makeResult(globalProductId = "uuid-abc-123")

        assertEquals("uuid-abc-123", result.globalProductId)
    }

    @Test
    fun `precio en centavos correcto para PriceSmart`() {
        // PriceSmart price_SV ya viene en centavos (1129 = $11.29)
        val result = makeResult("PriceSmart SV", "Agua", 1129, "pricesmart_bloomreach")

        assertEquals(1129L, result.price)
        // Verificar que NO es 112900
        assertTrue(result.price!! < 10000) // Precio razonable < $100
    }
}
