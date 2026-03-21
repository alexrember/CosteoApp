package com.mg.costeoapp.feature.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportTest {

	private fun escapeCsv(value: String): String {
		val sanitized = if (value.isNotEmpty() && value[0] in charArrayOf('=', '+', '-', '@')) {
			"'$value"
		} else value
		return if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
			"\"${sanitized.replace("\"", "\"\"")}\""
		} else sanitized
	}

	@Test
	fun textoNormal_sinCambios() {
		assertEquals("Pollo asado", escapeCsv("Pollo asado"))
	}

	@Test
	fun textoVacio_sinCambios() {
		assertEquals("", escapeCsv(""))
	}

	@Test
	fun inyeccionCsv_igualPrefijado() {
		assertEquals("'=CMD()", escapeCsv("=CMD()"))
	}

	@Test
	fun inyeccionCsv_masPrefijado() {
		assertEquals("'+1-555-1234", escapeCsv("+1-555-1234"))
	}

	@Test
	fun inyeccionCsv_menosPrefijado() {
		assertEquals("'-100", escapeCsv("-100"))
	}

	@Test
	fun inyeccionCsv_arrobaPrefijado() {
		assertEquals("'@SUM(A1)", escapeCsv("@SUM(A1)"))
	}

	@Test
	fun textoConComas_entreComillas() {
		assertEquals("\"Sal, pimienta\"", escapeCsv("Sal, pimienta"))
	}

	@Test
	fun textoConComillasDobles_escapadas() {
		assertEquals("\"Queso \"\"cheddar\"\"\"", escapeCsv("Queso \"cheddar\""))
	}

	@Test
	fun textoConSaltoLinea_entreComillas() {
		assertEquals("\"Linea1\nLinea2\"", escapeCsv("Linea1\nLinea2"))
	}

	@Test
	fun inyeccionConComa_prefijadoYEntreComillas() {
		assertEquals("\"'=A1,B2\"", escapeCsv("=A1,B2"))
	}

	@Test
	fun textoSoloEspacios_sinCambios() {
		assertEquals("   ", escapeCsv("   "))
	}
}
