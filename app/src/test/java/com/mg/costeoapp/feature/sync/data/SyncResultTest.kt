package com.mg.costeoapp.feature.sync.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncResultTest {

    @Test
    fun `resultado exitoso por defecto`() {
        val result = SyncResult(success = true)
        assertTrue(result.success)
        assertEquals(0, result.pushedCount)
        assertEquals(0, result.pulledCount)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `combinar dos resultados exitosos suma conteos`() {
        val a = SyncResult(success = true, pushedCount = 3, pulledCount = 5)
        val b = SyncResult(success = true, pushedCount = 2, pulledCount = 1)
        val combined = a + b
        assertTrue(combined.success)
        assertEquals(5, combined.pushedCount)
        assertEquals(6, combined.pulledCount)
        assertTrue(combined.errors.isEmpty())
    }

    @Test
    fun `combinar exito con fallo resulta en fallo`() {
        val success = SyncResult(success = true, pushedCount = 3, pulledCount = 2)
        val failure = SyncResult(success = false, pushedCount = 0, pulledCount = 0, errors = listOf("Error de red"))
        val combined = success + failure
        assertFalse(combined.success)
        assertEquals(3, combined.pushedCount)
        assertEquals(2, combined.pulledCount)
        assertEquals(listOf("Error de red"), combined.errors)
    }

    @Test
    fun `combinar fallo con exito resulta en fallo`() {
        val failure = SyncResult(success = false, errors = listOf("timeout"))
        val success = SyncResult(success = true, pushedCount = 1, pulledCount = 1)
        val combined = failure + success
        assertFalse(combined.success)
    }

    @Test
    fun `combinar dos fallos concatena errores`() {
        val a = SyncResult(success = false, errors = listOf("Error A"))
        val b = SyncResult(success = false, errors = listOf("Error B"))
        val combined = a + b
        assertFalse(combined.success)
        assertEquals(listOf("Error A", "Error B"), combined.errors)
    }

    @Test
    fun `resultado vacio combinado no cambia valores`() {
        val empty = SyncResult(success = true, pushedCount = 0, pulledCount = 0)
        val real = SyncResult(success = true, pushedCount = 10, pulledCount = 5, errors = emptyList())
        val combined = empty + real
        assertTrue(combined.success)
        assertEquals(10, combined.pushedCount)
        assertEquals(5, combined.pulledCount)
    }

    @Test
    fun `combinar multiples resultados con fold`() {
        val results = listOf(
            SyncResult(success = true, pushedCount = 1, pulledCount = 2),
            SyncResult(success = true, pushedCount = 3, pulledCount = 4),
            SyncResult(success = true, pushedCount = 5, pulledCount = 6)
        )
        val total = results.reduce { acc, r -> acc + r }
        assertTrue(total.success)
        assertEquals(9, total.pushedCount)
        assertEquals(12, total.pulledCount)
    }

    @Test
    fun `un fallo en cadena de fold hace fallar todo`() {
        val results = listOf(
            SyncResult(success = true, pushedCount = 1),
            SyncResult(success = false, errors = listOf("fallo")),
            SyncResult(success = true, pushedCount = 2)
        )
        val total = results.reduce { acc, r -> acc + r }
        assertFalse(total.success)
        assertEquals(3, total.pushedCount)
        assertEquals(listOf("fallo"), total.errors)
    }
}
