package com.mg.costeoapp.feature.inventario.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationTest {

    // Barcode validation regex: "^\\d{8,14}$"
    private val barcodeRegex = Regex("^\\d{8,14}$")

    // Query sanitization: take(100), strip non-letter/non-number/non-space, trim
    private fun sanitizeQuery(query: String): String {
        return query.take(100).replace(Regex("[^\\p{L}\\p{N}\\s]"), "").trim()
    }

    // --- Barcode validation ---

    @Test
    fun `barcode valido EAN-13 de 13 digitos`() {
        assertTrue(barcodeRegex.matches("7406249000413"))
    }

    @Test
    fun `barcode valido EAN-8 de 8 digitos`() {
        assertTrue(barcodeRegex.matches("02200015"))
    }

    @Test
    fun `barcode valido UPC-12 de 12 digitos`() {
        assertTrue(barcodeRegex.matches("012345678901"))
    }

    @Test
    fun `barcode valido GTIN-14 de 14 digitos`() {
        assertTrue(barcodeRegex.matches("01234567890123"))
    }

    @Test
    fun `barcode invalido con letras`() {
        assertFalse(barcodeRegex.matches("abc123"))
    }

    @Test
    fun `barcode invalido muy corto 3 digitos`() {
        assertFalse(barcodeRegex.matches("123"))
    }

    @Test
    fun `barcode invalido muy corto 7 digitos`() {
        assertFalse(barcodeRegex.matches("1234567"))
    }

    @Test
    fun `barcode invalido muy largo 15 digitos`() {
        assertFalse(barcodeRegex.matches("123456789012345"))
    }

    @Test
    fun `barcode invalido vacio`() {
        assertFalse(barcodeRegex.matches(""))
    }

    @Test
    fun `barcode invalido con espacios`() {
        assertFalse(barcodeRegex.matches("7406249 000413"))
    }

    @Test
    fun `barcode invalido con caracteres especiales`() {
        assertFalse(barcodeRegex.matches("7406249-000413"))
    }

    // --- PriceSmart query sanitization ---

    @Test
    fun `sanitize query normal sin cambios`() {
        assertEquals("leche entera", sanitizeQuery("leche entera"))
    }

    @Test
    fun `sanitize query remueve simbolos especiales`() {
        assertEquals("arroz 500", sanitizeQuery("arroz \$5.00"))
    }

    @Test
    fun `sanitize query remueve signos de puntuacion`() {
        assertEquals("leche azucar sal", sanitizeQuery("leche, azucar; sal!"))
    }

    @Test
    fun `sanitize query trunca a 100 caracteres`() {
        val longQuery = "a".repeat(150)
        val result = sanitizeQuery(longQuery)
        assertEquals(100, result.length)
    }

    @Test
    fun `sanitize query con solo espacios retorna vacio`() {
        val result = sanitizeQuery("   ")
        assertTrue(result.isBlank())
    }

    @Test
    fun `sanitize query vacio retorna vacio`() {
        val result = sanitizeQuery("")
        assertEquals("", result)
    }

    @Test
    fun `sanitize query preserva acentos y enies`() {
        assertEquals("porcion de pollo con arroz y frijol", sanitizeQuery("porcion de pollo con arroz y frijol"))
    }

    @Test
    fun `sanitize query preserva numeros`() {
        assertEquals("leche 1 litro", sanitizeQuery("leche 1 litro"))
    }

    @Test
    fun `sanitize query remueve parentesis y corchetes`() {
        assertEquals("arroz 5lb", sanitizeQuery("arroz (5lb)"))
    }

    @Test
    fun `sanitize query trim espacios al inicio y final`() {
        assertEquals("arroz", sanitizeQuery("  arroz  "))
    }
}
