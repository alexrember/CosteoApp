package com.mg.costeoapp.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.entity.Producto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductoDaoTest {

	private lateinit var database: CosteoDatabase
	private lateinit var productoDao: ProductoDao

	@Before
	fun setup() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		database = Room.inMemoryDatabaseBuilder(context, CosteoDatabase::class.java)
			.allowMainThreadQueries()
			.build()
		productoDao = database.productoDao()
	}

	@After
	fun teardown() {
		database.close()
	}

	private fun testProducto(
		nombre: String = "Arroz",
		unidadMedida: String = "lb",
		cantidadPorEmpaque: Double = 1.0
	) = Producto(nombre = nombre, unidadMedida = unidadMedida, cantidadPorEmpaque = cantidadPorEmpaque)

	@Test
	fun insert_y_getAll() = runTest {
		val id = productoDao.insert(testProducto())
		assertTrue(id > 0)

		val productos = productoDao.getAll().first()
		assertEquals(1, productos.size)
		assertEquals("Arroz", productos[0].nombre)
	}

	@Test
	fun getById_retorna_producto() = runTest {
		val id = productoDao.insert(testProducto(nombre = "Frijoles"))
		val producto = productoDao.getById(id)
		assertNotNull(producto)
		assertEquals("Frijoles", producto!!.nombre)
	}

	@Test
	fun getByCodigoBarras() = runTest {
		productoDao.insert(testProducto().copy(codigoBarras = "7501234567890"))
		val result = productoDao.getByCodigoBarras("7501234567890")
		assertNotNull(result)
		assertEquals("Arroz", result!!.nombre)
	}

	@Test
	fun getByCodigoBarras_no_retorna_inactivo() = runTest {
		val id = productoDao.insert(testProducto().copy(codigoBarras = "999"))
		productoDao.softDelete(id)
		val result = productoDao.getByCodigoBarras("999")
		assertNull(result)
	}

	@Test
	fun softDelete_oculta_producto() = runTest {
		val id = productoDao.insert(testProducto())
		assertEquals(1, productoDao.getAll().first().size)

		productoDao.softDelete(id)
		assertEquals(0, productoDao.getAll().first().size)
	}

	@Test
	fun search_por_nombre() = runTest {
		productoDao.insert(testProducto(nombre = "Arroz Gallo Rojo"))
		productoDao.insert(testProducto(nombre = "Frijoles Rojos"))
		productoDao.insert(testProducto(nombre = "Aceite Orisol"))

		val results = productoDao.search("rojo").first()
		assertEquals(2, results.size)
	}

	@Test
	fun search_por_codigo_barras() = runTest {
		productoDao.insert(testProducto().copy(codigoBarras = "ABC123"))
		productoDao.insert(testProducto(nombre = "Otro"))

		val results = productoDao.search("ABC").first()
		assertEquals(1, results.size)
	}

	@Test
	fun update_modifica_producto() = runTest {
		val id = productoDao.insert(testProducto())
		val producto = productoDao.getById(id)!!
		productoDao.update(producto.copy(nombre = "Arroz Integral"))

		assertEquals("Arroz Integral", productoDao.getById(id)!!.nombre)
	}

	@Test
	fun getAll_ordenado_por_nombre() = runTest {
		productoDao.insert(testProducto(nombre = "Zucaritas"))
		productoDao.insert(testProducto(nombre = "Aceite"))
		productoDao.insert(testProducto(nombre = "Mantequilla"))

		val productos = productoDao.getAll().first()
		assertEquals("Aceite", productos[0].nombre)
		assertEquals("Mantequilla", productos[1].nombre)
		assertEquals("Zucaritas", productos[2].nombre)
	}
}
