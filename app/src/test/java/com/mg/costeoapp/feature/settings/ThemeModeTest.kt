package com.mg.costeoapp.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

	@Test
	fun fromValue0_retornaSYSTEM() {
		assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(0))
	}

	@Test
	fun fromValue1_retornaLIGHT() {
		assertEquals(ThemeMode.LIGHT, ThemeMode.fromValue(1))
	}

	@Test
	fun fromValue2_retornaDARK() {
		assertEquals(ThemeMode.DARK, ThemeMode.fromValue(2))
	}

	@Test
	fun fromValue99_defaultSYSTEM() {
		assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(99))
	}

	@Test
	fun fromValueNegativo_defaultSYSTEM() {
		assertEquals(ThemeMode.SYSTEM, ThemeMode.fromValue(-1))
	}

	@Test
	fun systemValue_esCero() {
		assertEquals(0, ThemeMode.SYSTEM.value)
	}

	@Test
	fun lightValue_esUno() {
		assertEquals(1, ThemeMode.LIGHT.value)
	}

	@Test
	fun darkValue_esDos() {
		assertEquals(2, ThemeMode.DARK.value)
	}
}
