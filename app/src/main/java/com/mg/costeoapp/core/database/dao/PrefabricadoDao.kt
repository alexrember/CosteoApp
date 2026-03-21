package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.relation.PrefabricadoConIngredientes
import kotlinx.coroutines.flow.Flow

@Dao
interface PrefabricadoDao {

    @Query("SELECT * FROM prefabricados WHERE activo = :activo ORDER BY nombre ASC")
    fun getAll(activo: Boolean = true): Flow<List<Prefabricado>>

    @Query("SELECT * FROM prefabricados WHERE id = :id")
    suspend fun getById(id: Long): Prefabricado?

    @Transaction
    @Query("SELECT * FROM prefabricados WHERE id = :id")
    suspend fun getConIngredientes(id: Long): PrefabricadoConIngredientes?

    @Transaction
    @Query("SELECT * FROM prefabricados WHERE id = :id")
    fun observeConIngredientes(id: Long): Flow<PrefabricadoConIngredientes?>

    @Query("SELECT * FROM prefabricados WHERE duplicado_de = :prefabricadoId AND activo = 1")
    fun getVariantes(prefabricadoId: Long): Flow<List<Prefabricado>>

    @Query("""
        SELECT * FROM prefabricados
        WHERE activo = 1 AND LOWER(nombre) LIKE '%' || LOWER(:query) || '%'
        ORDER BY nombre ASC
    """)
    fun search(query: String): Flow<List<Prefabricado>>

    @Query("""
        SELECT DISTINCT p.* FROM prefabricados p
        INNER JOIN prefabricado_ingrediente pi ON p.id = pi.prefabricado_id
        WHERE pi.producto_id = :productoId AND p.activo = 1
    """)
    suspend fun getPrefabricadosQueUsanProducto(productoId: Long): List<Prefabricado>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(prefabricado: Prefabricado): Long

    @Update
    suspend fun update(prefabricado: Prefabricado)

    @Query("UPDATE prefabricados SET activo = 0, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE prefabricados SET activo = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM prefabricados WHERE activo = 1")
    suspend fun countActive(): Int

    @Query("SELECT * FROM prefabricados WHERE updated_at > :since")
    suspend fun getModifiedSince(since: Long): List<Prefabricado>

    @Query("SELECT * FROM prefabricados")
    suspend fun getAllOnce(): List<Prefabricado>
}
