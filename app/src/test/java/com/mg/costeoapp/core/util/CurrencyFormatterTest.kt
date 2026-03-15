package com.mg.costeoapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyFormatterTest {

	@Test
	fun fromCents_formatea_correctamente() {
		assertEquals("$10.50", CurrencyFormatter.fromCents(1050))
		assertEquals("$0.00", CurrencyFormatter.fromCents(0))
		assertEquals("$1.00", CurrencyFormatter.fromCents(100))
		assertEquals("$99.99", CurrencyFormatter.fromCents(9999))
	}

	@Test
	fun toCents_convierte_correctamente() {
		assertEquals(1050L, CurrencyFormatter.toCents("10.50"))
		assertEquals(100L, CurrencyFormatter.toCents("1.00"))
		assertEquals(0L, CurrencyFormatter.toCents("0"))
		assertEquals(9999L, CurrencyFormatter.toCents("99.99"))
	}

	@Test
	fun toCents_maneja_simbolo_dolar() {
		assertEquals(1050L, CurrencyFormatter.toCents("$10.50"))
	}

	@Test
	fun toCents_precision_floating_point() {
		// Este es el caso critico: 19.99 * 100 = 1998.9999... en floating point
		assertEquals(1999L, CurrencyFormatter.toCents("19.99"))
		assertEquals(1001L, CurrencyFormatter.toCents("10.01"))
		assertEquals(2999L, CurrencyFormatter.toCents("29.99"))
	}

	@Test
	fun toCents_retorna_null_para_invalido() {
		assertNull(CurrencyFormatter.toCents(""))
		assertNull(CurrencyFormatter.toCents("abc"))
		assertNull(CurrencyFormatter.toCents("--5"))
	}
}
