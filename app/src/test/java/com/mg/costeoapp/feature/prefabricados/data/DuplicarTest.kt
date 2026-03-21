package com.mg.costeoapp.feature.prefabricados.data

import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DuplicarTest {

	private val originalId = 42L

	private val original = Prefabricado(
		id = originalId,
		nombre = "Salsa roja",
		descripcion = "Salsa casera",
		rendimientoPorciones = 4.0,
		costoFijo = 100,
		createdAt = 1000L,
		updatedAt = 1000L
	)

	private val ingredientes = listOf(
		PrefabricadoIngrediente(id = 1, prefabricadoId = originalId, productoId = 10, cantidadUsada = 200.0, unidadUsada = "g"),
		PrefabricadoIngrediente(id = 2, prefabricadoId = originalId, productoId = 20, cantidadUsada = 50.0, unidadUsada = "ml"),
		PrefabricadoIngrediente(id = 3, prefabricadoId = originalId, productoId = 30, cantidadUsada = 3.0, unidadUsada = "unidad")
	)

	private fun simulateDuplicar(
		source: Prefabricado,
		sourceIngredientes: List<PrefabricadoIngrediente>,
		nuevoNombre: String,
		newId: Long
	): Pair<Prefabricado, List<PrefabricadoIngrediente>> {
		val newPref = source.copy(
			id = newId,
			nombre = nuevoNombre,
			duplicadoDe = source.id,
			createdAt = System.currentTimeMillis(),
			updatedAt = System.currentTimeMillis()
		)
		val newIngredientes = sourceIngredientes.map {
			it.copy(id = 0, prefabricadoId = newId)
		}
		return newPref to newIngredientes
	}

	@Test
	fun copiaContieneIgualCantidadIngredientes() {
		val (_, copiaIngredientes) = simulateDuplicar(original, ingredientes, "Salsa roja (copia)", 99)
		assertEquals(3, copiaIngredientes.size)
	}

	@Test
	fun copiaIdDiferenteAlOriginal() {
		val (copia, _) = simulateDuplicar(original, ingredientes, "Salsa roja (copia)", 99)
		assertNotEquals(original.id, copia.id)
	}

	@Test
	fun copiaTieneDuplicadoDeConIdOriginal() {
		val (copia, _) = simulateDuplicar(original, ingredientes, "Salsa roja (copia)", 99)
		assertEquals(originalId, copia.duplicadoDe)
	}

	@Test
	fun copiaNombreDiferenteAlOriginal() {
		val nuevoNombre = "Salsa roja (copia)"
		val (copia, _) = simulateDuplicar(original, ingredientes, nuevoNombre, 99)
		assertNotEquals(original.nombre, copia.nombre)
		assertEquals(nuevoNombre, copia.nombre)
	}

	@Test
	fun ingredientesCopiadosConNuevoPrefabricadoId() {
		val newId = 99L
		val (_, copiaIngredientes) = simulateDuplicar(original, ingredientes, "Copia", newId)
		copiaIngredientes.forEach { ing ->
			assertEquals(newId, ing.prefabricadoId)
		}
	}

	@Test
	fun ingredientesCopiadosConIdCero() {
		val (_, copiaIngredientes) = simulateDuplicar(original, ingredientes, "Copia", 99)
		copiaIngredientes.forEach { ing ->
			assertEquals(0L, ing.id)
		}
	}

	@Test
	fun ingredientesConservanProductoIdYCantidades() {
		val (_, copiaIngredientes) = simulateDuplicar(original, ingredientes, "Copia", 99)
		assertEquals(10L, copiaIngredientes[0].productoId)
		assertEquals(200.0, copiaIngredientes[0].cantidadUsada, 0.001)
		assertEquals(20L, copiaIngredientes[1].productoId)
		assertEquals(50.0, copiaIngredientes[1].cantidadUsada, 0.001)
	}

	@Test
	fun copiaConservaRendimientoYCostoFijo() {
		val (copia, _) = simulateDuplicar(original, ingredientes, "Copia", 99)
		assertEquals(original.rendimientoPorciones, copia.rendimientoPorciones, 0.001)
		assertEquals(original.costoFijo, copia.costoFijo)
	}
}
