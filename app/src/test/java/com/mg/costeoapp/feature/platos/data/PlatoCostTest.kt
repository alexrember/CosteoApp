package com.mg.costeoapp.feature.platos.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToLong

class PlatoCostTest {

	private fun costoPrefabricadoComponente(costoPorPorcion: Long, cantidad: Double): Long =
		(costoPorPorcion * cantidad).roundToLong()

	private fun costoProductoComponente(precioUnitario: Long?, cantidadPorEmpaque: Double, cantidad: Double): Long {
		if (precioUnitario == null) return 0L
		val ppu = precioUnitario.toDouble() / cantidadPorEmpaque
		return (ppu * cantidad).roundToLong()
	}

	@Test
	fun prefabricadoComponente_costoPorPorcionPorCantidad() {
		val resultado = costoPrefabricadoComponente(costoPorPorcion = 250, cantidad = 2.0)
		assertEquals(500L, resultado)
	}

	@Test
	fun prefabricadoComponente_cantidadFraccionaria() {
		val resultado = costoPrefabricadoComponente(costoPorPorcion = 300, cantidad = 1.5)
		assertEquals(450L, resultado)
	}

	@Test
	fun productoComponente_calculaCorrecto() {
		val resultado = costoProductoComponente(
			precioUnitario = 500,
			cantidadPorEmpaque = 1000.0,
			cantidad = 200.0
		)
		assertEquals(100L, resultado)
	}

	@Test
	fun productoComponente_empaqueUnitario() {
		val resultado = costoProductoComponente(
			precioUnitario = 150,
			cantidadPorEmpaque = 1.0,
			cantidad = 3.0
		)
		assertEquals(450L, resultado)
	}

	@Test
	fun componentesMixtos_sumanCorrectamente() {
		val costoPref = costoPrefabricadoComponente(costoPorPorcion = 250, cantidad = 2.0)
		val costoProd = costoProductoComponente(
			precioUnitario = 500,
			cantidadPorEmpaque = 1000.0,
			cantidad = 200.0
		)
		assertEquals(600L, costoPref + costoProd)
	}

	@Test
	fun precioNulo_contribuyeCero() {
		val resultado = costoProductoComponente(
			precioUnitario = null,
			cantidadPorEmpaque = 1.0,
			cantidad = 5.0
		)
		assertEquals(0L, resultado)
	}

	@Test
	fun precioCero_contribuyeCero() {
		val resultado = costoProductoComponente(
			precioUnitario = 0,
			cantidadPorEmpaque = 500.0,
			cantidad = 100.0
		)
		assertEquals(0L, resultado)
	}

	@Test
	fun variosComponentes_sumaTotalCorrecta() {
		val costos = listOf(
			costoPrefabricadoComponente(200, 1.0),
			costoProductoComponente(1000, 500.0, 100.0),
			costoProductoComponente(300, 12.0, 2.0),
			costoPrefabricadoComponente(150, 3.0)
		)
		val total = costos.sum()
		assertEquals(200L + 200L + 50L + 450L, total)
	}

	@Test
	fun cantidadMuyPequena_redondeoACero() {
		val resultado = costoProductoComponente(
			precioUnitario = 100,
			cantidadPorEmpaque = 5000.0,
			cantidad = 1.0
		)
		assertEquals(0L, resultado)
	}
}
