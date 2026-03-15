package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.core.database.dao.InventarioConDetalles
import com.mg.costeoapp.core.database.dao.ResumenInventarioTienda
import com.mg.costeoapp.core.database.entity.Inventario
import kotlinx.coroutines.flow.Flow

interface InventarioRepository {
    fun getInventarioDisponible(): Flow<List<InventarioConDetalles>>
    fun getByProducto(productoId: Long): Flow<List<InventarioConDetalles>>
    suspend fun getStockDisponible(productoId: Long): Inventario?
    suspend fun getCantidadTotalDisponible(productoId: Long): Double
    fun getResumenPorTienda(): Flow<List<ResumenInventarioTienda>>
    fun buscarInventario(query: String): Flow<List<InventarioConDetalles>>
    suspend fun getById(id: Long): InventarioConDetalles?
    suspend fun confirmarCompra(items: List<Inventario>): Result<List<Long>>
    suspend fun insert(inventario: Inventario): Result<Long>
    suspend fun descontarInventario(inventarioId: Long, cantidad: Double): Result<Unit>
    suspend fun marcarAgotado(inventarioId: Long): Result<Unit>
    suspend fun softDelete(inventarioId: Long): Result<Unit>
}
