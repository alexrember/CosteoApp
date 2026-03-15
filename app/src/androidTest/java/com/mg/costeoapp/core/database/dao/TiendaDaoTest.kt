package com.mg.costeoapp.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.entity.Tienda
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TiendaDaoTest {

	private lateinit var database: CosteoDatabase
	private lateinit var tiendaDao: TiendaDao

	@Before
	fun setup() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		database = Room.inMemoryDatabaseBuilder(context, CosteoDatabase::class.java)
			.allowMainThreadQueries()
			.build()
		tiendaDao = database.tiendaDao()
	}

	@After
	fun teardown() {
		database.close()
	}

	@Test
	fun insert_y_getAll_retorna_tiendas_activas() = runTest {
		val id = tiendaDao.insert(Tienda(nombre = "Super Selectos"))
		assertTrue(id > 0)

		val tiendas = tiendaDao.getAll().first()
		assertEquals(1, tiendas.size)
		assertEquals("Super Selectos", tiendas[0].nombre)
	}

	@Test
	fun getById_retorna_tienda_correcta() = runTest {
		val id = tiendaDao.insert(Tienda(nombre = "La Despensa"))
		val tienda = tiendaDao.getById(id)
		assertNotNull(tienda)
		assertEquals("La Despensa", tienda!!.nombre)
	}

	@Test
	fun getByNombre_case_insensitive() = runTest {
		tiendaDao.insert(Tienda(nombre = "Mercado Central"))
		val result = tiendaDao.getByNombre("mercado central")
		assertNotNull(result)
		assertEquals("Mercado Central", result!!.nombre)
	}

	@Test
	fun softDelete_oculta_de_getAll() = runTest {
		val id = tiendaDao.insert(Tienda(nombre = "Tienda Test"))
		assertEquals(1, tiendaDao.getAll().first().size)

		tiendaDao.softDelete(id)
		assertEquals(0, tiendaDao.getAll().first().size)
	}

	@Test
	fun restore_despues_de_softDelete() = runTest {
		val id = tiendaDao.insert(Tienda(nombre = "Tienda Restore"))
		tiendaDao.softDelete(id)
		assertEquals(0, tiendaDao.getAll().first().size)

		tiendaDao.restore(id)
		assertEquals(1, tiendaDao.getAll().first().size)
	}

	@Test
	fun search_por_nombre() = runTest {
		tiendaDao.insert(Tienda(nombre = "Super Selectos"))
		tiendaDao.insert(Tienda(nombre = "La Despensa"))
		tiendaDao.insert(Tienda(nombre = "Mercado Central"))

		val results = tiendaDao.search("super").first()
		assertEquals(1, results.size)
		assertEquals("Super Selectos", results[0].nombre)
	}

	@Test
	fun getAll_ordenado_por_nombre() = runTest {
		tiendaDao.insert(Tienda(nombre = "Zapata"))
		tiendaDao.insert(Tienda(nombre = "Alpha"))
		tiendaDao.insert(Tienda(nombre = "Mercado"))

		val tiendas = tiendaDao.getAll().first()
		assertEquals("Alpha", tiendas[0].nombre)
		assertEquals("Mercado", tiendas[1].nombre)
		assertEquals("Zapata", tiendas[2].nombre)
	}

	@Test
	fun update_modifica_datos() = runTest {
		val id = tiendaDao.insert(Tienda(nombre = "Original"))
		val tienda = tiendaDao.getById(id)!!
		tiendaDao.update(tienda.copy(nombre = "Modificado"))

		val updated = tiendaDao.getById(id)
		assertEquals("Modificado", updated!!.nombre)
	}

	@Test
	fun unique_index_previene_nombre_duplicado() = runTest {
		tiendaDao.insert(Tienda(nombre = "Unica"))
		try {
			tiendaDao.insert(Tienda(nombre = "Unica"))
			assertTrue("Deberia lanzar excepcion", false)
		} catch (_: Exception) {
			assertTrue(true)
		}
	}
}
