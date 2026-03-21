package com.mg.costeoapp.feature.platos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.roundToLong

class PrecioVentaTest {

	private fun calculatePrecioVenta(
		costoTotal: Long,
		margenPorcentaje: Double?,
		precioVentaManual: Long? = null
	): Long? {
		if (precioVentaManual != null) return precioVentaManual
		val margen = margenPorcentaje ?: return null
		if (margen <= 0 || margen >= 100) return null
		return (costoTotal.toDouble() / (1.0 - margen / 100.0)).roundToLong()
	}

	@Test
	fun costo145_margen35_retorna223() {
		val resultado = calculatePrecioVenta(costoTotal = 145, margenPorcentaje = 35.0)
		assertEquals(223L, resultado)
	}

	@Test
	fun costo100_margen30_retorna143() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = 30.0)
		assertEquals(143L, resultado)
	}

	@Test
	fun costo500_margen25_retorna667() {
		val resultado = calculatePrecioVenta(costoTotal = 500, margenPorcentaje = 25.0)
		assertEquals(667L, resultado)
	}

	@Test
	fun costoCero_retornaCero() {
		val resultado = calculatePrecioVenta(costoTotal = 0, margenPorcentaje = 35.0)
		assertEquals(0L, resultado)
	}

	@Test
	fun margenCero_retornaNull() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = 0.0)
		assertNull(resultado)
	}

	@Test
	fun margen100_retornaNull() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = 100.0)
		assertNull(resultado)
	}

	@Test
	fun margenMayor100_retornaNull() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = 150.0)
		assertNull(resultado)
	}

	@Test
	fun margenNegativo_retornaNull() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = -10.0)
		assertNull(resultado)
	}

	@Test
	fun precioManual_tienePrecedencia() {
		val resultado = calculatePrecioVenta(
			costoTotal = 100,
			margenPorcentaje = 35.0,
			precioVentaManual = 500
		)
		assertEquals(500L, resultado)
	}

	@Test
	fun sinMargen_retornaNull() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = null)
		assertNull(resultado)
	}

	@Test
	fun margen99_bordeMaximoValido() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = 99.0)
		// 100 / (1 - 0.99) = 100 / 0.01 = 10000
		assertEquals(10000L, resultado)
	}

	@Test
	fun margen1_bordeMinimoValido() {
		val resultado = calculatePrecioVenta(costoTotal = 100, margenPorcentaje = 1.0)
		// 100 / (1 - 0.01) = 100 / 0.99 = 101.01... rounds to 101
		assertEquals(101L, resultado)
	}

	@Test
	fun costoMuyGrande_calculaSinOverflow() {
		val costoGrande = Long.MAX_VALUE / 2
		val resultado = calculatePrecioVenta(costoTotal = costoGrande, margenPorcentaje = 50.0)
		// costoGrande / (1 - 0.5) = costoGrande / 0.5 = costoGrande * 2
		val esperado = (costoGrande.toDouble() / 0.5).roundToLong()
		assertEquals(esperado, resultado)
	}
}
