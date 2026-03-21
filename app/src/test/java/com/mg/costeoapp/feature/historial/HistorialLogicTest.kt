package com.mg.costeoapp.feature.historial

import com.mg.costeoapp.core.database.dao.PrecioHistoricoRaw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistorialLogicTest {

	private fun calcMinPrecio(precios: List<PrecioHistoricoRaw>): Long? =
		precios.minByOrNull { it.precio }?.precio

	private fun calcMaxPrecio(precios: List<PrecioHistoricoRaw>): Long? =
		precios.maxByOrNull { it.precio }?.precio

	private fun calcPrecioActual(precios: List<PrecioHistoricoRaw>): Long? =
		precios.firstOrNull()?.precio

	private fun crearPrecio(
		id: Long = 1L,
		productoId: Long = 1L,
		tiendaId: Long = 1L,
		tiendaNombre: String = "Tienda",
		precio: Long,
		fechaRegistro: Long = System.currentTimeMillis()
	) = PrecioHistoricoRaw(
		id = id,
		productoId = productoId,
		tiendaId = tiendaId,
		tiendaNombre = tiendaNombre,
		precio = precio,
		fechaRegistro = fechaRegistro
	)

	@Test
	fun listaVacia_minEsNull() {
		assertNull(calcMinPrecio(emptyList()))
	}

	@Test
	fun listaVacia_maxEsNull() {
		assertNull(calcMaxPrecio(emptyList()))
	}

	@Test
	fun listaVacia_actualEsNull() {
		assertNull(calcPrecioActual(emptyList()))
	}

	@Test
	fun unElemento_minIgualMaxIgualActual() {
		val precios = listOf(crearPrecio(precio = 500))
		assertEquals(500L, calcMinPrecio(precios))
		assertEquals(500L, calcMaxPrecio(precios))
		assertEquals(500L, calcPrecioActual(precios))
	}

	@Test
	fun variosElementos_minEsMenorPrecio() {
		val precios = listOf(
			crearPrecio(id = 1, precio = 300, fechaRegistro = 3000),
			crearPrecio(id = 2, precio = 100, fechaRegistro = 2000),
			crearPrecio(id = 3, precio = 500, fechaRegistro = 1000)
		)
		assertEquals(100L, calcMinPrecio(precios))
	}

	@Test
	fun variosElementos_maxEsMayorPrecio() {
		val precios = listOf(
			crearPrecio(id = 1, precio = 300, fechaRegistro = 3000),
			crearPrecio(id = 2, precio = 100, fechaRegistro = 2000),
			crearPrecio(id = 3, precio = 500, fechaRegistro = 1000)
		)
		assertEquals(500L, calcMaxPrecio(precios))
	}

	@Test
	fun variosElementos_actualEsPrimeroEnLista() {
		val precios = listOf(
			crearPrecio(id = 1, precio = 300, fechaRegistro = 3000),
			crearPrecio(id = 2, precio = 100, fechaRegistro = 2000),
			crearPrecio(id = 3, precio = 500, fechaRegistro = 1000)
		)
		assertEquals(300L, calcPrecioActual(precios))
	}

	@Test
	fun preciosIguales_minIgualMax() {
		val precios = listOf(
			crearPrecio(id = 1, precio = 250, fechaRegistro = 3000),
			crearPrecio(id = 2, precio = 250, fechaRegistro = 2000),
			crearPrecio(id = 3, precio = 250, fechaRegistro = 1000)
		)
		assertEquals(250L, calcMinPrecio(precios))
		assertEquals(250L, calcMaxPrecio(precios))
	}

	@Test
	fun dosElementos_minYMaxCorrectos() {
		val precios = listOf(
			crearPrecio(id = 1, precio = 800, fechaRegistro = 2000),
			crearPrecio(id = 2, precio = 200, fechaRegistro = 1000)
		)
		assertEquals(200L, calcMinPrecio(precios))
		assertEquals(800L, calcMaxPrecio(precios))
		assertEquals(800L, calcPrecioActual(precios))
	}
}
