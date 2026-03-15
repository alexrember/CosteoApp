package com.mg.costeoapp.feature.tiendas.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
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
class TiendaFlowTest {

	@get:Rule(order = 0)
	val hiltRule = HiltAndroidRule(this)

	@get:Rule(order = 1)
	val composeRule = createAndroidComposeRule<MainActivity>()

	@Before
	fun setup() {
		hiltRule.inject()
	}

	private fun navigateToTiendas() {
		composeRule.onNode(
			hasContentDescription("Tiendas"),
			useUnmergedTree = true
		).performClick()
		composeRule.waitForIdle()
	}

	@Test
	fun t1_app_inicia_con_dashboard() {
		composeRule.onNodeWithText("CosteoApp").assertIsDisplayed()
		composeRule.onNodeWithText("Calcula el costo real de tus platos").assertIsDisplayed()
	}

	@Test
	fun t2_navegacion_a_tiendas_muestra_seed_data() {
		navigateToTiendas()
		// Al menos una tienda del seed debe existir
		composeRule.onNodeWithText("Walmart").assertIsDisplayed()
	}

	@Test
	fun t3_navegacion_a_productos_funciona() {
		composeRule.onNode(
			hasContentDescription("Productos"),
			useUnmergedTree = true
		).performClick()
		composeRule.waitForIdle()
		// Verifica que estamos en la pantalla de productos (FAB visible)
		composeRule.onNodeWithContentDescription("Agregar producto").assertIsDisplayed()
	}

	@Test
	fun t4_crear_tienda_validacion_y_guardado() {
		navigateToTiendas()

		composeRule.onNodeWithContentDescription("Agregar tienda").performClick()
		composeRule.waitForIdle()
		composeRule.onNodeWithText("Nueva tienda").assertIsDisplayed()

		// Guardar sin nombre muestra error
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()
		composeRule.onNodeWithText("El nombre debe tener al menos 2 caracteres").assertIsDisplayed()

		// Llenar nombre y guardar
		composeRule.onNode(hasText("Nombre *")).performTextInput("Tienda Test Auto")
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Tienda Test Auto").assertIsDisplayed()
	}

	@Test
	fun t5_editar_tienda() {
		navigateToTiendas()

		// Crear tienda para editar
		composeRule.onNodeWithContentDescription("Agregar tienda").performClick()
		composeRule.waitForIdle()
		composeRule.onNode(hasText("Nombre *")).performTextInput("Para Editar")
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()

		// Tocar para editar
		composeRule.onNodeWithText("Para Editar").performClick()
		composeRule.waitForIdle()
		composeRule.onNodeWithText("Editar tienda").assertIsDisplayed()

		composeRule.onNode(hasText("Para Editar")).performTextClearance()
		composeRule.onNode(hasText("Nombre *")).performTextInput("Editada OK")
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Editada OK").assertIsDisplayed()
	}

	@Test
	fun t6_busqueda_filtra_y_limpia() {
		navigateToTiendas()

		composeRule.onNode(hasText("Buscar tienda...")).performTextInput("walmart")
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Walmart").assertIsDisplayed()
		composeRule.onNodeWithText("PriceSmart").assertDoesNotExist()

		composeRule.onNodeWithContentDescription("Limpiar").performClick()
		composeRule.waitForIdle()

		composeRule.onNodeWithText("Walmart").assertIsDisplayed()
		composeRule.onNodeWithText("PriceSmart").assertIsDisplayed()
	}

	@Test
	fun t7_soft_delete_tienda() {
		navigateToTiendas()

		// Crear tienda para eliminar
		composeRule.onNodeWithContentDescription("Agregar tienda").performClick()
		composeRule.waitForIdle()
		composeRule.onNode(hasText("Nombre *")).performTextInput("Para Borrar")
		composeRule.onNodeWithText("Guardar").performClick()
		composeRule.waitForIdle()
		composeRule.onNodeWithText("Para Borrar").assertIsDisplayed()

		// Click en eliminar (primer icono delete visible)
		composeRule.onAllNodesWithContentDescription("Eliminar").onFirst().performClick()
		composeRule.waitForIdle()

		// Confirmar en dialogo
		composeRule.onAllNodes(hasText("Eliminar")).onFirst().performClick()
		composeRule.waitForIdle()
	}
}
