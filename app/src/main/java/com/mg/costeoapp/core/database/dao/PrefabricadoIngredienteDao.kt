package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.relation.IngredienteConProducto
import kotlinx.coroutines.flow.Flow

@Dao
interface PrefabricadoIngredienteDao {

    @Transaction
    @Query("SELECT * FROM prefabricado_ingrediente WHERE prefabricado_id = :prefabricadoId")
    fun getByPrefabricado(prefabricadoId: Long): Flow<List<IngredienteConProducto>>

    @Transaction
    @Query("SELECT * FROM prefabricado_ingrediente WHERE prefabricado_id = :prefabricadoId")
    suspend fun getByPrefabricadoOnce(prefabricadoId: Long): List<IngredienteConProducto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredientes: List<PrefabricadoIngrediente>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingrediente: PrefabricadoIngrediente): Long

    @Update
    suspend fun update(ingrediente: PrefabricadoIngrediente)

    @Delete
    suspend fun delete(ingrediente: PrefabricadoIngrediente)

    @Query("DELETE FROM prefabricado_ingrediente WHERE prefabricado_id = :prefabricadoId")
    suspend fun deleteByPrefabricado(prefabricadoId: Long)

    @Query("""
        SELECT COUNT(DISTINCT pi.prefabricado_id) FROM prefabricado_ingrediente pi
        INNER JOIN productos p ON pi.producto_id = p.id
        WHERE p.activo = 0
    """)
    suspend fun countRecetasConIngredientesInactivos(): Int

    @Query("SELECT * FROM prefabricado_ingrediente")
    suspend fun getAllOnce(): List<PrefabricadoIngrediente>

    @Query("SELECT * FROM prefabricado_ingrediente WHERE updated_at > :since")
    suspend fun getModifiedSince(since: Long): List<PrefabricadoIngrediente>
}
