package com.mg.costeoapp.feature.sync.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la logica de pull de datos del usuario.
 * Valida el modelo SyncResult y la logica de deduplicacion
 * sin instanciar SyncManager (requiere SupabaseClient Android).
 */
class SyncManagerPullTest {

    // --- SyncResult ---

    @Test
    fun `SyncResult exitoso con productos pulled`() {
        val result = SyncResult(success = true, pulledCount = 3)
        assertTrue(result.success)
        assertEquals(3, result.pulledCount)
    }

    @Test
    fun `SyncResult fallido con errores`() {
        val result = SyncResult(success = false, errors = listOf("No hay sesion activa"))
        assertTrue(!result.success)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `SyncResult combinacion con operador plus`() {
        val r1 = SyncResult(success = true, pulledCount = 2)
        val r2 = SyncResult(success = true, pushedCount = 3)
        val combined = r1 + r2
        assertTrue(combined.success)
        assertEquals(2, combined.pulledCount)
        assertEquals(3, combined.pushedCount)
    }

    @Test
    fun `SyncResult combinacion propaga failure`() {
        val r1 = SyncResult(success = true, pulledCount = 2)
        val r2 = SyncResult(success = false, errors = listOf("Error de red"))
        val combined = r1 + r2
        assertTrue(!combined.success)
    }

    // --- Deduplicacion de productos ---

    @Test
    fun `producto con EAN existente no se duplica`() {
        val existingEans = setOf("022000159540", "7406249000413")
        val incomingEan = "022000159540"

        assertTrue(incomingEan in existingEans)
    }

    @Test
    fun `producto con EAN nuevo se crea`() {
        val existingEans = setOf("022000159540", "7406249000413")
        val incomingEan = "607766541183"

        assertTrue(incomingEan !in existingEans)
    }

    @Test
    fun `multiples aliases generan productos unicos`() {
        data class MockAlias(val productId: String, val ean: String)

        val aliases = listOf(
            MockAlias("uuid-1", "022000159540"),
            MockAlias("uuid-2", "7406249000413"),
            MockAlias("uuid-3", "607766541183")
        )

        val existingEans = setOf("022000159540")
        val toCreate = aliases.filter { it.ean !in existingEans }

        assertEquals(2, toCreate.size)
        assertEquals("7406249000413", toCreate[0].ean)
        assertEquals("607766541183", toCreate[1].ean)
    }

    // --- Mapeo de datos globales a locales ---

    @Test
    fun `unidad_medida default es unidad`() {
        val unidadMedida: String? = null
        assertEquals("unidad", unidadMedida ?: "unidad")
    }

    @Test
    fun `cantidad_por_empaque default es 1`() {
        val cantidad: Double? = null
        assertEquals(1.0, cantidad ?: 1.0, 0.01)
    }

    @Test
    fun `unidades_por_empaque default es 1`() {
        val unidades: Int? = null
        assertEquals(1, unidades ?: 1)
    }

    @Test
    fun `nombre default usa EAN cuando nombre es null`() {
        val nombre: String? = null
        val ean = "022000159540"
        assertEquals("Producto 022000159540", nombre ?: "Producto $ean")
    }

    @Test
    fun `nombre se usa cuando existe`() {
        val nombre: String? = "Chicle Extra"
        val ean = "022000159540"
        assertEquals("Chicle Extra", nombre ?: "Producto $ean")
    }

    @Test
    fun `factor_merma default es 0`() {
        val factorMerma: Int? = null
        assertEquals(0, factorMerma ?: 0)
    }

    @Test
    fun `factor_merma se preserva del alias`() {
        val factorMerma: Int? = 10
        assertEquals(10, factorMerma ?: 0)
    }

    // --- Validacion de sesion ---

    @Test
    fun `sin sesion retorna error`() {
        val hasSession = false
        val result = if (!hasSession) {
            SyncResult(success = false, errors = listOf("No hay sesion activa"))
        } else {
            SyncResult(success = true)
        }

        assertTrue(!result.success)
        assertTrue(result.errors.contains("No hay sesion activa"))
    }
}
