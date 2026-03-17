package com.mg.costeoapp.core.domain.engine

import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.domain.model.PlatoAfectado
import com.mg.costeoapp.core.domain.model.PrefabricadoAfectado
import com.mg.costeoapp.core.domain.model.PropagacionResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class PricePropagationService @Inject constructor(
    private val productoDao: ProductoDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val platoDao: PlatoDao,
    private val platoComponenteDao: PlatoComponenteDao,
    private val pricingEngine: PricingEngine
) {
    private val _propagaciones = MutableSharedFlow<PropagacionResult>(replay = 1)
    val propagaciones: SharedFlow<PropagacionResult> = _propagaciones.asSharedFlow()

    suspend fun onPriceChanged(productoId: Long, precioAnterior: Long, precioNuevo: Long) {
        val producto = productoDao.getById(productoId) ?: return

        // 1. Buscar prefabricados afectados
        val prefsAfectados = prefabricadoDao.getPrefabricadosQueUsanProducto(productoId)
        val prefabricadosResult = prefsAfectados.map { pref ->
            val costoNuevo = pricingEngine.calculatePrefabricadoCost(pref.id)
            PrefabricadoAfectado(
                id = pref.id,
                nombre = pref.nombre,
                costoAnterior = null,
                costoNuevo = costoNuevo.costoPorPorcion,
                diferencia = 0
            )
        }

        // 2. Buscar platos afectados directamente (usan el producto)
        val platosDirectos = platoDao.getPlatosQueUsanProducto(productoId)

        // 3. Buscar platos afectados indirectamente (usan un prefabricado afectado)
        val platosIndirectos = prefsAfectados.flatMap { pref ->
            platoDao.getPlatosQueUsanPrefabricado(pref.id)
        }

        val todosPlatos = (platosDirectos + platosIndirectos).distinctBy { it.id }

        val platosResult = todosPlatos.map { plato ->
            val componentes = platoComponenteDao.getByPlato(plato.id)
            var costoTotal = 0L
            for (comp in componentes) {
                when {
                    comp.prefabricadoId != null -> {
                        val costeo = pricingEngine.calculatePrefabricadoCost(comp.prefabricadoId)
                        costoTotal += ((costeo.costoPorPorcion ?: 0L) * comp.cantidad).roundToLong()
                    }
                    comp.productoId != null -> {
                        val precio = pricingEngine.resolvePrice(comp.productoId)
                        if (precio.precioUnitario != null) {
                            val prod = productoDao.getById(comp.productoId)
                            val ppu = precio.precioUnitario.toDouble() / (prod?.cantidadPorEmpaque ?: 1.0)
                            costoTotal += (ppu * comp.cantidad).roundToLong()
                        }
                    }
                }
            }
            val costeoNuevo = com.mg.costeoapp.core.domain.model.CosteoResult(costoTotal = costoTotal)
            val margen = plato.margenPorcentaje
            val precioVentaNuevo = if (margen != null && margen > 0 && margen < 100) {
                (costeoNuevo.costoTotal.toDouble() / (1.0 - margen / 100.0)).roundToLong()
            } else plato.precioVentaManual

            PlatoAfectado(
                id = plato.id,
                nombre = plato.nombre,
                costoAnterior = null,
                costoNuevo = costeoNuevo.costoTotal,
                precioVentaAnterior = null,
                precioVentaNuevo = precioVentaNuevo
            )
        }

        val result = PropagacionResult(
            productoId = productoId,
            productoNombre = producto.nombre,
            precioAnterior = precioAnterior,
            precioNuevo = precioNuevo,
            prefabricadosAfectados = prefabricadosResult,
            platosAfectados = platosResult
        )

        _propagaciones.emit(result)
    }
}
