package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests del carrito de compras (CompraManager).
 * Valida cantidades, precios, subtotales y totales con multiples productos.
 */
class CompraManagerTest {

    private lateinit var manager: CompraManager
    private val tienda = Tienda(id = 1, nombre = "Super Selectos")

    // Productos de ejemplo
    private val leche = Producto(
        id = 1, nombre = "Leche Dos Pinos 946ml",
        unidadMedida = "ml", cantidadPorEmpaque = 946.0, unidadesPorEmpaque = 1
    )
    private val sopa = Producto(
        id = 2, nombre = "Sopa Maggi 55g",
        unidadMedida = "g", cantidadPorEmpaque = 55.0, unidadesPorEmpaque = 1
    )
    private val arroz = Producto(
        id = 3, nombre = "Arroz Gallo Rojo 5lb",
        unidadMedida = "lb", cantidadPorEmpaque = 5.0, unidadesPorEmpaque = 1
    )
    private val packLeche = Producto(
        id = 4, nombre = "Pack Leche 12 unidades",
        unidadMedida = "ml", cantidadPorEmpaque = 946.0, unidadesPorEmpaque = 12
    )
    private val manzanas = Producto(
        id = 5, nombre = "Bolsa Manzanas",
        unidadMedida = "unidad", cantidadPorEmpaque = 1.0, unidadesPorEmpaque = 10
    )

    @Before
    fun setup() {
        manager = CompraManager()
        manager.iniciarCompra(tienda)
    }

    @Test
    fun `carrito inicia vacio`() {
        assertEquals(0, manager.itemCount)
        assertEquals(emptyList<Any>(), manager.getItems())
    }

    @Test
    fun `agregar un producto`() {
        manager.agregarProducto(leche, 1.0, 199) // $1.99

        assertEquals(1, manager.itemCount)
        val item = manager.getItems()[0]
        assertEquals("Leche Dos Pinos 946ml", item.producto.nombre)
        assertEquals(1.0, item.cantidad, 0.01)
        assertEquals(199L, item.precioUnitario)
        assertEquals(199L, item.subtotal) // 1 x $1.99 = $1.99
    }

    @Test
    fun `agregar mismo producto dos veces aumenta cantidad`() {
        manager.agregarProducto(leche, 1.0, 199)
        manager.agregarProducto(leche, 1.0, 199)

        assertEquals(1, manager.itemCount) // sigue siendo 1 item
        val item = manager.getItems()[0]
        assertEquals(2.0, item.cantidad, 0.01) // cantidad = 2
        assertEquals(398L, item.subtotal) // 2 x $1.99 = $3.98
    }

    @Test
    fun `agregar multiples productos diferentes`() {
        manager.agregarProducto(leche, 1.0, 199)  // $1.99
        manager.agregarProducto(sopa, 1.0, 39)     // $0.39
        manager.agregarProducto(arroz, 1.0, 350)   // $3.50

        assertEquals(3, manager.itemCount)

        val items = manager.getItems()
        assertEquals(199L, items[0].subtotal)  // leche
        assertEquals(39L, items[1].subtotal)   // sopa
        assertEquals(350L, items[2].subtotal)  // arroz

        val total = items.sumOf { it.subtotal }
        assertEquals(588L, total) // $5.88
    }

    @Test
    fun `agregar 3 leches individuales`() {
        manager.agregarProducto(leche, 1.0, 199)
        manager.agregarProducto(leche, 1.0, 199)
        manager.agregarProducto(leche, 1.0, 199)

        assertEquals(1, manager.itemCount)
        val item = manager.getItems()[0]
        assertEquals(3.0, item.cantidad, 0.01)
        assertEquals(597L, item.subtotal) // 3 x $1.99 = $5.97
    }

    @Test
    fun `pack de 12 leches es 1 item con precio del pack`() {
        manager.agregarProducto(packLeche, 1.0, 1800) // $18.00 el pack

        assertEquals(1, manager.itemCount)
        val item = manager.getItems()[0]
        assertEquals(1.0, item.cantidad, 0.01)
        assertEquals(1800L, item.subtotal) // $18.00
        assertEquals(12, item.producto.unidadesPorEmpaque) // 12 unidades dentro
    }

    @Test
    fun `bolsa de manzanas`() {
        manager.agregarProducto(manzanas, 1.0, 500) // $5.00 la bolsa

        assertEquals(1, manager.itemCount)
        val item = manager.getItems()[0]
        assertEquals(500L, item.subtotal) // $5.00
        assertEquals(10, item.producto.unidadesPorEmpaque) // 10 manzanas

        // Costo por manzana
        val costoPorManzana = item.precioUnitario / item.producto.unidadesPorEmpaque
        assertEquals(50L, costoPorManzana) // $0.50
    }

    @Test
    fun `compra completa con total correcto`() {
        manager.agregarProducto(leche, 1.0, 199)     // $1.99
        manager.agregarProducto(leche, 1.0, 199)     // +1 leche = $3.98
        manager.agregarProducto(sopa, 1.0, 39)       // $0.39
        manager.agregarProducto(sopa, 1.0, 39)       // +1 sopa = $0.78
        manager.agregarProducto(sopa, 1.0, 39)       // +1 sopa = $1.17
        manager.agregarProducto(arroz, 1.0, 350)     // $3.50
        manager.agregarProducto(packLeche, 1.0, 1800) // $18.00
        manager.agregarProducto(manzanas, 1.0, 500)   // $5.00

        assertEquals(5, manager.itemCount) // 5 productos distintos

        val total = manager.getItems().sumOf { it.subtotal }
        // 2x199 + 3x39 + 350 + 1800 + 500 = 398 + 117 + 350 + 1800 + 500 = 3165
        assertEquals(3165L, total) // $31.65
    }

    @Test
    fun `remover item del carrito`() {
        manager.agregarProducto(leche, 1.0, 199)
        manager.agregarProducto(sopa, 1.0, 39)

        assertEquals(2, manager.itemCount)

        manager.removerItem(0) // quitar leche

        assertEquals(1, manager.itemCount)
        assertEquals("Sopa Maggi 55g", manager.getItems()[0].producto.nombre)
    }

    @Test
    fun `limpiar carrito`() {
        manager.agregarProducto(leche, 1.0, 199)
        manager.agregarProducto(sopa, 1.0, 39)

        manager.limpiar()

        assertEquals(0, manager.itemCount)
        assertEquals(null, manager.getTienda())
    }

    @Test
    fun `tienda se asigna correctamente`() {
        assertEquals("Super Selectos", manager.getTienda()?.nombre)
    }
}
