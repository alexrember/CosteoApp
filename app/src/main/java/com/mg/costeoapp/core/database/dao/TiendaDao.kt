package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.Tienda
import kotlinx.coroutines.flow.Flow

@Dao
interface TiendaDao {

    @Query("SELECT * FROM tiendas WHERE activo = 1 ORDER BY nombre ASC")
    fun getAll(): Flow<List<Tienda>>

    @Query("SELECT * FROM tiendas ORDER BY nombre ASC")
    fun getAllIncludingInactive(): Flow<List<Tienda>>

    @Query("SELECT * FROM tiendas WHERE id = :id")
    suspend fun getById(id: Long): Tienda?

    @Query("SELECT * FROM tiendas WHERE LOWER(nombre) = LOWER(:nombre) AND activo = 1 LIMIT 1")
    suspend fun getByNombre(nombre: String): Tienda?

    @Query("""
        SELECT * FROM tiendas
        WHERE activo = 1
        AND LOWER(nombre) LIKE '%' || LOWER(:query) || '%'
        ORDER BY nombre ASC
    """)
    fun search(query: String): Flow<List<Tienda>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tienda: Tienda): Long

    @Update
    suspend fun update(tienda: Tienda)

    @Query("UPDATE tiendas SET activo = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tiendas SET activo = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM tiendas WHERE activo = 1")
    suspend fun countActive(): Int

    @Query("SELECT * FROM tiendas WHERE updated_at > :since")
    suspend fun getModifiedSince(since: Long): List<Tienda>

    @Query("SELECT * FROM tiendas")
    suspend fun getAllOnce(): List<Tienda>
}
