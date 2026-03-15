package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.Inventario
import kotlinx.coroutines.flow.Flow

data class InventarioConDetalles(
    val id: Long,
    val productoId: Long,
    val tiendaId: Long,
    val cantidad: Double,
    val precioCompra: Long,
    val fechaCompra: Long,
    val agotado: Boolean,
    val activo: Boolean,
    val productoNombre: String,
    val unidadMedida: String,
    val tiendaNombre: String
)

data class ResumenInventarioTienda(
    val tiendaId: Long,
    val tiendaNombre: String,
    val totalItems: Int,
    val valorTotal: Long
)

@Dao
interface InventarioDao {

    @Query("""
        SELECT i.id, i.producto_id AS productoId, i.tienda_id AS tiendaId,
               i.cantidad, i.precio_compra AS precioCompra, i.fecha_compra AS fechaCompra,
               i.agotado, i.activo,
               p.nombre AS productoNombre, p.unidad_medida AS unidadMedida,
               t.nombre AS tiendaNombre
        FROM inventario i
        INNER JOIN productos p ON i.producto_id = p.id
        INNER JOIN tiendas t ON i.tienda_id = t.id
        WHERE i.agotado = 0 AND i.activo = 1
        ORDER BY i.fecha_compra DESC
    """)
    fun getInventarioDisponible(): Flow<List<InventarioConDetalles>>

    @Query("""
        SELECT i.id, i.producto_id AS productoId, i.tienda_id AS tiendaId,
               i.cantidad, i.precio_compra AS precioCompra, i.fecha_compra AS fechaCompra,
               i.agotado, i.activo,
               p.nombre AS productoNombre, p.unidad_medida AS unidadMedida,
               t.nombre AS tiendaNombre
        FROM inventario i
        INNER JOIN productos p ON i.producto_id = p.id
        INNER JOIN tiendas t ON i.tienda_id = t.id
        WHERE i.producto_id = :productoId AND i.agotado = 0 AND i.activo = 1
        ORDER BY i.fecha_compra ASC
    """)
    fun getByProducto(productoId: Long): Flow<List<InventarioConDetalles>>

    @Query("""
        SELECT * FROM inventario
        WHERE producto_id = :productoId AND agotado = 0 AND activo = 1
        ORDER BY fecha_compra ASC
        LIMIT 1
    """)
    suspend fun getStockDisponible(productoId: Long): Inventario?

    @Query("""
        SELECT COALESCE(SUM(cantidad), 0.0) FROM inventario
        WHERE producto_id = :productoId AND agotado = 0 AND activo = 1
    """)
    suspend fun getCantidadTotalDisponible(productoId: Long): Double

    @Query("""
        SELECT t.id AS tiendaId, t.nombre AS tiendaNombre,
               COUNT(i.id) AS totalItems,
               SUM(i.precio_compra) AS valorTotal
        FROM inventario i
        INNER JOIN tiendas t ON i.tienda_id = t.id
        WHERE i.agotado = 0 AND i.activo = 1
        GROUP BY t.id
        ORDER BY valorTotal DESC
    """)
    fun getResumenPorTienda(): Flow<List<ResumenInventarioTienda>>

    @Query("""
        SELECT i.id, i.producto_id AS productoId, i.tienda_id AS tiendaId,
               i.cantidad, i.precio_compra AS precioCompra, i.fecha_compra AS fechaCompra,
               i.agotado, i.activo,
               p.nombre AS productoNombre, p.unidad_medida AS unidadMedida,
               t.nombre AS tiendaNombre
        FROM inventario i
        INNER JOIN productos p ON i.producto_id = p.id
        INNER JOIN tiendas t ON i.tienda_id = t.id
        WHERE i.agotado = 0 AND i.activo = 1
        AND LOWER(p.nombre) LIKE '%' || LOWER(:query) || '%'
        ORDER BY i.fecha_compra DESC
    """)
    fun buscarInventario(query: String): Flow<List<InventarioConDetalles>>

    @Query("""
        SELECT i.id, i.producto_id AS productoId, i.tienda_id AS tiendaId,
               i.cantidad, i.precio_compra AS precioCompra, i.fecha_compra AS fechaCompra,
               i.agotado, i.activo,
               p.nombre AS productoNombre, p.unidad_medida AS unidadMedida,
               t.nombre AS tiendaNombre
        FROM inventario i
        INNER JOIN productos p ON i.producto_id = p.id
        INNER JOIN tiendas t ON i.tienda_id = t.id
        WHERE i.id = :id
    """)
    suspend fun getById(id: Long): InventarioConDetalles?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registrarCompra(items: List<Inventario>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(inventario: Inventario): Long

    @Query("""
        UPDATE inventario
        SET cantidad = MAX(cantidad - :cantidadDescontar, 0),
            agotado = CASE WHEN (cantidad - :cantidadDescontar) <= 0 THEN 1 ELSE 0 END,
            updated_at = :timestamp
        WHERE id = :inventarioId AND activo = 1
    """)
    suspend fun descontarInventario(inventarioId: Long, cantidadDescontar: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventario SET agotado = 1, updated_at = :timestamp WHERE id = :inventarioId")
    suspend fun marcarAgotado(inventarioId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventario SET activo = 0, updated_at = :timestamp WHERE id = :inventarioId")
    suspend fun softDelete(inventarioId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE inventario SET activo = 1, updated_at = :timestamp WHERE id = :inventarioId")
    suspend fun restaurar(inventarioId: Long, timestamp: Long = System.currentTimeMillis())

    @Update
    suspend fun update(inventario: Inventario)
}
