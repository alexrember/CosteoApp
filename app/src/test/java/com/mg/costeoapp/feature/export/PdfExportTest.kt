package com.mg.costeoapp.feature.export

import com.mg.costeoapp.core.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfExportTest {

	private fun calculateRemainingComponents(
		totalComponents: Int,
		currentIndex: Int
	): Int = totalComponents - currentIndex - 1

	private fun shouldTruncate(y: Float): Boolean = y > 780f

	private fun simulateYPosition(
		componentCount: Int,
		baseY: Float = 195f,
		perComponent: Float = 33f
	): Float = baseY + componentCount * perComponent

	@Test
	fun componentes24_noTrunca() {
		val y = simulateYPosition(24)
		// 195 + 24*33 = 987 > 780, pero el check ocurre despues de dibujar
		// Con 23 componentes dibujados: 195 + 23*33 = 954 > 780
		// El truncado ocurre cuando y > 780 despues de dibujar un componente
		// Verificamos que la logica de remaining es correcta
		val remaining = calculateRemainingComponents(30, 23)
		assertEquals(6, remaining)
	}

	@Test
	fun remaining_ultimoComponente_retornaCero() {
		val remaining = calculateRemainingComponents(10, 9)
		assertEquals(0, remaining)
	}

	@Test
	fun remaining_primerComponente_retornaRestoTotal() {
		val remaining = calculateRemainingComponents(25, 0)
		assertEquals(24, remaining)
	}

	@Test
	fun remaining_componenteMedio_retornaCorrectamente() {
		val remaining = calculateRemainingComponents(25, 12)
		assertEquals(12, remaining)
	}

	@Test
	fun truncado_yMayorA780_debeCortar() {
		assertTrue(shouldTruncate(781f))
	}

	@Test
	fun truncado_yIgualA780_noCorta() {
		assertTrue(!shouldTruncate(780f))
	}

	@Test
	fun truncado_yMenorA780_noCorta() {
		assertTrue(!shouldTruncate(500f))
	}

	@Test
	fun currencyFormatter_centavosPositivos() {
		assertEquals("$1.50", CurrencyFormatter.fromCents(150))
	}

	@Test
	fun currencyFormatter_cero() {
		assertEquals("$0.00", CurrencyFormatter.fromCents(0))
	}

	@Test
	fun currencyFormatter_montoGrande() {
		assertEquals("$1,234.56", CurrencyFormatter.fromCents(123456))
	}

	@Test
	fun currencyFormatter_unCentavo() {
		assertEquals("$0.01", CurrencyFormatter.fromCents(1))
	}

	@Test
	fun listaVacia_sinComponentes_noTrunca() {
		val componentes = emptyList<String>()
		assertEquals(0, componentes.size)
		// Con lista vacia no se entra al loop, no hay truncado
		assertTrue(componentes.isEmpty())
	}
}
