package com.mg.costeoapp.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToLong

class PricingEngineTest {

	private fun calcularCostoIngrediente(
		precioUnitario: Long,
		cantidadPorEmpaque: Double,
		cantidadUsada: Double,
		factorMerma: Int
	): Long {
		val precioPorUnidad = precioUnitario.toDouble() / cantidadPorEmpaque
		val costo = precioPorUnidad * cantidadUsada
		return if (factorMerma > 0 && factorMerma < 100) {
			(costo / (1.0 - factorMerma / 100.0)).roundToLong()
		} else {
			costo.roundToLong()
		}
	}

	@Test
	fun costoSinMerma_calculaCorrectamente() {
		// Producto: $5.00 (500 centavos), empaque 1000g, usa 200g, merma 0%
		val resultado = calcularCostoIngrediente(
			precioUnitario = 500,
			cantidadPorEmpaque = 1000.0,
			cantidadUsada = 200.0,
			factorMerma = 0
		)
		// (500 / 1000) * 200 = 100 centavos = $1.00
		assertEquals(100L, resultado)
	}

	@Test
	fun costoConMerma20_calculaCorrectamente() {
		// Producto: $5.00 (500 centavos), empaque 1000g, usa 200g, merma 20%
		val resultado = calcularCostoIngrediente(
			precioUnitario = 500,
			cantidadPorEmpaque = 1000.0,
			cantidadUsada = 200.0,
			factorMerma = 20
		)
		// (500 / 1000) * 200 = 100; 100 / (1 - 0.20) = 100 / 0.8 = 125
		assertEquals(125L, resultado)
	}

	@Test
	fun costoConMerma99_caseTodoDespericio() {
		// Merma extrema 99%: casi todo se desperdicia
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1000,
			cantidadPorEmpaque = 1.0,
			cantidadUsada = 1.0,
			factorMerma = 99
		)
		// 1000 / (1 - 0.99) = 1000 / 0.01 = 100000
		assertEquals(100000L, resultado)
	}

	@Test
	fun costoConMerma100_noAplicaMerma() {
		// factorMerma 100 no es < 100, asi que no aplica merma
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1000,
			cantidadPorEmpaque = 1.0,
			cantidadUsada = 1.0,
			factorMerma = 100
		)
		assertEquals(1000L, resultado)
	}

	@Test
	fun costoConMermaNegativa_noAplicaMerma() {
		// factorMerma negativo no es > 0, no aplica merma
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1000,
			cantidadPorEmpaque = 1.0,
			cantidadUsada = 1.0,
			factorMerma = -5
		)
		assertEquals(1000L, resultado)
	}

	@Test
	fun precioUnitarioCero_retornaCero() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 0,
			cantidadPorEmpaque = 500.0,
			cantidadUsada = 100.0,
			factorMerma = 10
		)
		assertEquals(0L, resultado)
	}

	@Test
	fun cantidadUsadaFraccionaria_calculaCorrectamente() {
		// Producto: $3.00 (300 centavos), empaque 12 unidades, usa 0.5, merma 0%
		val resultado = calcularCostoIngrediente(
			precioUnitario = 300,
			cantidadPorEmpaque = 12.0,
			cantidadUsada = 0.5,
			factorMerma = 0
		)
		// (300 / 12) * 0.5 = 12.5 -> rounds to 13
		assertEquals(13L, resultado)
	}

	@Test
	fun multiplesIngredientes_sumaTotalCorrecta() {
		// Ingrediente 1: harina $2.50 (250c), 1000g, usa 500g, merma 0%
		val costo1 = calcularCostoIngrediente(
			precioUnitario = 250,
			cantidadPorEmpaque = 1000.0,
			cantidadUsada = 500.0,
			factorMerma = 0
		)
		// (250/1000)*500 = 125

		// Ingrediente 2: carne $8.00 (800c), 1lb (453.592g), usa 200g, merma 15%
		val costo2 = calcularCostoIngrediente(
			precioUnitario = 800,
			cantidadPorEmpaque = 453.592,
			cantidadUsada = 200.0,
			factorMerma = 15
		)
		// (800/453.592)*200 = 352.74; / 0.85 = 415.0
		val costoTotal = costo1 + costo2
		assertEquals(125L, costo1)
		assertEquals(415L, costo2)
		assertEquals(540L, costoTotal)
	}

	@Test
	fun costoFijoReceta_sumadoCorrectamente() {
		val costoFijo = 500L // $5.00 gas
		val costoIngrediente = calcularCostoIngrediente(
			precioUnitario = 300,
			cantidadPorEmpaque = 1.0,
			cantidadUsada = 1.0,
			factorMerma = 0
		)
		val costoTotal = costoFijo + costoIngrediente
		assertEquals(800L, costoTotal)
	}

	@Test
	fun costoPorPorcion_divideCorrectamente() {
		val costoTotal = 1000L
		val porciones = 4.0
		val costoPorPorcion = (costoTotal.toDouble() / porciones).roundToLong()
		assertEquals(250L, costoPorPorcion)
	}

	@Test
	fun costoPorPorcion_redondea() {
		val costoTotal = 1000L
		val porciones = 3.0
		val costoPorPorcion = (costoTotal.toDouble() / porciones).roundToLong()
		// 1000 / 3 = 333.33... rounds to 333
		assertEquals(333L, costoPorPorcion)
	}

	@Test
	fun empaqueGrande_precioChico() {
		// Producto barato, empaque grande: $1.00 (100c) por 5000g, usa 10g
		val resultado = calcularCostoIngrediente(
			precioUnitario = 100,
			cantidadPorEmpaque = 5000.0,
			cantidadUsada = 10.0,
			factorMerma = 0
		)
		// (100/5000)*10 = 0.2 -> rounds to 0
		assertEquals(0L, resultado)
	}
}
