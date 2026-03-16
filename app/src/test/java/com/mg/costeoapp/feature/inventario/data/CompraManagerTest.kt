package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.feature.inventario.ui.CarritoItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de logica del carrito (calculos de subtotal/total).
 * CompraManager requiere DAOs (Room), asi que estos tests verifican
 * la logica de CarritoItem y los calculos de precios.
 */
class CompraManagerTest {

    private val leche = Producto(
        id = 1, nombre = "Leche 946ml",
        unidadMedida = "ml", cantidadPorEmpaque = 946.0, unidadesPorEmpaque = 1
    )
    private val sopa = Producto(
        id = 2, nombre = "Sopa Maggi 55g",
        unidadMedida = "g", cantidadPorEmpaque = 55.0, unidadesPorEmpaque = 1
    )
    private val arroz = Producto(
        id = 3, nombre = "Arroz 5lb",
        unidadMedida = "lb", cantidadPorEmpaque = 5.0, unidadesPorEmpaque = 1
    )
    private val packLeche = Producto(
        id = 4, nombre = "Pack Leche 12u",
        unidadMedida = "ml", cantidadPorEmpaque = 946.0, unidadesPorEmpaque = 12
    )
    private val manzanas = Producto(
        id = 5, nombre = "Bolsa Manzanas",
        unidadMedida = "unidad", cantidadPorEmpaque = 1.0, unidadesPorEmpaque = 10
    )

    @Test
    fun `subtotal 1 item`() {
        val item = CarritoItem(producto = leche, cantidad = 1.0, precioUnitario = 199)
        assertEquals(199L, item.subtotal)
    }

    @Test
    fun `subtotal 2 items`() {
        val item = CarritoItem(producto = leche, cantidad = 2.0, precioUnitario = 199)
        assertEquals(398L, item.subtotal)
    }

    @Test
    fun `subtotal 3 sopas`() {
        val item = CarritoItem(producto = sopa, cantidad = 3.0, precioUnitario = 39)
        assertEquals(117L, item.subtotal)
    }

    @Test
    fun `subtotal pack de leche`() {
        val item = CarritoItem(producto = packLeche, cantidad = 1.0, precioUnitario = 1800)
        assertEquals(1800L, item.subtotal)
        assertEquals(12, item.producto.unidadesPorEmpaque)
        val costoPorUnidad = item.precioUnitario / item.producto.unidadesPorEmpaque
        assertEquals(150L, costoPorUnidad)
    }

    @Test
    fun `subtotal manzanas`() {
        val item = CarritoItem(producto = manzanas, cantidad = 1.0, precioUnitario = 500)
        assertEquals(500L, item.subtotal)
        val costoPorManzana = item.precioUnitario / item.producto.unidadesPorEmpaque
        assertEquals(50L, costoPorManzana)
    }

    @Test
    fun `subtotal con cantidad fraccionaria redondea correctamente`() {
        // 1.5 x $1.99 = $2.985 → redondea a $2.99 (299 centavos)
        val item = CarritoItem(producto = leche, cantidad = 1.5, precioUnitario = 199)
        assertEquals(299L, item.subtotal) // round(199 * 1.5) = round(298.5) = 299
    }

    @Test
    fun `total de compra completa`() {
        val items = listOf(
            CarritoItem(producto = leche, cantidad = 2.0, precioUnitario = 199),   // 398
            CarritoItem(producto = sopa, cantidad = 3.0, precioUnitario = 39),     // 117
            CarritoItem(producto = arroz, cantidad = 1.0, precioUnitario = 350),   // 350
            CarritoItem(producto = packLeche, cantidad = 1.0, precioUnitario = 1800), // 1800
            CarritoItem(producto = manzanas, cantidad = 1.0, precioUnitario = 500)    // 500
        )
        val total = items.sumOf { it.subtotal }
        assertEquals(3165L, total) // $31.65
    }
}
