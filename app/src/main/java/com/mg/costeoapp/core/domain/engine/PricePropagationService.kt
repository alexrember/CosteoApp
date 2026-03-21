package com.mg.costeoapp.core.domain.engine

import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricePropagationService @Inject constructor(
    private val productoDao: ProductoDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val platoDao: PlatoDao,
    private val platoComponenteDao: PlatoComponenteDao,
    private val pricingEngine: PricingEngine
) {
    private val _notificaciones = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val notificaciones: SharedFlow<String> = _notificaciones.asSharedFlow()

    suspend fun onPriceChanged(productoId: Long, precioNuevo: Long) {
        val producto = productoDao.getById(productoId) ?: return

        val recetasAfectadas = prefabricadoDao.getPrefabricadosQueUsanProducto(productoId)

        val platosDirectos = platoDao.getPlatosQueUsanProducto(productoId)
        val platosViaReceta = recetasAfectadas.flatMap { receta ->
            platoDao.getPlatosQueUsanPrefabricado(receta.id)
        }
        val platosAfectados = (platosDirectos + platosViaReceta).distinctBy { it.id }

        val totalRecetas = recetasAfectadas.size
        val totalPlatos = platosAfectados.size

        if (totalRecetas > 0 || totalPlatos > 0) {
            val partes = mutableListOf<String>()
            if (totalRecetas > 0) partes.add("$totalRecetas receta${if (totalRecetas > 1) "s" else ""}")
            if (totalPlatos > 0) partes.add("$totalPlatos plato${if (totalPlatos > 1) "s" else ""}")
            _notificaciones.emit("Precio de \"${producto.nombre}\" actualizado. Afecta ${partes.joinToString(" y ")}.")
        }
    }
}
