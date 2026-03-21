package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.PlatoComponente
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatoComponenteDao {

    @Query("SELECT * FROM plato_componente WHERE plato_id = :platoId")
    suspend fun getByPlato(platoId: Long): List<PlatoComponente>

    @Query("SELECT * FROM plato_componente WHERE plato_id = :platoId")
    fun getByPlatoFlow(platoId: Long): Flow<List<PlatoComponente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(componentes: List<PlatoComponente>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(componente: PlatoComponente): Long

    @Update
    suspend fun update(componente: PlatoComponente)

    @Delete
    suspend fun delete(componente: PlatoComponente)

    @Query("DELETE FROM plato_componente WHERE plato_id = :platoId")
    suspend fun deleteByPlato(platoId: Long)

    @Query("SELECT * FROM plato_componente")
    suspend fun getAllOnce(): List<PlatoComponente>
}
