package com.mg.costeoapp.feature.dashboard

import com.mg.costeoapp.feature.dashboard.ui.DashboardUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardMetricsTest {

	private fun tieneAlertas(state: DashboardUiState): Boolean =
		state.productosSinPrecio > 0 ||
			state.productosConMermaAlta > 0 ||
			state.productosConStockBajo > 0 ||
			state.recetasConIngredientesInactivos > 0

	@Test
	fun todosEnCero_sinAlertas() {
		val state = DashboardUiState(
			productosSinPrecio = 0,
			productosConMermaAlta = 0,
			productosConStockBajo = 0,
			recetasConIngredientesInactivos = 0
		)
		assertFalse(tieneAlertas(state))
	}

	@Test
	fun productosSinPrecio_muestraAlerta() {
		val state = DashboardUiState(productosSinPrecio = 3)
		assertTrue(tieneAlertas(state))
	}

	@Test
	fun productosConMermaAlta_muestraAlerta() {
		val state = DashboardUiState(productosConMermaAlta = 2)
		assertTrue(tieneAlertas(state))
	}

	@Test
	fun productosConStockBajo_muestraAlerta() {
		val state = DashboardUiState(productosConStockBajo = 1)
		assertTrue(tieneAlertas(state))
	}

	@Test
	fun recetasConIngredientesInactivos_muestraAlerta() {
		val state = DashboardUiState(recetasConIngredientesInactivos = 5)
		assertTrue(tieneAlertas(state))
	}

	@Test
	fun variasAlertas_muestraAlerta() {
		val state = DashboardUiState(
			productosSinPrecio = 1,
			productosConMermaAlta = 2,
			productosConStockBajo = 3,
			recetasConIngredientesInactivos = 1
		)
		assertTrue(tieneAlertas(state))
	}

	@Test
	fun defaultState_valoresEnCero() {
		val state = DashboardUiState()
		assertEquals(0, state.totalTiendas)
		assertEquals(0, state.totalProductos)
		assertEquals(0, state.totalRecetas)
		assertEquals(0, state.totalPlatos)
		assertEquals(0, state.productosSinPrecio)
		assertEquals(0, state.productosConMermaAlta)
		assertEquals(0, state.productosConStockBajo)
		assertEquals(0, state.recetasConIngredientesInactivos)
	}

	@Test
	fun defaultState_isLoadingTrue() {
		val state = DashboardUiState()
		assertTrue(state.isLoading)
	}

	@Test
	fun copyConDatos_mantieneOtrosValores() {
		val state = DashboardUiState(
			totalTiendas = 5,
			totalProductos = 20,
			totalRecetas = 8,
			totalPlatos = 12,
			isLoading = false
		)
		assertEquals(5, state.totalTiendas)
		assertEquals(20, state.totalProductos)
		assertEquals(8, state.totalRecetas)
		assertEquals(12, state.totalPlatos)
		assertFalse(state.isLoading)
	}
}
