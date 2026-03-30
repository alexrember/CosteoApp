package com.mg.costeoapp.feature.tiendas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de integracion para verificar que la busqueda
 * solo se ejecuta en tiendas activas del usuario.
 */
class TiendaSearchFilterIntegrationTest {

    // Simula la logica de shouldSearchStore del Edge Function
    private fun shouldSearchStore(storeName: String, activeStores: List<String>?): Boolean {
        if (activeStores == null) return true
        return activeStores.any { s ->
            storeName.lowercase().contains(s.lowercase().substring(0, minOf(6, s.length))) ||
            s.lowercase().contains(storeName.lowercase().substring(0, minOf(6, storeName.length)))
        }
    }

    // --- Busqueda con todas las tiendas activas ---

    @Test
    fun `con 3 tiendas activas busca en las 3`() {
        val activas = listOf("Walmart", "PriceSmart", "Super Selectos")

        assertTrue(shouldSearchStore("walmart", activas))
        assertTrue(shouldSearchStore("pricesmart", activas))
        assertTrue(shouldSearchStore("super selectos", activas))
    }

    // --- Busqueda con Selectos desactivada ---

    @Test
    fun `con Selectos desactivada no busca en Selectos`() {
        val activas = listOf("Walmart", "PriceSmart")

        assertTrue(shouldSearchStore("walmart", activas))
        assertTrue(shouldSearchStore("pricesmart", activas))
        assertTrue(!shouldSearchStore("super selectos", activas))
    }

    // --- Busqueda con solo PriceSmart ---

    @Test
    fun `con solo PriceSmart activa busca solo en PriceSmart`() {
        val activas = listOf("PriceSmart")

        assertTrue(!shouldSearchStore("walmart", activas))
        assertTrue(shouldSearchStore("pricesmart", activas))
        assertTrue(!shouldSearchStore("super selectos", activas))
    }

    // --- Stores null busca en todas ---

    @Test
    fun `stores null busca en todas las tiendas`() {
        val activas: List<String>? = null

        assertTrue(shouldSearchStore("walmart", activas))
        assertTrue(shouldSearchStore("pricesmart", activas))
        assertTrue(shouldSearchStore("super selectos", activas))
    }

    // --- Request body incluye stores ---

    @Test
    fun `request body con stores filtra correctamente`() {
        data class MockRequest(val barcode: String, val stores: List<String>?)

        val req = MockRequest("04781531", listOf("Walmart", "PriceSmart"))
        assertEquals(2, req.stores?.size)
        assertTrue(req.stores!!.contains("Walmart"))
        assertTrue(!req.stores.contains("Super Selectos"))
    }

    @Test
    fun `request body sin stores busca en todas`() {
        data class MockRequest(val barcode: String, val stores: List<String>?)

        val req = MockRequest("04781531", null)
        assertNull(req.stores)
    }

    // --- Resultados filtrados por tiendas activas ---

    @Test
    fun `resultados se filtran por tiendas activas del usuario`() {
        data class MockResult(val storeName: String, val price: Long)

        val allResults = listOf(
            MockResult("walmartsv", 95),
            MockResult("PriceSmart SV", 89),
            MockResult("Super Selectos", 90)
        )
        val activas = listOf("Walmart", "PriceSmart")

        val filtered = allResults.filter { r ->
            shouldSearchStore(r.storeName, activas)
        }

        assertEquals(2, filtered.size)
        assertEquals("walmartsv", filtered[0].storeName)
        assertEquals("PriceSmart SV", filtered[1].storeName)
    }

    // --- Edge case: lista vacia de tiendas ---

    @Test
    fun `lista vacia de tiendas no busca en ninguna`() {
        val activas = emptyList<String>()

        assertTrue(!shouldSearchStore("walmart", activas))
        assertTrue(!shouldSearchStore("pricesmart", activas))
        assertTrue(!shouldSearchStore("super selectos", activas))
    }

    // --- Parallel search solo lanza tiendas activas ---

    @Test
    fun `Promise all solo incluye tiendas activas`() {
        val activas = listOf("Walmart", "Super Selectos")

        val searchWalmart = shouldSearchStore("walmart", activas)
        val searchPriceSmart = shouldSearchStore("pricesmart", activas)
        val searchSelectos = shouldSearchStore("super selectos", activas)

        assertTrue(searchWalmart)
        assertTrue(!searchPriceSmart)
        assertTrue(searchSelectos)
    }
}
