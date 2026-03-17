package com.mg.costeoapp.core.domain.engine

import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.domain.model.Advertencia
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.FuentePrecio
import com.mg.costeoapp.core.domain.model.PrecioResuelto
import javax.inject.Inject
import kotlin.math.roundToLong

interface PricingEngine {
    suspend fun resolvePrice(productoId: Long): PrecioResuelto
    suspend fun calculatePrefabricadoCost(prefabricadoId: Long): CosteoResult
}

class PricingEngineImpl @Inject constructor(
    private val inventarioDao: InventarioDao,
    private val productoTiendaDao: ProductoTiendaDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val ingredienteDao: PrefabricadoIngredienteDao
) : PricingEngine {

    override suspend fun resolvePrice(productoId: Long): PrecioResuelto {
        // 1. Buscar en inventario (FIFO)
        val stock = inventarioDao.getStockDisponible(productoId)
        if (stock != null) {
            return PrecioResuelto(
                productoId = productoId,
                productoNombre = "",
                precioUnitario = stock.precioCompra,
                fuente = FuentePrecio.INVENTARIO
            )
        }

        // 2. Buscar precio más reciente en producto_tienda
        val precioReciente = productoTiendaDao.getPrecioMasReciente(productoId)
        if (precioReciente != null) {
            return PrecioResuelto(
                productoId = productoId,
                productoNombre = "",
                precioUnitario = precioReciente.precio,
                fuente = FuentePrecio.PRECIO_RECIENTE
            )
        }

        // 3. Sin precio
        return PrecioResuelto(
            productoId = productoId,
            productoNombre = "",
            precioUnitario = null,
            fuente = FuentePrecio.SIN_PRECIO
        )
    }

    override suspend fun calculatePrefabricadoCost(prefabricadoId: Long): CosteoResult {
        val prefabricado = prefabricadoDao.getById(prefabricadoId)
            ?: return CosteoResult()

        val ingredientes = ingredienteDao.getByPrefabricadoOnce(prefabricadoId)
        val advertencias = mutableListOf<Advertencia>()
        val fuentesPrecio = mutableMapOf<Long, FuentePrecio>()
        var costoIngredientes = 0L

        for (item in ingredientes) {
            val producto = item.producto

            if (!producto.activo) {
                advertencias.add(Advertencia.IngredienteInactivo(producto.nombre))
            }

            val precio = resolvePrice(producto.id)
            fuentesPrecio[producto.id] = precio.fuente

            when (precio.fuente) {
                FuentePrecio.SIN_PRECIO -> {
                    advertencias.add(Advertencia.SinPrecio(producto.nombre))
                }
                FuentePrecio.PRECIO_RECIENTE -> {
                    advertencias.add(Advertencia.SinStock(producto.nombre))
                    costoIngredientes += calcularCostoIngrediente(
                        precioUnitario = precio.precioUnitario!!,
                        cantidadPorEmpaque = producto.cantidadPorEmpaque,
                        cantidadUsada = item.ingrediente.cantidadUsada,
                        factorMerma = producto.factorMerma
                    )
                }
                FuentePrecio.INVENTARIO -> {
                    costoIngredientes += calcularCostoIngrediente(
                        precioUnitario = precio.precioUnitario!!,
                        cantidadPorEmpaque = producto.cantidadPorEmpaque,
                        cantidadUsada = item.ingrediente.cantidadUsada,
                        factorMerma = producto.factorMerma
                    )
                }
            }
        }

        val costoTotal = prefabricado.costoFijo + costoIngredientes
        val costoPorPorcion = if (prefabricado.rendimientoPorciones > 0) {
            (costoTotal.toDouble() / prefabricado.rendimientoPorciones).roundToLong()
        } else null

        return CosteoResult(
            costoTotal = costoTotal,
            costoPorPorcion = costoPorPorcion,
            fuentesPrecio = fuentesPrecio,
            advertencias = advertencias
        )
    }

    private fun calcularCostoIngrediente(
        precioUnitario: Long,
        cantidadPorEmpaque: Double,
        cantidadUsada: Double,
        factorMerma: Int
    ): Long {
        if (cantidadPorEmpaque <= 0) return 0L
        val precioPorUnidad = precioUnitario.toDouble() / cantidadPorEmpaque
        val costo = precioPorUnidad * cantidadUsada
        return if (factorMerma in 1..99) {
            (costo / (1.0 - factorMerma / 100.0)).roundToLong()
        } else {
            costo.roundToLong()
        }
    }
}
