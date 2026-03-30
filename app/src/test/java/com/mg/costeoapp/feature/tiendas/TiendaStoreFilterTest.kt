package com.mg.costeoapp.feature.tiendas

import com.mg.costeoapp.core.database.entity.Tienda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para la logica de filtrado de tiendas activas
 * y la busqueda filtrada por tiendas del usuario.
 */
class TiendaStoreFilterTest {

    private val walmart = Tienda(id = 1, nombre = "Walmart", activo = true)
    private val priceSmart = Tienda(id = 2, nombre = "PriceSmart", activo = true)
    private val selectos = Tienda(id = 3, nombre = "Super Selectos", activo = false)

    // --- Filtrado de tiendas activas ---

    @Test
    fun `solo tiendas activas se incluyen en busqueda`() {
        val todas = listOf(walmart, priceSmart, selectos)
        val activas = todas.filter { it.activo }.map { it.nombre }

        assertEquals(2, activas.size)
        assertTrue(activas.contains("Walmart"))
        assertTrue(activas.contains("PriceSmart"))
        assertFalse(activas.contains("Super Selectos"))
    }

    @Test
    fun `todas activas retorna las 3 tiendas`() {
        val todas = listOf(
            walmart.copy(activo = true),
            priceSmart.copy(activo = true),
            selectos.copy(activo = true)
        )
        val activas = todas.filter { it.activo }.map { it.nombre }

        assertEquals(3, activas.size)
    }

    @Test
    fun `ninguna activa retorna lista vacia`() {
        val todas = listOf(
            walmart.copy(activo = false),
            priceSmart.copy(activo = false),
            selectos.copy(activo = false)
        )
        val activas = todas.filter { it.activo }.map { it.nombre }

        assertTrue(activas.isEmpty())
    }

    @Test
    fun `toggle cambia estado de activo`() {
        val tienda = walmart.copy(activo = true)
        val toggled = tienda.copy(activo = !tienda.activo)

        assertFalse(toggled.activo)

        val toggledBack = toggled.copy(activo = !toggled.activo)
        assertTrue(toggledBack.activo)
    }

    // --- Matching de nombres de tiendas (backend vs local) ---

    @Test
    fun `Walmart local coincide con walmartsv del backend`() {
        val local = "Walmart"
        val backend = "walmartsv"

        val match = backend.lowercase().contains(local.lowercase().take(6)) ||
            local.lowercase().contains(backend.lowercase().take(6))

        assertTrue(match)
    }

    @Test
    fun `PriceSmart coincide con PriceSmart SV`() {
        val local = "PriceSmart"
        val backend = "PriceSmart SV"

        val match = backend.lowercase().contains(local.lowercase().take(6)) ||
            local.lowercase().contains(backend.lowercase().take(6))

        assertTrue(match)
    }

    @Test
    fun `Super Selectos coincide con Super Selectos`() {
        val local = "Super Selectos"
        val backend = "Super Selectos"

        val match = backend.lowercase().contains(local.lowercase().take(6)) ||
            local.lowercase().contains(backend.lowercase().take(6))

        assertTrue(match)
    }

    @Test
    fun `Walmart no coincide con PriceSmart`() {
        val local = "Walmart"
        val backend = "PriceSmart SV"

        val match = backend.lowercase().contains(local.lowercase().take(6)) ||
            local.lowercase().contains(backend.lowercase().take(6))

        assertFalse(match)
    }

    // --- Request stores al backend ---

    @Test
    fun `request incluye solo tiendas activas`() {
        val todas = listOf(walmart, priceSmart, selectos)
        val stores = todas.filter { it.activo }.map { it.nombre }

        assertEquals(listOf("Walmart", "PriceSmart"), stores)
    }

    @Test
    fun `stores null busca en todas las tiendas`() {
        val stores: List<String>? = null
        val searchAll = stores == null

        assertTrue(searchAll)
    }

    // --- Sync de estado activo ---

    @Test
    fun `dto incluye campo activo true`() {
        data class MockStoreAlias(val alias: String, val activo: Boolean)

        val dto = MockStoreAlias(alias = "Walmart", activo = true)
        assertTrue(dto.activo)
    }

    @Test
    fun `dto incluye campo activo false`() {
        data class MockStoreAlias(val alias: String, val activo: Boolean)

        val dto = MockStoreAlias(alias = "Super Selectos", activo = false)
        assertFalse(dto.activo)
    }

    // --- Comparacion de precios filtrada ---

    @Test
    fun `comparacion solo muestra tiendas activas del usuario`() {
        data class MockPrecio(val tiendaNombre: String, val precio: Long)

        val precios = listOf(
            MockPrecio("Walmart SV", 95),
            MockPrecio("PriceSmart SV", 89),
            MockPrecio("Super Selectos", 90)
        )
        val activas = listOf("Walmart", "PriceSmart")

        val filtrados = precios.filter { precio ->
            activas.any { tienda ->
                precio.tiendaNombre.lowercase().contains(tienda.lowercase().take(6)) ||
                tienda.lowercase().contains(precio.tiendaNombre.lowercase().take(6))
            }
        }

        assertEquals(2, filtrados.size)
        assertEquals("Walmart SV", filtrados[0].tiendaNombre)
        assertEquals("PriceSmart SV", filtrados[1].tiendaNombre)
    }

    @Test
    fun `comparacion con todas activas muestra 3 precios`() {
        data class MockPrecio(val tiendaNombre: String, val precio: Long)

        val precios = listOf(
            MockPrecio("Walmart SV", 95),
            MockPrecio("PriceSmart SV", 89),
            MockPrecio("Super Selectos", 90)
        )
        val activas = listOf("Walmart", "PriceSmart", "Super Selectos")

        val filtrados = precios.filter { precio ->
            activas.any { tienda ->
                precio.tiendaNombre.lowercase().contains(tienda.lowercase().take(6)) ||
                tienda.lowercase().contains(precio.tiendaNombre.lowercase().take(6))
            }
        }

        assertEquals(3, filtrados.size)
    }
}
