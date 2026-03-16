package com.mg.costeoapp.core.domain.model

import com.mg.costeoapp.core.util.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDataMergerTest {

    @Test
    fun `sin fuentes retorna todo vacio`() {
        val merged = ProductDataMerger.merge(emptyList())
        assertTrue(merged.nombre is FieldResolution.Empty)
        assertTrue(merged.precio is FieldResolution.Empty)
    }

    @Test
    fun `una fuente resuelve todo directamente`() {
        val source = ProductDataSource(
            sourceName = "Walmart SV",
            nombre = "Leche 946ml",
            precio = 199,
            unidadMedida = UnidadMedida.MILILITRO,
            cantidadPorEmpaque = 946.0
        )
        val merged = ProductDataMerger.merge(listOf(source))

        val nombre = merged.nombre as FieldResolution.Resolved
        assertEquals("Leche 946ml", nombre.value)
        assertEquals("Walmart SV", nombre.source)

        val precio = merged.precio as FieldResolution.Resolved
        assertEquals(199L, precio.value)
    }

    @Test
    fun `dos fuentes con mismo nombre se resuelve`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", nombre = "Leche Dos Pinos"),
            ProductDataSource(sourceName = "Open Food Facts", nombre = "Leche Dos Pinos")
        )
        val merged = ProductDataMerger.merge(sources)
        assertTrue("nombres iguales → Resolved", merged.nombre is FieldResolution.Resolved)
    }

    @Test
    fun `dos fuentes con mismo nombre case insensitive se resuelve`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", nombre = "LECHE DOS PINOS"),
            ProductDataSource(sourceName = "OFF", nombre = "leche dos pinos")
        )
        val merged = ProductDataMerger.merge(sources)
        assertTrue("case insensitive → Resolved", merged.nombre is FieldResolution.Resolved)
    }

    @Test
    fun `dos fuentes con nombres diferentes genera conflicto`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", nombre = "Leche Dos Pinos Delactomy Semidescremada"),
            ProductDataSource(sourceName = "OFF", nombre = "Delactomy")
        )
        val merged = ProductDataMerger.merge(sources)
        val nombre = merged.nombre
        assertTrue("nombres diferentes → Conflict", nombre is FieldResolution.Conflict)

        val conflict = nombre as FieldResolution.Conflict
        assertEquals(2, conflict.options.size)
        assertEquals("Walmart", conflict.options[0].source)
        assertEquals("OFF", conflict.options[1].source)
    }

    @Test
    fun `precio solo de una fuente se resuelve`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", precio = 199),
            ProductDataSource(sourceName = "OFF") // OFF no tiene precio
        )
        val merged = ProductDataMerger.merge(sources)
        val precio = merged.precio as FieldResolution.Resolved
        assertEquals(199L, precio.value)
        assertEquals("Walmart", precio.source)
    }

    @Test
    fun `contenido de diferentes fuentes con conflicto`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", unidadMedida = UnidadMedida.GRAMO, cantidadPorEmpaque = 55.0),
            ProductDataSource(sourceName = "OFF", unidadMedida = UnidadMedida.GRAMO, cantidadPorEmpaque = 57.0)
        )
        val merged = ProductDataMerger.merge(sources)
        assertTrue("cantidades diferentes → Conflict", merged.cantidadPorEmpaque is FieldResolution.Conflict)
    }

    @Test
    fun `contenido igual de ambas fuentes se resuelve`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", cantidadPorEmpaque = 946.0),
            ProductDataSource(sourceName = "OFF", cantidadPorEmpaque = 946.0)
        )
        val merged = ProductDataMerger.merge(sources)
        assertTrue("cantidades iguales → Resolved", merged.cantidadPorEmpaque is FieldResolution.Resolved)
    }

    @Test
    fun `sources lista las fuentes usadas`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart SV", nombre = "A"),
            ProductDataSource(sourceName = "Open Food Facts", nombre = "B"),
        )
        val merged = ProductDataMerger.merge(sources)
        assertEquals(listOf("Walmart SV", "Open Food Facts"), merged.sources)
    }

    @Test
    fun `fuentes con todos los campos null resulta todo Empty`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart"),
            ProductDataSource(sourceName = "OFF")
        )
        val merged = ProductDataMerger.merge(sources)

        assertTrue("nombre → Empty", merged.nombre is FieldResolution.Empty)
        assertTrue("precio → Empty", merged.precio is FieldResolution.Empty)
        assertTrue("unidadMedida → Empty", merged.unidadMedida is FieldResolution.Empty)
        assertTrue("cantidadPorEmpaque → Empty", merged.cantidadPorEmpaque is FieldResolution.Empty)
        assertTrue("unidadesPorEmpaque → Empty", merged.unidadesPorEmpaque is FieldResolution.Empty)
        assertEquals(listOf("Walmart", "OFF"), merged.sources)
    }

    @Test
    fun `una fuente con todos los campos null resulta todo Empty`() {
        val sources = listOf(ProductDataSource(sourceName = "Solo"))
        val merged = ProductDataMerger.merge(sources)

        assertTrue(merged.nombre is FieldResolution.Empty)
        assertTrue(merged.precio is FieldResolution.Empty)
        assertTrue(merged.unidadMedida is FieldResolution.Empty)
        assertTrue(merged.cantidadPorEmpaque is FieldResolution.Empty)
        assertTrue(merged.unidadesPorEmpaque is FieldResolution.Empty)
        assertEquals(listOf("Solo"), merged.sources)
    }

    @Test
    fun `unidadesPorEmpaque iguales se resuelve`() {
        val sources = listOf(
            ProductDataSource(sourceName = "A", unidadesPorEmpaque = 6),
            ProductDataSource(sourceName = "B", unidadesPorEmpaque = 6)
        )
        val merged = ProductDataMerger.merge(sources)
        val field = merged.unidadesPorEmpaque as FieldResolution.Resolved
        assertEquals(6, field.value)
    }

    @Test
    fun `unidadesPorEmpaque diferentes genera conflicto`() {
        val sources = listOf(
            ProductDataSource(sourceName = "A", unidadesPorEmpaque = 6),
            ProductDataSource(sourceName = "B", unidadesPorEmpaque = 12)
        )
        val merged = ProductDataMerger.merge(sources)
        assertTrue(merged.unidadesPorEmpaque is FieldResolution.Conflict)
    }

    @Test
    fun `futuro - tres fuentes con conflicto parcial`() {
        val sources = listOf(
            ProductDataSource(sourceName = "Walmart", nombre = "Leche A", precio = 199),
            ProductDataSource(sourceName = "PriceSmart", nombre = "Leche A", precio = 185),
            ProductDataSource(sourceName = "OFF", nombre = "Delactomy")
        )
        val merged = ProductDataMerger.merge(sources)

        // Nombre: Walmart y PriceSmart coinciden, OFF diferente → 2 opciones
        val nombre = merged.nombre as FieldResolution.Conflict
        assertEquals(2, nombre.options.size) // "Leche A" dedup + "Delactomy"

        // Precio: diferentes → conflicto
        val precio = merged.precio as FieldResolution.Conflict
        assertEquals(2, precio.options.size)
        assertEquals(199L, precio.options[0].value)
        assertEquals(185L, precio.options[1].value)
    }
}
