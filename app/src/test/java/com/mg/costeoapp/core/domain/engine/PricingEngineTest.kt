package com.mg.costeoapp.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToLong

/**
 * Tests de la formula de costeo. NOTA: Estos tests replican la formula
 * de PricingEngine.calcularCostoIngrediente() localmente porque el motor
 * requiere DAOs de Room que no estan disponibles en unit tests.
 * Si la formula cambia en PricingEngine, actualizar aqui tambien.
 */
class PricingEngineTest {

	private fun calcularCostoIngrediente(
		precioUnitario: Long,
		cantidadPorEmpaque: Double,
		unidadesPorEmpaque: Int = 1,
		cantidadUsada: Double,
		factorMerma: Int
	): Long {
		if (cantidadPorEmpaque <= 0) return 0L
		val contenidoTotal = cantidadPorEmpaque * maxOf(unidadesPorEmpaque, 1)
		val precioPorUnidad = precioUnitario.toDouble() / contenidoTotal
		val costo = precioPorUnidad * cantidadUsada
		return if (factorMerma > 0 && factorMerma < 100) {
			(costo / (1.0 - factorMerma / 100.0)).roundToLong()
		} else {
			costo.roundToLong()
		}
	}

	// =====================================================================
	// Flujo completo: Registrar receta con chicle PriceSmart
	// Producto: Chicle Extra Spearmint 10 Unidades x 15 unidades c/u
	//   - Precio PriceSmart: $11.29 (1129 centavos)
	//   - unidades_por_empaque: 10 (paquetes individuales)
	//   - cantidad_por_empaque: 15 (chicles por paquete)
	//   - Total chicles: 10 * 15 = 150
	//   - Precio por chicle: $11.29 / 150 = $0.0753
	// =====================================================================

	@Test
	fun `chicle PriceSmart - 5 unidades debe costar 0_38`() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1129,      // $11.29
			cantidadPorEmpaque = 15.0,  // 15 chicles por paquete individual
			unidadesPorEmpaque = 10,    // 10 paquetes en el paqueton
			cantidadUsada = 5.0,        // uso 5 chicles
			factorMerma = 0
		)
		// 1129 / (15 * 10) * 5 = 1129 / 150 * 5 = 37.63 -> 38
		assertEquals(38L, resultado)
	}

	@Test
	fun `chicle PriceSmart - 1 unidad debe costar 0_08`() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 15.0,
			unidadesPorEmpaque = 10,
			cantidadUsada = 1.0,
			factorMerma = 0
		)
		// 1129 / 150 * 1 = 7.527 -> 8
		assertEquals(8L, resultado)
	}

	@Test
	fun `chicle PriceSmart - paquete completo 150 unidades cuesta 11_29`() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 15.0,
			unidadesPorEmpaque = 10,
			cantidadUsada = 150.0,
			factorMerma = 0
		)
		assertEquals(1129L, resultado)
	}

	@Test
	fun `chicle PriceSmart - 1 paquete individual 15 chicles`() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 15.0,
			unidadesPorEmpaque = 10,
			cantidadUsada = 15.0,
			factorMerma = 0
		)
		// 1129 / 150 * 15 = 112.9 -> 113
		assertEquals(113L, resultado)
	}

	@Test
	fun `receta test2 - chicle 5 unidades porciones 1 costo total 0_38`() {
		val costoFijo = 0L
		val costoIngrediente = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 15.0,
			unidadesPorEmpaque = 10,
			cantidadUsada = 5.0,
			factorMerma = 0
		)
		val costoTotal = costoFijo + costoIngrediente
		val porciones = 1.0
		val costoPorPorcion = (costoTotal.toDouble() / porciones).roundToLong()

		assertEquals(38L, costoTotal)
		assertEquals(38L, costoPorPorcion)
	}

	@Test
	fun `receta con 2 ingredientes PriceSmart`() {
		// Chicle: $11.29 paquete (10 x 15 = 150 unidades), uso 10
		val costoChicle = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 15.0,
			unidadesPorEmpaque = 10,
			cantidadUsada = 10.0,
			factorMerma = 0
		)
		// 1129 / 150 * 10 = 75.27 -> 75

		// Agua 24 pack: $11.29 (1129c), 503ml x 24 = 12072ml, uso 1000ml
		val costoAgua = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 503.0,
			unidadesPorEmpaque = 24,
			cantidadUsada = 1000.0,
			factorMerma = 0
		)
		// 1129 / (503 * 24) * 1000 = 1129 / 12072 * 1000 = 93.51 -> 94

		val costoTotal = costoChicle + costoAgua
		assertEquals(75L, costoChicle)
		assertEquals(94L, costoAgua)
		assertEquals(169L, costoTotal)
	}

	// =====================================================================
	// Tests originales actualizados (unidadesPorEmpaque = 1 por defecto)
	// =====================================================================

	@Test
	fun costoSinMerma_calculaCorrectamente() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 500,
			cantidadPorEmpaque = 1000.0,
			cantidadUsada = 200.0,
			factorMerma = 0
		)
		assertEquals(100L, resultado)
	}

	@Test
	fun costoConMerma20_calculaCorrectamente() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 500,
			cantidadPorEmpaque = 1000.0,
			cantidadUsada = 200.0,
			factorMerma = 20
		)
		assertEquals(125L, resultado)
	}

	@Test
	fun costoConMerma99_caseTodoDespericio() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1000,
			cantidadPorEmpaque = 1.0,
			cantidadUsada = 1.0,
			factorMerma = 99
		)
		assertEquals(100000L, resultado)
	}

	@Test
	fun costoConMerma100_noAplicaMerma() {
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
		val resultado = calcularCostoIngrediente(
			precioUnitario = 300,
			cantidadPorEmpaque = 12.0,
			cantidadUsada = 0.5,
			factorMerma = 0
		)
		assertEquals(13L, resultado)
	}

	@Test
	fun multiplesIngredientes_sumaTotalCorrecta() {
		val costo1 = calcularCostoIngrediente(
			precioUnitario = 250,
			cantidadPorEmpaque = 1000.0,
			cantidadUsada = 500.0,
			factorMerma = 0
		)
		val costo2 = calcularCostoIngrediente(
			precioUnitario = 800,
			cantidadPorEmpaque = 453.592,
			cantidadUsada = 200.0,
			factorMerma = 15
		)
		val costoTotal = costo1 + costo2
		assertEquals(125L, costo1)
		assertEquals(415L, costo2)
		assertEquals(540L, costoTotal)
	}

	@Test
	fun costoFijoReceta_sumadoCorrectamente() {
		val costoFijo = 500L
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
		assertEquals(333L, costoPorPorcion)
	}

	@Test
	fun empaqueGrande_precioChico() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 100,
			cantidadPorEmpaque = 5000.0,
			cantidadUsada = 10.0,
			factorMerma = 0
		)
		assertEquals(0L, resultado)
	}

	// =====================================================================
	// Producto individual (unidadesPorEmpaque = 1) vs paquete
	// =====================================================================

	@Test
	fun `producto individual unidadesPorEmpaque 1 calcula igual que antes`() {
		// Leche 946ml, $1.99, usa 200ml
		val resultado = calcularCostoIngrediente(
			precioUnitario = 199,
			cantidadPorEmpaque = 946.0,
			unidadesPorEmpaque = 1,
			cantidadUsada = 200.0,
			factorMerma = 0
		)
		// 199 / 946 * 200 = 42.07 -> 42
		assertEquals(42L, resultado)
	}

	@Test
	fun `pack 12 leches vs leche individual`() {
		// Pack: $18.00 (1800c), 946ml x 12 = 11352ml, uso 946ml (1 leche)
		val costoPack = calcularCostoIngrediente(
			precioUnitario = 1800,
			cantidadPorEmpaque = 946.0,
			unidadesPorEmpaque = 12,
			cantidadUsada = 946.0,
			factorMerma = 0
		)
		// 1800 / (946 * 12) * 946 = 1800 / 12 = 150

		// Individual: $1.99 (199c), 946ml, uso 946ml
		val costoIndividual = calcularCostoIngrediente(
			precioUnitario = 199,
			cantidadPorEmpaque = 946.0,
			unidadesPorEmpaque = 1,
			cantidadUsada = 946.0,
			factorMerma = 0
		)
		// 199 / 946 * 946 = 199

		assertEquals(150L, costoPack)
		assertEquals(199L, costoIndividual)
		// El pack es mas barato por unidad
		assert(costoPack < costoIndividual)
	}

	@Test
	fun `unidadesPorEmpaque 0 se trata como 1`() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1000,
			cantidadPorEmpaque = 10.0,
			unidadesPorEmpaque = 0,
			cantidadUsada = 5.0,
			factorMerma = 0
		)
		// maxOf(0, 1) = 1, so 1000 / (10 * 1) * 5 = 500
		assertEquals(500L, resultado)
	}

	@Test
	fun `chicle con merma 10 porciento`() {
		val resultado = calcularCostoIngrediente(
			precioUnitario = 1129,
			cantidadPorEmpaque = 15.0,
			unidadesPorEmpaque = 10,
			cantidadUsada = 5.0,
			factorMerma = 10
		)
		// 1129 / 150 * 5 = 37.63; / 0.9 = 41.81 -> 42
		assertEquals(42L, resultado)
	}
}
