package com.mg.costeoapp.feature.prefabricados.data

import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.relation.PrefabricadoConIngredientes
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.NutricionResumen
import kotlinx.coroutines.flow.Flow

interface PrefabricadoRepository {
    fun getAll(activo: Boolean = true): Flow<List<Prefabricado>>
    suspend fun getConIngredientes(id: Long): PrefabricadoConIngredientes?
    fun observeConIngredientes(id: Long): Flow<PrefabricadoConIngredientes?>
    fun getVariantes(prefabricadoId: Long): Flow<List<Prefabricado>>
    fun search(query: String): Flow<List<Prefabricado>>
    suspend fun create(prefabricado: Prefabricado, ingredientes: List<PrefabricadoIngrediente>): Result<Long>
    suspend fun update(prefabricado: Prefabricado, ingredientes: List<PrefabricadoIngrediente>): Result<Unit>
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun duplicar(sourceId: Long, nuevoNombre: String): Result<Long>
    suspend fun calculateCost(prefabricadoId: Long): CosteoResult
    suspend fun calculateNutricion(prefabricadoId: Long): NutricionResumen
}
