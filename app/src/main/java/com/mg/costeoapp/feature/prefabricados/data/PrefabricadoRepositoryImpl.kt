package com.mg.costeoapp.feature.prefabricados.data

import androidx.room.withTransaction
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.relation.PrefabricadoConIngredientes
import com.mg.costeoapp.core.domain.engine.NutricionEngine
import com.mg.costeoapp.core.domain.engine.PricingEngine
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.NutricionResumen
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefabricadoRepositoryImpl @Inject constructor(
    private val db: CosteoDatabase,
    private val prefabricadoDao: PrefabricadoDao,
    private val ingredienteDao: PrefabricadoIngredienteDao,
    private val pricingEngine: PricingEngine,
    private val nutricionEngine: NutricionEngine
) : PrefabricadoRepository {

    override fun getAll(activo: Boolean): Flow<List<Prefabricado>> =
        prefabricadoDao.getAll(activo)

    override suspend fun getConIngredientes(id: Long): PrefabricadoConIngredientes? =
        prefabricadoDao.getConIngredientes(id)

    override fun observeConIngredientes(id: Long): Flow<PrefabricadoConIngredientes?> =
        prefabricadoDao.observeConIngredientes(id)

    override fun getVariantes(prefabricadoId: Long): Flow<List<Prefabricado>> =
        prefabricadoDao.getVariantes(prefabricadoId)

    override fun search(query: String): Flow<List<Prefabricado>> =
        prefabricadoDao.search(query)

    override suspend fun create(
        prefabricado: Prefabricado,
        ingredientes: List<PrefabricadoIngrediente>
    ): Result<Long> = try {
        val id = db.withTransaction {
            val prefId = prefabricadoDao.insert(prefabricado)
            val mapped = ingredientes.map { it.copy(prefabricadoId = prefId) }
            ingredienteDao.insertAll(mapped)
            prefId
        }
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun update(
        prefabricado: Prefabricado,
        ingredientes: List<PrefabricadoIngrediente>
    ): Result<Unit> = try {
        db.withTransaction {
            prefabricadoDao.update(prefabricado.copy(updatedAt = System.currentTimeMillis()))
            ingredienteDao.deleteByPrefabricado(prefabricado.id)
            val mapped = ingredientes.map { it.copy(prefabricadoId = prefabricado.id) }
            ingredienteDao.insertAll(mapped)
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun softDelete(id: Long) = prefabricadoDao.softDelete(id)

    override suspend fun restore(id: Long) = prefabricadoDao.restore(id)

    override suspend fun duplicar(sourceId: Long, nuevoNombre: String): Result<Long> = try {
        val source = prefabricadoDao.getConIngredientes(sourceId)
            ?: return Result.failure(IllegalArgumentException("Receta no encontrada"))

        val id = db.withTransaction {
            val newPref = source.prefabricado.copy(
                id = 0,
                nombre = nuevoNombre,
                duplicadoDe = sourceId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val prefId = prefabricadoDao.insert(newPref)

            val newIngredientes = source.ingredientes.map {
                it.ingrediente.copy(id = 0, prefabricadoId = prefId)
            }
            ingredienteDao.insertAll(newIngredientes)
            prefId
        }
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun calculateCost(prefabricadoId: Long): CosteoResult =
        pricingEngine.calculatePrefabricadoCost(prefabricadoId)

    override suspend fun calculateNutricion(prefabricadoId: Long): NutricionResumen =
        nutricionEngine.calculateNutricionPrefabricado(prefabricadoId)
}
