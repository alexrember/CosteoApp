package com.mg.costeoapp.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
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
class ProductoTiendaDaoTest {

	private lateinit var database: CosteoDatabase
	private lateinit var tiendaDao: TiendaDao
	private lateinit var productoDao: ProductoDao
	private lateinit var productoTiendaDao: ProductoTiendaDao

	private var tiendaId: Long = 0
	private var tienda2Id: Long = 0
	private var productoId: Long = 0

	@Before
	fun setup() = runTest {
		val context = ApplicationProvider.getApplicationContext<Context>()
		database = Room.inMemoryDatabaseBuilder(context, CosteoDatabase::class.java)
			.allowMainThreadQueries()
			.build()
		tiendaDao = database.tiendaDao()
		productoDao = database.productoDao()
		productoTiendaDao = database.productoTiendaDao()

		tiendaId = tiendaDao.insert(Tienda(nombre = "Super Selectos"))
		tienda2Id = tiendaDao.insert(Tienda(nombre = "La Despensa"))
		productoId = productoDao.insert(
			Producto(nombre = "Arroz", unidadMedida = "lb", cantidadPorEmpaque = 1.0)
		)
	}

	@After
	fun teardown() {
		database.close()
	}

	@Test
	fun insert_precio_y_getPreciosByProducto() = runTest {
		val id = productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tiendaId, precio = 150)
		)
		assertTrue(id > 0)

		val precios = productoTiendaDao.getPreciosByProducto(productoId).first()
		assertEquals(1, precios.size)
		assertEquals(150L, precios[0].precio)
	}

	@Test
	fun getPrecioMasReciente_retorna_ultimo() = runTest {
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tiendaId, precio = 100, fechaRegistro = 1000)
		)
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tienda2Id, precio = 200, fechaRegistro = 2000)
		)

		val reciente = productoTiendaDao.getPrecioMasReciente(productoId)
		assertNotNull(reciente)
		assertEquals(200L, reciente!!.precio)
	}

	@Test
	fun getPrecioActivo() = runTest {
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tiendaId, precio = 350)
		)
		val precio = productoTiendaDao.getPrecioActivo(productoId, tiendaId)
		assertNotNull(precio)
		assertEquals(350L, precio!!.precio)
	}

	@Test
	fun desactivarPrecios_oculta_precios() = runTest {
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tiendaId, precio = 100)
		)
		assertEquals(1, productoTiendaDao.getPreciosByProducto(productoId).first().size)

		productoTiendaDao.desactivarPrecios(productoId, tiendaId)
		assertEquals(0, productoTiendaDao.getPreciosByProducto(productoId).first().size)
	}

	@Test
	fun getPreciosActivosConTiendaActiva_filtra_tienda_inactiva() = runTest {
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tiendaId, precio = 100)
		)
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tienda2Id, precio = 200)
		)

		assertEquals(2, productoTiendaDao.getPreciosActivosConTiendaActiva(productoId).first().size)

		tiendaDao.softDelete(tienda2Id)
		assertEquals(1, productoTiendaDao.getPreciosActivosConTiendaActiva(productoId).first().size)
	}

	@Test
	fun precios_multiples_tiendas() = runTest {
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tiendaId, precio = 100)
		)
		productoTiendaDao.insert(
			ProductoTienda(productoId = productoId, tiendaId = tienda2Id, precio = 150)
		)

		val precios = productoTiendaDao.getPreciosByProducto(productoId).first()
		assertEquals(2, precios.size)
	}
}
