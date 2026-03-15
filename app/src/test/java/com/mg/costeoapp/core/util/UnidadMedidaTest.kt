package com.mg.costeoapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnidadMedidaTest {

	@Test
	fun fromCodigo_encuentra_por_codigo() {
		assertEquals(UnidadMedida.LIBRA, UnidadMedida.fromCodigo("lb"))
		assertEquals(UnidadMedida.KILOGRAMO, UnidadMedida.fromCodigo("kg"))
		assertEquals(UnidadMedida.UNIDAD, UnidadMedida.fromCodigo("unidad"))
	}

	@Test
	fun fromCodigo_case_insensitive() {
		assertEquals(UnidadMedida.LIBRA, UnidadMedida.fromCodigo("LB"))
		assertEquals(UnidadMedida.KILOGRAMO, UnidadMedida.fromCodigo("Kg"))
	}

	@Test
	fun fromCodigo_retorna_null_si_no_existe() {
		assertNull(UnidadMedida.fromCodigo("xyz"))
		assertNull(UnidadMedida.fromCodigo(""))
	}

	@Test
	fun factorAGramos_correcto() {
		assertEquals(453.592, UnidadMedida.LIBRA.factorAGramos!!, 0.001)
		assertEquals(1000.0, UnidadMedida.KILOGRAMO.factorAGramos!!, 0.001)
		assertEquals(1.0, UnidadMedida.GRAMO.factorAGramos!!, 0.001)
		assertNull(UnidadMedida.UNIDAD.factorAGramos)
	}
}
