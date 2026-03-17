package com.mg.costeoapp.feature.platos.data

import androidx.room.withTransaction
import com.mg.costeoapp.core.database.CosteoDatabase
import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.domain.engine.NutricionEngine
import com.mg.costeoapp.core.domain.engine.PricingEngine
import com.mg.costeoapp.core.domain.model.Advertencia
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.FuentePrecio
import com.mg.costeoapp.core.domain.model.NutricionResumen
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class PlatoRepositoryImpl @Inject constructor(
    private val db: CosteoDatabase,
    private val platoDao: PlatoDao,
    private val componenteDao: PlatoComponenteDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val productoDao: ProductoDao,
    private val pricingEngine: PricingEngine,
    private val nutricionEngine: NutricionEngine
) : PlatoRepository {

    override fun getAllActive(): Flow<List<Plato>> = platoDao.getAllActive()

    override fun search(query: String): Flow<List<Plato>> = platoDao.search(query)

    override suspend fun getById(id: Long): Plato? = platoDao.getById(id)

    override suspend fun getComponentes(platoId: Long): List<PlatoComponente> =
        componenteDao.getByPlato(platoId)

    override fun getComponentesFlow(platoId: Long): Flow<List<PlatoComponente>> =
        componenteDao.getByPlatoFlow(platoId)

    override suspend fun createPlato(plato: Plato, componentes: List<PlatoComponente>): Result<Long> =
        try {
            val id = db.withTransaction {
                val platoId = platoDao.insert(plato)
                val mapped = componentes.map { it.copy(platoId = platoId) }
                componenteDao.insertAll(mapped)
                platoId
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updatePlato(plato: Plato, componentes: List<PlatoComponente>): Result<Unit> =
        try {
            db.withTransaction {
                platoDao.update(plato.copy(updatedAt = System.currentTimeMillis()))
                componenteDao.deleteByPlato(plato.id)
                val mapped = componentes.map { it.copy(platoId = plato.id) }
                componenteDao.insertAll(mapped)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun softDelete(id: Long) = platoDao.softDelete(id)

    override suspend fun restore(id: Long) = platoDao.restore(id)

    override suspend fun calculateCost(platoId: Long): CosteoResult {
        val componentes = componenteDao.getByPlato(platoId)
        val advertencias = mutableListOf<Advertencia>()
        val fuentesPrecio = mutableMapOf<Long, FuentePrecio>()
        var costoTotal = 0L

        for (comp in componentes) {
            when {
                comp.prefabricadoId != null -> {
                    val costeoReceta = pricingEngine.calculatePrefabricadoCost(comp.prefabricadoId)
                    val costoPorPorcion = costeoReceta.costoPorPorcion ?: 0L
                    costoTotal += (costoPorPorcion * comp.cantidad).roundToLong()
                    advertencias.addAll(costeoReceta.advertencias)
                }
                comp.productoId != null -> {
                    val precio = pricingEngine.resolvePrice(comp.productoId)
                    fuentesPrecio[comp.productoId] = precio.fuente
                    if (precio.precioUnitario != null) {
                        val producto = productoDao.getById(comp.productoId)
                        val cantPorEmpaque = producto?.cantidadPorEmpaque ?: 1.0
                        val ppu = precio.precioUnitario.toDouble() / cantPorEmpaque
                        costoTotal += (ppu * comp.cantidad).roundToLong()
                    } else {
                        val nombre = productoDao.getById(comp.productoId)?.nombre ?: "?"
                        advertencias.add(Advertencia.SinPrecio(nombre))
                    }
                }
            }
        }

        val plato = platoDao.getById(platoId)
        return CosteoResult(
            costoTotal = costoTotal,
            costoPorPorcion = costoTotal,
            fuentesPrecio = fuentesPrecio,
            advertencias = advertencias
        )
    }

    override suspend fun calculateNutricion(platoId: Long): NutricionResumen {
        // Delegamos nutrición de componentes prefabricados al engine
        // Para productos directos, calculamos aquí
        return NutricionResumen() // Simplificado por ahora
    }

    override suspend fun calculatePrecioVenta(plato: Plato, costoTotal: Long): Long? {
        if (plato.precioVentaManual != null) return plato.precioVentaManual
        val margen = plato.margenPorcentaje ?: return null
        if (margen <= 0 || margen > 100) return null
        return (costoTotal.toDouble() / (margen / 100.0)).roundToLong()
    }

    override suspend fun getComponentesDetalle(platoId: Long): List<ComponenteDetalle> {
        val componentes = componenteDao.getByPlato(platoId)
        return componentes.map { comp ->
            when {
                comp.prefabricadoId != null -> {
                    val pref = prefabricadoDao.getById(comp.prefabricadoId)
                    val costeo = pricingEngine.calculatePrefabricadoCost(comp.prefabricadoId)
                    val costoPorPorcion = costeo.costoPorPorcion ?: 0L
                    ComponenteDetalle(
                        componente = comp,
                        nombre = pref?.nombre ?: "Receta eliminada",
                        tipo = TipoComponente.PREFABRICADO,
                        costoUnitario = costoPorPorcion,
                        costoTotal = (costoPorPorcion * comp.cantidad).roundToLong()
                    )
                }
                comp.productoId != null -> {
                    val producto = productoDao.getById(comp.productoId)
                    val precio = pricingEngine.resolvePrice(comp.productoId)
                    val ppu = if (precio.precioUnitario != null && producto != null) {
                        (precio.precioUnitario.toDouble() / producto.cantidadPorEmpaque).roundToLong()
                    } else null
                    ComponenteDetalle(
                        componente = comp,
                        nombre = producto?.nombre ?: "Producto eliminado",
                        tipo = TipoComponente.PRODUCTO,
                        costoUnitario = ppu,
                        costoTotal = ppu?.let { (it * comp.cantidad).roundToLong() }
                    )
                }
                else -> ComponenteDetalle(comp, "Componente invalido", TipoComponente.PRODUCTO, null, null)
            }
        }
    }
}
