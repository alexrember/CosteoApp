package com.mg.costeoapp.feature.inventario.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BloomreachDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `deserializa respuesta completa correctamente`() {
        val jsonString = """
            {
              "response": {
                "numFound": 1,
                "docs": [{
                  "pid": "328800",
                  "title": "Member's Selection Leche Semidescremada 12 Unidades / 1 L",
                  "brand": "Member's Selection",
                  "price_SV": 1649.0,
                  "thumb_image": "https://example.com/img.jpg",
                  "availability_SV": "true"
                }]
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<BloomreachSearchResponse>(jsonString)
        assertNotNull(result.response)
        assertEquals(1, result.response!!.numFound)
        assertEquals(1, result.response!!.docs!!.size)

        val product = result.response!!.docs!!.first()
        assertEquals("328800", product.pid)
        assertEquals("Member's Selection Leche Semidescremada 12 Unidades / 1 L", product.title)
        assertEquals("Member's Selection", product.brand)
        assertEquals(1649.0, product.priceSV!!, 0.01)
        assertEquals("https://example.com/img.jpg", product.thumbImage)
        assertEquals("true", product.availabilitySV)
    }

    @Test
    fun `deserializa con campos faltantes como null`() {
        val jsonString = """
            {
              "response": {
                "numFound": 1,
                "docs": [{
                  "pid": "123"
                }]
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<BloomreachSearchResponse>(jsonString)
        val product = result.response!!.docs!!.first()
        assertEquals("123", product.pid)
        assertNull(product.title)
        assertNull(product.brand)
        assertNull(product.priceSV)
        assertNull(product.thumbImage)
        assertNull(product.availabilitySV)
    }

    @Test
    fun `deserializa con docs vacio`() {
        val jsonString = """
            {
              "response": {
                "numFound": 0,
                "docs": []
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<BloomreachSearchResponse>(jsonString)
        assertNotNull(result.response)
        assertEquals(0, result.response!!.numFound)
        assertTrue(result.response!!.docs!!.isEmpty())
    }

    @Test
    fun `deserializa con response null`() {
        val jsonString = """{}""".trimIndent()

        val result = json.decodeFromString<BloomreachSearchResponse>(jsonString)
        assertNull(result.response)
    }

    @Test
    fun `deserializa con campos extra ignora unknown keys`() {
        val jsonString = """
            {
              "response": {
                "numFound": 1,
                "start": 0,
                "docs": [{
                  "pid": "456",
                  "title": "Arroz",
                  "unknown_field": "ignored",
                  "price_SV": 500.0
                }]
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<BloomreachSearchResponse>(jsonString)
        val product = result.response!!.docs!!.first()
        assertEquals("456", product.pid)
        assertEquals("Arroz", product.title)
        assertEquals(500.0, product.priceSV!!, 0.01)
    }

    @Test
    fun `deserializa multiples productos`() {
        val jsonString = """
            {
              "response": {
                "numFound": 2,
                "docs": [
                  {"pid": "1", "title": "Producto A", "price_SV": 100.0},
                  {"pid": "2", "title": "Producto B", "price_SV": 200.0}
                ]
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<BloomreachSearchResponse>(jsonString)
        assertEquals(2, result.response!!.docs!!.size)
        assertEquals("Producto A", result.response!!.docs!![0].title)
        assertEquals("Producto B", result.response!!.docs!![1].title)
    }
}
