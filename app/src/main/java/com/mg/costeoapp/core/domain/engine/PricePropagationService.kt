package com.mg.costeoapp.core.domain.engine

import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio de propagacion de precios.
 * Actualmente los costos se recalculan on-the-fly cuando se abren las pantallas.
 * La propagacion en tiempo real con notificaciones se implementara en una fase futura.
 * TODO: Implementar costoAnterior para mostrar diferencias de precio.
 */
@Singleton
class PricePropagationService @Inject constructor(
    private val productoDao: ProductoDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val platoDao: PlatoDao,
    private val platoComponenteDao: PlatoComponenteDao,
    private val pricingEngine: PricingEngine
)
