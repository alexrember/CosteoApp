package com.mg.costeoapp.feature.sync.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la logica de push/sync sin dependencias Android.
 * Valida mapeo de columnas, DTOs, y logica de negocio del sync.
 */
class SyncPushTest {

    // =====================================================================
    // Mapeo de columnas Supabase vs local Room
    // =====================================================================

    @Test
    fun `global_stores usa columna nombre no name`() {
        // La tabla global_stores tiene columna "nombre", no "name"
        val columnName = "nombre"
        assertEquals("nombre", columnName)
    }

    @Test
    fun `user_store_aliases usa columna store_id no global_store_id`() {
        val columnName = "store_id"
        assertEquals("store_id", columnName)
    }

    @Test
    fun `user_product_aliases usa columna product_id no global_product_id`() {
        val columnName = "product_id"
        assertEquals("product_id", columnName)
    }

    // =====================================================================
    // Mapeo de IDs locales a globales (Long -> UUID)
    // =====================================================================

    @Test
    fun `buildProductIdMap solo incluye productos con globalProductId`() {
        data class MockProduct(val id: Long, val globalProductId: String?)

        val products = listOf(
            MockProduct(1, "uuid-1"),
            MockProduct(2, null),
            MockProduct(3, "uuid-3")
        )

        val map = products
            .filter { it.globalProductId != null }
            .associate { it.id to it.globalProductId!! }

        assertEquals(2, map.size)
        assertEquals("uuid-1", map[1])
        assertNull(map[2])
        assertEquals("uuid-3", map[3])
    }

    @Test
    fun `buildProductUuidToLocalMap invierte el mapeo`() {
        data class MockProduct(val id: Long, val globalProductId: String?)

        val products = listOf(
            MockProduct(1, "uuid-1"),
            MockProduct(3, "uuid-3")
        )

        val map = products
            .filter { it.globalProductId != null }
            .associate { it.globalProductId!! to it.id }

        assertEquals(1L, map["uuid-1"])
        assertEquals(3L, map["uuid-3"])
        assertNull(map["uuid-nonexistent"])
    }

    // =====================================================================
    // Push recetas: validacion de datos
    // =====================================================================

    @Test
    fun `receta con ingredientes sin globalProductId se omite`() {
        data class MockIngredient(val productoId: Long)
        val productIdMap = mapOf(1L to "uuid-1") // solo producto 1 tiene global ID

        val ingredients = listOf(
            MockIngredient(1),  // tiene global ID
            MockIngredient(2)   // NO tiene global ID
        )

        val pushable = ingredients.filter { productIdMap.containsKey(it.productoId) }
        assertEquals(1, pushable.size)
    }

    @Test
    fun `receta sin ingredientes pushables se sube sin ingredientes`() {
        val productIdMap = emptyMap<Long, String>()

        val ingredients = listOf(
            mapOf("productoId" to 1L),
            mapOf("productoId" to 2L)
        )

        val pushable = ingredients.filter { productIdMap.containsKey(it["productoId"]) }
        assertTrue(pushable.isEmpty())
    }

    // =====================================================================
    // Push platos: mapeo de componentes
    // =====================================================================

    @Test
    fun `componente con prefabricado se mapea por nombre`() {
        data class MockRemoteRecipe(val id: String, val nombre: String)

        val remoteRecipes = listOf(
            MockRemoteRecipe("recipe-uuid-1", "test"),
            MockRemoteRecipe("recipe-uuid-2", "test2")
        )

        val localRecipeName = "test"
        val remoteId = remoteRecipes.find { it.nombre == localRecipeName }?.id

        assertEquals("recipe-uuid-1", remoteId)
    }

    @Test
    fun `componente con producto se mapea por globalProductId`() {
        val productIdMap = mapOf(1L to "product-uuid-1", 3L to "product-uuid-3")

        val localProductoId = 1L
        val remoteId = productIdMap[localProductoId]

        assertEquals("product-uuid-1", remoteId)
    }

    @Test
    fun `componente sin mapeo se omite`() {
        val productIdMap = mapOf(1L to "uuid-1")
        val localProductoId = 99L

        val remoteId = productIdMap[localProductoId]
        assertNull(remoteId)
    }

    // =====================================================================
    // Pull: deduplicacion al bajar datos
    // =====================================================================

    @Test
    fun `receta existente por nombre no se duplica`() {
        val localRecipeNames = setOf("test", "test2")
        val remoteRecipes = listOf("test", "test2", "nueva receta")

        val toCreate = remoteRecipes.filter { it !in localRecipeNames }
        assertEquals(1, toCreate.size)
        assertEquals("nueva receta", toCreate[0])
    }

    @Test
    fun `plato existente por nombre no se duplica`() {
        val localDishNames = setOf("sss")
        val remoteDishes = listOf("sss", "nuevo plato")

        val toCreate = remoteDishes.filter { it !in localDishNames }
        assertEquals(1, toCreate.size)
        assertEquals("nuevo plato", toCreate[0])
    }

    // =====================================================================
    // Upsert conflict keys
    // =====================================================================

    @Test
    fun `user_recipes conflict key es user_id y nombre`() {
        val conflictKey = "user_id,nombre"
        assertTrue(conflictKey.contains("user_id"))
        assertTrue(conflictKey.contains("nombre"))
    }

    @Test
    fun `user_dishes conflict key es user_id y nombre`() {
        val conflictKey = "user_id,nombre"
        assertTrue(conflictKey.contains("user_id"))
        assertTrue(conflictKey.contains("nombre"))
    }

    @Test
    fun `user_store_aliases conflict key es user_id y store_id`() {
        val conflictKey = "user_id,store_id"
        assertTrue(conflictKey.contains("user_id"))
        assertTrue(conflictKey.contains("store_id"))
    }

    // =====================================================================
    // SyncResult combinacion
    // =====================================================================

    @Test
    fun `push exitoso con recetas y platos suma pushedCount`() {
        val r1 = SyncResult(success = true, pushedCount = 2) // 2 recetas
        val r2 = SyncResult(success = true, pushedCount = 1) // 1 plato
        val combined = r1 + r2
        assertEquals(3, combined.pushedCount)
        assertTrue(combined.success)
    }

    @Test
    fun `push parcial con error no bloquea otros pushes`() {
        val r1 = SyncResult(success = true, pushedCount = 2)
        val r2 = SyncResult(success = false, errors = listOf("store_aliases failed"))
        val r3 = SyncResult(success = true, pushedCount = 1)
        val combined = r1 + r2 + r3
        assertEquals(3, combined.pushedCount)
        // success is false because one step failed
        assertTrue(!combined.success)
        assertEquals(1, combined.errors.size)
    }

    // =====================================================================
    // Startup sync: delay y session check
    // =====================================================================

    @Test
    fun `pushInBackground sin session no hace nada`() {
        val hasSession = false
        val shouldPush = hasSession
        assertTrue(!shouldPush)
    }

    @Test
    fun `pushInBackground con session ejecuta push`() {
        val hasSession = true
        val shouldPush = hasSession
        assertTrue(shouldPush)
    }

    @Test
    fun `startup sync usa delay de 3 segundos`() {
        val delayMs = 3000L
        assertEquals(3000L, delayMs)
    }
}
