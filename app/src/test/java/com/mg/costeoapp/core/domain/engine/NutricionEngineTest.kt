package com.mg.costeoapp.core.domain.engine

import com.mg.costeoapp.core.util.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutricionEngineTest {

	private fun convertirAGramos(cantidad: Double, unidad: String): Double? {
		val um = UnidadMedida.fromCodigo(unidad) ?: return null
		val factor = um.factorAGramos ?: return null
		return cantidad * factor
	}

	@Test
	fun convertirLibrasAGramos() {
		val resultado = convertirAGramos(1.0, "lb")
		assertEquals(453.592, resultado!!, 0.001)
	}

	@Test
	fun convertirKilogramosAGramos() {
		val resultado = convertirAGramos(1.0, "kg")
		assertEquals(1000.0, resultado!!, 0.001)
	}

	@Test
	fun convertirGramosAGramos() {
		val resultado = convertirAGramos(250.0, "g")
		assertEquals(250.0, resultado!!, 0.001)
	}

	@Test
	fun convertirMililitrosAGramos() {
		val resultado = convertirAGramos(500.0, "ml")
		assertEquals(500.0, resultado!!, 0.001)
	}

	@Test
	fun convertirOnzasAGramos() {
		val resultado = convertirAGramos(1.0, "oz")
		assertEquals(28.3495, resultado!!, 0.001)
	}

	@Test
	fun convertirLitrosAGramos() {
		val resultado = convertirAGramos(1.0, "l")
		assertEquals(1000.0, resultado!!, 0.001)
	}

	@Test
	fun unidad_retornaNull() {
		val resultado = convertirAGramos(5.0, "unidad")
		assertNull(resultado)
	}

	@Test
	fun unidadDesconocida_retornaNull() {
		assertNull(convertirAGramos(1.0, "taza"))
		assertNull(convertirAGramos(1.0, "cucharada"))
		assertNull(convertirAGramos(1.0, "xyz"))
	}

	@Test
	fun cantidadCero_retornaCero() {
		val resultado = convertirAGramos(0.0, "kg")
		assertEquals(0.0, resultado!!, 0.001)
	}

	@Test
	fun cantidadFraccionaria_calculaCorrectamente() {
		val resultado = convertirAGramos(0.5, "lb")
		assertEquals(226.796, resultado!!, 0.001)
	}

	@Test
	fun unidadMayusculas_funcionaConNormalizacion() {
		val resultado = convertirAGramos(1.0, "KG")
		assertEquals(1000.0, resultado!!, 0.001)
	}

	@Test
	fun unidadMixedCase_funcionaConNormalizacion() {
		val resultado = convertirAGramos(1.0, "Lb")
		assertEquals(453.592, resultado!!, 0.001)
	}
}
