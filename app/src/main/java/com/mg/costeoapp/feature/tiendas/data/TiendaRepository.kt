package com.mg.costeoapp.feature.tiendas.data

import com.mg.costeoapp.core.database.entity.Tienda
import kotlinx.coroutines.flow.Flow

interface TiendaRepository {
    fun getAll(): Flow<List<Tienda>>
    suspend fun getById(id: Long): Tienda?
    fun search(query: String): Flow<List<Tienda>>
    suspend fun insert(tienda: Tienda): Result<Long>
    suspend fun update(tienda: Tienda): Result<Unit>
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
}
