package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.core.util.UnidadMedida
import com.mg.costeoapp.feature.inventario.data.mapper.parseContenidoFromName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentParserTest {

    @Test
    fun `parsea mililitros de nombre`() {
        val result = parseContenidoFromName("Leche Entera La Salud 946ml")
        assertNotNull(result)
        assertEquals(946.0, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.MILILITRO, result.unidad)
    }

    @Test
    fun `parsea litros de nombre`() {
        val result = parseContenidoFromName("Aceite Orisol 1L")
        assertNotNull(result)
        assertEquals(1.0, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.LITRO, result.unidad)
    }

    @Test
    fun `parsea kilogramos de nombre`() {
        val result = parseContenidoFromName("Azucar Cana Real 2.5kg")
        assertNotNull(result)
        assertEquals(2.5, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.KILOGRAMO, result.unidad)
    }

    @Test
    fun `parsea gramos de nombre`() {
        val result = parseContenidoFromName("Pasta Doria 500g")
        assertNotNull(result)
        assertEquals(500.0, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.GRAMO, result.unidad)
    }

    @Test
    fun `parsea libras de nombre`() {
        val result = parseContenidoFromName("Arroz Gallo Rojo 5lb")
        assertNotNull(result)
        assertEquals(5.0, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.LIBRA, result.unidad)
    }

    @Test
    fun `parsea onzas de nombre`() {
        val result = parseContenidoFromName("Cereal Kelloggs 12oz")
        assertNotNull(result)
        assertEquals(12.0, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.ONZA, result.unidad)
    }

    @Test
    fun `parsea con espacios entre numero y unidad`() {
        val result = parseContenidoFromName("Jugo Del Valle 500 ml")
        assertNotNull(result)
        assertEquals(500.0, result!!.cantidad, 0.01)
        assertEquals(UnidadMedida.MILILITRO, result.unidad)
    }

    @Test
    fun `retorna null si no encuentra unidad`() {
        val result = parseContenidoFromName("Coca Cola Regular")
        assertNull(result)
    }

    @Test
    fun `ml tiene prioridad sobre l en nombres ambiguos`() {
        // "946ml" debe matchear ml, no l
        val result = parseContenidoFromName("Leche 946ml entera")
        assertNotNull(result)
        assertEquals(UnidadMedida.MILILITRO, result!!.unidad)
    }
}
