package com.mg.costeoapp.feature.tiendas.data

import android.database.sqlite.SQLiteConstraintException
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.Tienda
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TiendaRepositoryImpl @Inject constructor(
    private val tiendaDao: TiendaDao
) : TiendaRepository {

    override fun getAll(): Flow<List<Tienda>> = tiendaDao.getAll()

    override suspend fun getById(id: Long): Tienda? = tiendaDao.getById(id)

    override fun search(query: String): Flow<List<Tienda>> = tiendaDao.search(query)

    override suspend fun insert(tienda: Tienda): Result<Long> {
        val existing = tiendaDao.getByNombre(tienda.nombre)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Ya existe una tienda con ese nombre"))
        }
        return try {
            Result.success(tiendaDao.insert(tienda))
        } catch (e: SQLiteConstraintException) {
            Result.failure(IllegalArgumentException("Ya existe una tienda con ese nombre"))
        }
    }

    override suspend fun update(tienda: Tienda): Result<Unit> {
        val existing = tiendaDao.getByNombre(tienda.nombre)
        if (existing != null && existing.id != tienda.id) {
            return Result.failure(IllegalArgumentException("Ya existe otra tienda con ese nombre"))
        }
        return try {
            tiendaDao.update(tienda.copy(updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            Result.failure(IllegalArgumentException("Ya existe otra tienda con ese nombre"))
        }
    }

    override suspend fun softDelete(id: Long) = tiendaDao.softDelete(id)

    override suspend fun restore(id: Long) = tiendaDao.restore(id)
}
