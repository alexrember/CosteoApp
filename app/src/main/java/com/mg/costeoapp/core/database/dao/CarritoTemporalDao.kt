package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mg.costeoapp.core.database.entity.CarritoTemporal

data class CarritoItemConDetalles(
    val id: Long,
    val productoId: Long,
    val tiendaId: Long,
    val cantidad: Double,
    val precioUnitario: Long,
    val productoNombre: String,
    val unidadMedida: String,
    val cantidadPorEmpaque: Double,
    val unidadesPorEmpaque: Int,
    val tiendaNombre: String
)

@Dao
interface CarritoTemporalDao {

    @Query("""
        SELECT ct.id, ct.producto_id AS productoId, ct.tienda_id AS tiendaId,
               ct.cantidad, ct.precio_unitario AS precioUnitario,
               p.nombre AS productoNombre, p.unidad_medida AS unidadMedida,
               p.cantidad_por_empaque AS cantidadPorEmpaque,
               p.unidades_por_empaque AS unidadesPorEmpaque,
               t.nombre AS tiendaNombre
        FROM carrito_temporal ct
        INNER JOIN productos p ON ct.producto_id = p.id
        INNER JOIN tiendas t ON ct.tienda_id = t.id
        ORDER BY ct.id ASC
    """)
    suspend fun getAll(): List<CarritoItemConDetalles>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CarritoTemporal): Long

    @Query("UPDATE carrito_temporal SET cantidad = :cantidad WHERE id = :id")
    suspend fun updateCantidad(id: Long, cantidad: Double)

    @Query("DELETE FROM carrito_temporal WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM carrito_temporal")
    suspend fun deleteAll()

    @Query("SELECT * FROM carrito_temporal WHERE producto_id = :productoId LIMIT 1")
    suspend fun getByProductoId(productoId: Long): CarritoTemporal?

    @Query("SELECT tienda_id FROM carrito_temporal LIMIT 1")
    suspend fun getTiendaId(): Long?
}
