package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.ProductoTienda
import kotlinx.coroutines.flow.Flow

data class PrecioHistoricoRaw(
    val id: Long,
    val productoId: Long,
    val tiendaId: Long,
    val tiendaNombre: String,
    val precio: Long,
    val fechaRegistro: Long
)

data class PrecioConTiendaTuple(
    val id: Long,
    val productoId: Long,
    val tiendaId: Long,
    val precio: Long,
    val fechaRegistro: Long,
    val activo: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val tiendaNombre: String
)

@Dao
interface ProductoTiendaDao {

    @Query("""
        SELECT * FROM producto_tienda
        WHERE producto_id = :productoId AND activo = 1
        ORDER BY fecha_registro DESC
    """)
    fun getPreciosByProducto(productoId: Long): Flow<List<ProductoTienda>>

    @Query("""
        SELECT * FROM producto_tienda
        WHERE producto_id = :productoId AND tienda_id = :tiendaId AND activo = 1
        ORDER BY fecha_registro DESC
        LIMIT 1
    """)
    suspend fun getPrecioActivo(productoId: Long, tiendaId: Long): ProductoTienda?

    @Query("""
        SELECT * FROM producto_tienda
        WHERE producto_id = :productoId AND activo = 1
        ORDER BY fecha_registro DESC
        LIMIT 1
    """)
    suspend fun getPrecioMasReciente(productoId: Long): ProductoTienda?

    @Query("""
        SELECT pt.id, pt.producto_id AS productoId, pt.tienda_id AS tiendaId,
               pt.precio, pt.fecha_registro AS fechaRegistro, pt.activo,
               pt.created_at AS createdAt, pt.updated_at AS updatedAt,
               t.nombre AS tiendaNombre
        FROM producto_tienda pt
        INNER JOIN tiendas t ON pt.tienda_id = t.id
        WHERE pt.producto_id = :productoId AND pt.activo = 1 AND t.activo = 1
        ORDER BY pt.fecha_registro DESC
    """)
    fun getPreciosConTiendaNombre(productoId: Long): Flow<List<PrecioConTiendaTuple>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(productoTienda: ProductoTienda): Long

    @Update
    suspend fun update(productoTienda: ProductoTienda)

    @Query("""
        UPDATE producto_tienda
        SET activo = 0, updated_at = :timestamp
        WHERE producto_id = :productoId AND tienda_id = :tiendaId
    """)
    suspend fun desactivarPrecios(productoId: Long, tiendaId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT pt.id, pt.producto_id AS productoId, pt.tienda_id AS tiendaId,
               t.nombre AS tiendaNombre, pt.precio, pt.fecha_registro AS fechaRegistro
        FROM producto_tienda pt
        INNER JOIN tiendas t ON pt.tienda_id = t.id
        WHERE pt.producto_id = :productoId AND pt.activo = 1 AND t.activo = 1
        ORDER BY pt.fecha_registro DESC
    """)
    suspend fun getHistorialPrecios(productoId: Long): List<PrecioHistoricoRaw>

    @Query("""
        SELECT pt.id, pt.producto_id AS productoId, pt.tienda_id AS tiendaId,
               t.nombre AS tiendaNombre, pt.precio, pt.fecha_registro AS fechaRegistro
        FROM producto_tienda pt
        INNER JOIN tiendas t ON pt.tienda_id = t.id
        WHERE pt.producto_id = :productoId AND pt.activo = 1 AND t.activo = 1
        ORDER BY pt.fecha_registro DESC
        LIMIT 5
    """)
    suspend fun getHistorialPreciosRecientes(productoId: Long): List<PrecioHistoricoRaw>
}
