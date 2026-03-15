package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.ProductoTienda
import kotlinx.coroutines.flow.Flow

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
        SELECT pt.* FROM producto_tienda pt
        INNER JOIN tiendas t ON pt.tienda_id = t.id
        WHERE pt.producto_id = :productoId AND pt.activo = 1 AND t.activo = 1
        ORDER BY pt.fecha_registro DESC
    """)
    fun getPreciosActivosConTiendaActiva(productoId: Long): Flow<List<ProductoTienda>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(productoTienda: ProductoTienda): Long

    @Update
    suspend fun update(productoTienda: ProductoTienda)

    @Query("""
        UPDATE producto_tienda
        SET activo = 0
        WHERE producto_id = :productoId AND tienda_id = :tiendaId
    """)
    suspend fun desactivarPrecios(productoId: Long, tiendaId: Long)
}
