package com.mg.costeoapp.feature.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchTest {

	private fun shouldSearch(query: String): Boolean = query.length >= 2

	@Test
	fun queryVacio_noDispara() {
		assertFalse(shouldSearch(""))
	}

	@Test
	fun queryUnCaracter_noDispara() {
		assertFalse(shouldSearch("A"))
	}

	@Test
	fun queryDosCaracteres_disparaBusqueda() {
		assertTrue(shouldSearch("AB"))
	}

	@Test
	fun queryLargo_disparaBusqueda() {
		assertTrue(shouldSearch("Pollo asado con papas"))
	}

	@Test
	fun queryEspacios_dosCaracteres_disparaBusqueda() {
		assertTrue(shouldSearch("  "))
	}

	@Test
	fun queryEspacio_unCaracter_noDispara() {
		assertFalse(shouldSearch(" "))
	}
}
