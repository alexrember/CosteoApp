package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.Plato
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatoDao {

    @Query("SELECT * FROM platos WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllActive(): Flow<List<Plato>>

    @Query("SELECT * FROM platos WHERE id = :id")
    suspend fun getById(id: Long): Plato?

    @Query("SELECT * FROM platos WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<Plato?>

    @Query("""
        SELECT * FROM platos
        WHERE activo = 1 AND (LOWER(nombre) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(descripcion) LIKE '%' || LOWER(:query) || '%')
        ORDER BY nombre ASC
    """)
    fun search(query: String): Flow<List<Plato>>

    @Query("""
        SELECT DISTINCT p.* FROM platos p
        INNER JOIN plato_componente pc ON p.id = pc.plato_id
        WHERE pc.prefabricado_id = :prefabricadoId AND p.activo = 1
    """)
    suspend fun getPlatosQueUsanPrefabricado(prefabricadoId: Long): List<Plato>

    @Query("""
        SELECT DISTINCT p.* FROM platos p
        INNER JOIN plato_componente pc ON p.id = pc.plato_id
        WHERE pc.producto_id = :productoId AND p.activo = 1
    """)
    suspend fun getPlatosQueUsanProducto(productoId: Long): List<Plato>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plato: Plato): Long

    @Update
    suspend fun update(plato: Plato)

    @Query("UPDATE platos SET activo = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE platos SET activo = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM platos WHERE activo = 1")
    suspend fun countActive(): Int
}
