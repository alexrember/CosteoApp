package com.mg.costeoapp.feature.platos.data

import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.NutricionResumen
import kotlinx.coroutines.flow.Flow

data class ComponenteDetalle(
    val componente: PlatoComponente,
    val nombre: String,
    val tipo: TipoComponente,
    val costoUnitario: Long?,
    val costoTotal: Long?
)

enum class TipoComponente { PREFABRICADO, PRODUCTO }

data class PlatoConDetalle(
    val plato: Plato,
    val componentes: List<PlatoComponente>,
    val costeo: CosteoResult,
    val nutricion: NutricionResumen,
    val precioVenta: Long?
)

interface PlatoRepository {
    fun getAllActive(): Flow<List<Plato>>
    fun search(query: String): Flow<List<Plato>>
    suspend fun getById(id: Long): Plato?
    suspend fun getComponentes(platoId: Long): List<PlatoComponente>
    fun getComponentesFlow(platoId: Long): Flow<List<PlatoComponente>>
    suspend fun createPlato(plato: Plato, componentes: List<PlatoComponente>): Result<Long>
    suspend fun updatePlato(plato: Plato, componentes: List<PlatoComponente>): Result<Unit>
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun calculateCost(platoId: Long): CosteoResult
    suspend fun calculateNutricion(platoId: Long): NutricionResumen
    suspend fun calculatePrecioVenta(plato: Plato, costoTotal: Long): Long?
    suspend fun getComponentesDetalle(platoId: Long): List<ComponenteDetalle>
}
