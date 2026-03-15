package com.mg.costeoapp.feature.productos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.mg.costeoapp.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters

@HiltAndroidTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ProductoFlowTest {

	@get:Rule(order = 0)
	val hiltRule = HiltAndroidRule(this)

	@get:Rule(order = 1)
	val composeRule = createAndroidComposeRule<MainActivity>()

	@Before
	fun setup() {
		hiltRule.inject()
	}

	private fun navigateToProductos() {
		composeRule.onNode(
			hasContentDescription("Productos"),
			useUnmergedTree = true
		).performClick()
		composeRule.waitForIdle()
	}

	private fun crearProducto(nombre: String, cantidad: String = "1") {
		navigateToProductos()
		composeRule.onNodeWithContentDescription("Agregar producto").performClick()
		composeRule.waitForIdle()
		composeRule.onNode(hasText("Nombre *")).performTextInput(nombre)
		composeRule.onNode(hasText("Cantidad por empaque *")).performTextInput(cantidad)
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()
	}

	@Test
	fun t1_crear_producto_validacion_y_guardado() {
		navigateToProductos()

		composeRule.onNodeWithContentDescription("Agregar producto").performClick()
		composeRule.waitForIdle()
		composeRule.onNodeWithText("Nuevo producto").assertIsDisplayed()

		// Guardar sin datos muestra error
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()
		composeRule.onNodeWithText("El nombre debe tener al menos 2 caracteres").assertIsDisplayed()

		// Llenar y guardar
		composeRule.onNode(hasText("Nombre *")).performTextInput("Arroz")
		composeRule.onNode(hasText("Cantidad por empaque *")).performTextInput("1")
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Arroz").assertIsDisplayed()
	}

	@Test
	fun t2_detalle_producto_muestra_info_y_sin_precios() {
		crearProducto("Frijoles")

		composeRule.onNodeWithText("Frijoles").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Frijoles").assertIsDisplayed()
		composeRule.onNodeWithText("Sin precios registrados").assertIsDisplayed()
	}

	@Test
	fun t3_registrar_precio_a_producto() {
		crearProducto("Para Precio")

		composeRule.onNodeWithText("Para Precio").performClick()
		composeRule.waitForIdle()

		composeRule.onNode(hasContentDescription("Agregar precio")).performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Registrar precio").assertIsDisplayed()

		composeRule.onNodeWithText("Seleccionar tienda").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Super Selectos").performClick()
		composeRule.waitForIdle()

		composeRule.onNode(hasText("Precio (ej: 10.50) *")).performTextInput("1.50")

		composeRule.onNodeWithText("Guardar precio").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Para Precio").assertIsDisplayed()
	}

	@Test
	fun t4_editar_producto() {
		crearProducto("Para Editar Prod")

		composeRule.onNodeWithText("Para Editar Prod").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithContentDescription("Editar").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Editar producto").assertIsDisplayed()

		composeRule.onNode(hasText("Para Editar Prod")).performTextClearance()
		composeRule.onNode(hasText("Nombre *")).performTextInput("Producto Editado")

		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Producto Editado").assertIsDisplayed()
	}

	@Test
	fun t5_soft_delete_producto() {
		crearProducto("Para Borrar Prod")

		composeRule.onNodeWithText("Para Borrar Prod").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithContentDescription("Eliminar").performClick()
		composeRule.waitForIdle()

		// Confirmar en dialogo
		composeRule.onAllNodes(hasText("Eliminar")).onLast().performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Para Borrar Prod").assertDoesNotExist()
	}
}
