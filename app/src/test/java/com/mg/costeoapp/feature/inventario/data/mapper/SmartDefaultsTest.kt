package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.core.util.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartDefaultsTest {

    @Test
    fun `translateForSearch traduce sugarfree gum`() {
        val result = SmartDefaults.translateForSearch("Sugarfree gum")
        assertEquals("sin azucar chicle", result)
    }

    @Test
    fun `translateForSearch traduce milk chocolate`() {
        val result = SmartDefaults.translateForSearch("milk chocolate")
        assertEquals("leche chocolate", result)
    }

    @Test
    fun `translateForSearch no traduce palabras en espanol`() {
        val result = SmartDefaults.translateForSearch("Arroz Diana")
        assertEquals("arroz diana", result)
    }

    @Test
    fun `translateForSearch traduce sugar free con espacio`() {
        // Map iterates in insertion order: "gum"->"chicle" first, then "sugar free"->"sin azucar"
        // "sugar" entry comes later but by then "sugar" is already gone from the string
        val result = SmartDefaults.translateForSearch("sugar free gum")
        assertEquals("sin azucar chicle", result)
    }

    @Test
    fun `translateForSearch traduce multiples palabras`() {
        val result = SmartDefaults.translateForSearch("milk and cheese")
        assertEquals("leche and queso", result)
    }

    @Test
    fun `translateForSearch convierte a minusculas`() {
        val result = SmartDefaults.translateForSearch("MILK")
        assertEquals("leche", result)
    }

    @Test
    fun `translateForSearch con texto vacio retorna vacio`() {
        val result = SmartDefaults.translateForSearch("")
        assertEquals("", result)
    }

    @Test
    fun `suggestUnit leche retorna MILILITRO`() {
        assertEquals(UnidadMedida.MILILITRO, SmartDefaults.suggestUnit("leche entera"))
    }

    @Test
    fun `suggestUnit jugo retorna MILILITRO`() {
        assertEquals(UnidadMedida.MILILITRO, SmartDefaults.suggestUnit("jugo de naranja"))
    }

    @Test
    fun `suggestUnit aceite retorna MILILITRO`() {
        assertEquals(UnidadMedida.MILILITRO, SmartDefaults.suggestUnit("aceite vegetal"))
    }

    @Test
    fun `suggestUnit arroz retorna LIBRA`() {
        assertEquals(UnidadMedida.LIBRA, SmartDefaults.suggestUnit("arroz diana"))
    }

    @Test
    fun `suggestUnit frijol retorna LIBRA`() {
        assertEquals(UnidadMedida.LIBRA, SmartDefaults.suggestUnit("frijoles rojos"))
    }

    @Test
    fun `suggestUnit sal retorna GRAMO`() {
        assertEquals(UnidadMedida.GRAMO, SmartDefaults.suggestUnit("sal marina"))
    }

    @Test
    fun `suggestUnit cafe retorna GRAMO`() {
        assertEquals(UnidadMedida.GRAMO, SmartDefaults.suggestUnit("cafe molido"))
    }

    @Test
    fun `suggestUnit producto generico retorna UNIDAD`() {
        assertEquals(UnidadMedida.UNIDAD, SmartDefaults.suggestUnit("producto generico"))
    }

    @Test
    fun `suggestUnit es case insensitive`() {
        assertEquals(UnidadMedida.MILILITRO, SmartDefaults.suggestUnit("LECHE ENTERA"))
    }

    @Test
    fun `suggestUnit refresco retorna MILILITRO`() {
        assertEquals(UnidadMedida.MILILITRO, SmartDefaults.suggestUnit("refresco cola"))
    }

    @Test
    fun `suggestUnit carne retorna LIBRA`() {
        assertEquals(UnidadMedida.LIBRA, SmartDefaults.suggestUnit("carne molida"))
    }

    @Test
    fun `suggestUnit pasta retorna GRAMO`() {
        assertEquals(UnidadMedida.GRAMO, SmartDefaults.suggestUnit("pasta espagueti"))
    }
}
