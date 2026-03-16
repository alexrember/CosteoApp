package com.mg.costeoapp.feature.inventario.data

import androidx.room.withTransaction
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.dao.CarritoTemporalDao
import com.mg.costeoapp.core.database.dao.InventarioConDetalles
import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.ResumenInventarioTienda
import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.database.entity.ProductoTienda
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventarioRepositoryImpl @Inject constructor(
    private val db: CosteoDatabase,
    private val inventarioDao: InventarioDao,
    private val productoTiendaDao: ProductoTiendaDao,
    private val carritoDao: CarritoTemporalDao
) : InventarioRepository {

    override fun getInventarioDisponible(): Flow<List<InventarioConDetalles>> =
        inventarioDao.getInventarioDisponible()

    override fun getByProducto(productoId: Long): Flow<List<InventarioConDetalles>> =
        inventarioDao.getByProducto(productoId)

    override suspend fun getStockDisponible(productoId: Long): Inventario? =
        inventarioDao.getStockDisponible(productoId)

    override suspend fun getCantidadTotalDisponible(productoId: Long): Double =
        inventarioDao.getCantidadTotalDisponible(productoId)

    override fun getResumenPorTienda(): Flow<List<ResumenInventarioTienda>> =
        inventarioDao.getResumenPorTienda()

    override fun buscarInventario(query: String): Flow<List<InventarioConDetalles>> =
        inventarioDao.buscarInventario(query)

    override suspend fun getById(id: Long): InventarioConDetalles? =
        inventarioDao.getById(id)

    override suspend fun confirmarCompra(items: List<Inventario>): Result<List<Long>> =
        try {
            val ids = db.withTransaction {
                val insertedIds = mutableListOf<Long>()
                for (item in items) {
                    val id = inventarioDao.insert(item)
                    insertedIds.add(id)
                    // Solo insertar precio si no existe uno para esta compra
                    val precioExistente = productoTiendaDao.getPrecioActivo(item.productoId, item.tiendaId)
                    if (precioExistente == null || precioExistente.precio != item.precioCompra) {
                        try {
                            productoTiendaDao.insert(
                                ProductoTienda(
                                    productoId = item.productoId,
                                    tiendaId = item.tiendaId,
                                    precio = item.precioCompra,
                                    fechaRegistro = item.fechaCompra
                                )
                            )
                        } catch (_: Exception) {
                            // Duplicate key — precio ya registrado, ignorar
                        }
                    }
                }
                carritoDao.deleteAll()
                insertedIds
            }
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun insert(inventario: Inventario): Result<Long> =
        try {
            Result.success(inventarioDao.insert(inventario))
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun descontarInventario(inventarioId: Long, cantidad: Double): Result<Unit> =
        try {
            inventarioDao.descontarInventario(inventarioId, cantidad)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun marcarAgotado(inventarioId: Long): Result<Unit> =
        try {
            inventarioDao.marcarAgotado(inventarioId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun softDelete(inventarioId: Long): Result<Unit> =
        try {
            inventarioDao.softDelete(inventarioId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
}
