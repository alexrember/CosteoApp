package com.mg.costeoapp.feature.productos.data

import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.database.relation.PrecioConTienda
import com.mg.costeoapp.core.database.relation.ProductoConPrecios
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoRepositoryImpl @Inject constructor(
    private val productoDao: ProductoDao,
    private val productoTiendaDao: ProductoTiendaDao
) : ProductoRepository {

    override fun getAll(): Flow<List<Producto>> = productoDao.getAll()

    override suspend fun getById(id: Long): Producto? = productoDao.getById(id)

    override fun search(query: String): Flow<List<Producto>> = productoDao.search(query)

    override suspend fun getByCodigoBarras(codigoBarras: String): Producto? =
        productoDao.getByCodigoBarras(codigoBarras)

    override suspend fun getProductoConPrecios(id: Long): ProductoConPrecios? =
        productoDao.getProductoConPrecios(id)

    override fun getPreciosConTienda(productoId: Long): Flow<List<PrecioConTienda>> =
        productoTiendaDao.getPreciosConTiendaNombre(productoId).map { tuples ->
            tuples.map { t ->
                PrecioConTienda(
                    productoTienda = ProductoTienda(
                        id = t.id,
                        productoId = t.productoId,
                        tiendaId = t.tiendaId,
                        precio = t.precio,
                        fechaRegistro = t.fechaRegistro,
                        activo = t.activo,
                        createdAt = t.createdAt,
                        updatedAt = t.updatedAt
                    ),
                    tiendaNombre = t.tiendaNombre
                )
            }
        }

    override suspend fun getPrecioMasReciente(productoId: Long): ProductoTienda? =
        productoTiendaDao.getPrecioMasReciente(productoId)

    override suspend fun insert(producto: Producto): Result<Long> {
        return try {
            Result.success(productoDao.insert(producto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(producto: Producto): Result<Unit> {
        return try {
            productoDao.update(producto.copy(updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun softDelete(id: Long) = productoDao.softDelete(id)

    override suspend fun restore(id: Long) = productoDao.restore(id)

    override suspend fun insertPrecio(productoTienda: ProductoTienda): Result<Long> {
        return try {
            Result.success(productoTiendaDao.insert(productoTienda))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePrecio(productoTienda: ProductoTienda): Result<Unit> {
        return try {
            productoTiendaDao.update(productoTienda)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun desactivarPrecios(productoId: Long, tiendaId: Long) =
        productoTiendaDao.desactivarPrecios(productoId, tiendaId)
}
