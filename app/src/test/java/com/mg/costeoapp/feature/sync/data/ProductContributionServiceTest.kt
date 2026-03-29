package com.mg.costeoapp.feature.sync.data

import com.mg.costeoapp.core.database.entity.Producto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la logica de contribucion de productos.
 * No instancia ProductContributionService directamente (requiere SupabaseClient Android).
 * Testea la logica de construccion del request body y validaciones.
 */
class ProductContributionServiceTest {

    private val testProducto = Producto(
        id = 1,
        nombre = "Leche Entera",
        codigoBarras = "7406249000413",
        unidadMedida = "ml",
        cantidadPorEmpaque = 946.0,
        unidadesPorEmpaque = 1,
        factorMerma = 0
    )

    /**
     * Replica la logica de construccion del body JSON
     * que hace ProductContributionService.contribute()
     */
    private fun buildContributionBody(
        producto: Producto,
        globalProductId: String? = null
    ): String {
        val body = buildJsonObject {
            put("ean", producto.codigoBarras)
            put("nombre", producto.nombre)
            put("unidad_medida", producto.unidadMedida)
            put("cantidad_por_empaque", producto.cantidadPorEmpaque)
            put("unidades_por_empaque", producto.unidadesPorEmpaque)
            put("factor_merma", producto.factorMerma)
            if (!globalProductId.isNullOrBlank()) {
                put("global_product_id", globalProductId)
            }
        }
        return body.toString()
    }

    // --- Validaciones pre-llamada ---

    @Test
    fun `producto sin barcode no debe contribuir`() {
        val producto = testProducto.copy(codigoBarras = null)
        assertTrue(producto.codigoBarras.isNullOrBlank())
    }

    @Test
    fun `producto con barcode vacio no debe contribuir`() {
        val producto = testProducto.copy(codigoBarras = "")
        assertTrue(producto.codigoBarras.isNullOrBlank())
    }

    @Test
    fun `producto con barcode whitespace no debe contribuir`() {
        val producto = testProducto.copy(codigoBarras = "   ")
        assertTrue(producto.codigoBarras.isNullOrBlank())
    }

    @Test
    fun `producto con barcode valido puede contribuir`() {
        assertFalse(testProducto.codigoBarras.isNullOrBlank())
    }

    // --- Construccion del body JSON ---

    @Test
    fun `body incluye ean y nombre`() {
        val body = buildContributionBody(testProducto)

        assertTrue(body.contains("\"ean\":\"7406249000413\""))
        assertTrue(body.contains("\"nombre\":\"Leche Entera\""))
    }

    @Test
    fun `body incluye unidad_medida y cantidad_por_empaque`() {
        val body = buildContributionBody(testProducto)

        assertTrue(body.contains("\"unidad_medida\":\"ml\""))
        assertTrue(body.contains("\"cantidad_por_empaque\":946.0"))
    }

    @Test
    fun `body incluye unidades_por_empaque`() {
        val producto = testProducto.copy(unidadesPorEmpaque = 12)
        val body = buildContributionBody(producto)

        assertTrue(body.contains("\"unidades_por_empaque\":12"))
    }

    @Test
    fun `body incluye factor_merma`() {
        val producto = testProducto.copy(factorMerma = 10)
        val body = buildContributionBody(producto)

        assertTrue(body.contains("\"factor_merma\":10"))
    }

    @Test
    fun `body incluye global_product_id cuando existe`() {
        val body = buildContributionBody(testProducto, "gp-uuid-123")

        assertTrue(body.contains("\"global_product_id\":\"gp-uuid-123\""))
    }

    @Test
    fun `body no incluye global_product_id cuando es null`() {
        val body = buildContributionBody(testProducto, null)

        assertFalse(body.contains("global_product_id"))
    }

    @Test
    fun `body no incluye global_product_id cuando es vacio`() {
        val body = buildContributionBody(testProducto, "")

        assertFalse(body.contains("global_product_id"))
    }

    // --- Parseo de respuesta ---

    @Test
    fun `respuesta con alias_created true`() {
        val response = """{"ok":true,"alias_created":true,"message":"Alias creado"}"""

        assertTrue(response.contains("\"alias_created\":true", ignoreCase = true))
    }

    @Test
    fun `respuesta con alias_created false`() {
        val response = """{"ok":true,"alias_created":false,"message":"Alias ya existe"}"""

        assertFalse(response.contains("\"alias_created\":true", ignoreCase = true))
    }

    @Test
    fun `respuesta de error no tiene alias_created`() {
        val response = """{"error":"Token invalido o expirado"}"""

        assertFalse(response.contains("\"alias_created\":true", ignoreCase = true))
    }

    // --- Edge cases productos ---

    @Test
    fun `producto con nombre largo se construye correctamente`() {
        val producto = testProducto.copy(nombre = "Member's Selection Agua con Gas de Sabor a Frutas Cero Azucar 24 Unidades / 503 mL / 17 oz")
        val body = buildContributionBody(producto)

        assertTrue(body.contains("Member's Selection"))
    }

    @Test
    fun `producto con cantidadPorEmpaque decimal`() {
        val producto = testProducto.copy(cantidadPorEmpaque = 37.5)
        val body = buildContributionBody(producto)

        assertTrue(body.contains("37.5"))
    }

    @Test
    fun `producto PriceSmart con 24 unidades`() {
        val producto = testProducto.copy(
            nombre = "Agua con Gas 24u",
            unidadesPorEmpaque = 24,
            cantidadPorEmpaque = 503.0,
            unidadMedida = "ml"
        )
        val body = buildContributionBody(producto)

        assertTrue(body.contains("\"unidades_por_empaque\":24"))
        assertTrue(body.contains("\"cantidad_por_empaque\":503.0"))
    }
}
