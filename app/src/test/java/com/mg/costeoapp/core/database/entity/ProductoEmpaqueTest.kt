package com.mg.costeoapp.core.database.entity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests para validar los 3 escenarios de empaque:
 * 1. Pack de 12 leches de 946ml cada una
 * 2. Leche individual de 946ml
 * 3. Multipack de 6 items de 946ml
 */
class ProductoEmpaqueTest {

    @Test
    fun `escenario 1 - pack de 12 leches`() {
        val producto = Producto(
            nombre = "Pack Leche Dos Pinos 12 unidades",
            unidadMedida = "ml",
            cantidadPorEmpaque = 946.0,
            unidadesPorEmpaque = 12
        )

        assertEquals(946.0, producto.cantidadPorEmpaque, 0.01)
        assertEquals(12, producto.unidadesPorEmpaque)
        // Contenido total = 946 * 12 = 11,352 ml
        val contenidoTotal = producto.cantidadPorEmpaque * producto.unidadesPorEmpaque
        assertEquals(11352.0, contenidoTotal, 0.01)
    }

    @Test
    fun `escenario 2 - leche individual`() {
        val producto = Producto(
            nombre = "Leche Dos Pinos 946ml",
            unidadMedida = "ml",
            cantidadPorEmpaque = 946.0,
            unidadesPorEmpaque = 1
        )

        assertEquals(946.0, producto.cantidadPorEmpaque, 0.01)
        assertEquals(1, producto.unidadesPorEmpaque)
        val contenidoTotal = producto.cantidadPorEmpaque * producto.unidadesPorEmpaque
        assertEquals(946.0, contenidoTotal, 0.01)
    }

    @Test
    fun `escenario 3 - multipack de 6`() {
        val producto = Producto(
            nombre = "Sixpack Leche Dos Pinos",
            unidadMedida = "ml",
            cantidadPorEmpaque = 946.0,
            unidadesPorEmpaque = 6
        )

        assertEquals(946.0, producto.cantidadPorEmpaque, 0.01)
        assertEquals(6, producto.unidadesPorEmpaque)
        val contenidoTotal = producto.cantidadPorEmpaque * producto.unidadesPorEmpaque
        assertEquals(5676.0, contenidoTotal, 0.01)
    }

    @Test
    fun `costo por unidad individual`() {
        val producto = Producto(
            nombre = "Pack Leche 12 unidades",
            unidadMedida = "ml",
            cantidadPorEmpaque = 946.0,
            unidadesPorEmpaque = 12
        )
        val precioEmpaque = 1800L // $18.00 centavos

        val costoPorUnidad = precioEmpaque / producto.unidadesPorEmpaque
        assertEquals(150L, costoPorUnidad) // $1.50 por caja

        val costoPorMl = precioEmpaque.toDouble() / (producto.unidadesPorEmpaque * producto.cantidadPorEmpaque)
        assertEquals(0.1586, costoPorMl, 0.001) // ~$0.0016 por ml en centavos
    }

    @Test
    fun `default unidadesPorEmpaque es 1`() {
        val producto = Producto(
            nombre = "Producto simple",
            unidadMedida = "lb",
            cantidadPorEmpaque = 1.0
        )
        assertEquals(1, producto.unidadesPorEmpaque)
    }
}
