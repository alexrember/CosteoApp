package com.mg.costeoapp.core.domain.model

data class PropagacionResult(
    val productoId: Long,
    val productoNombre: String,
    val precioAnterior: Long,
    val precioNuevo: Long,
    val prefabricadosAfectados: List<PrefabricadoAfectado>,
    val platosAfectados: List<PlatoAfectado>,
    val timestamp: Long = System.currentTimeMillis()
)

data class PrefabricadoAfectado(
    val id: Long,
    val nombre: String,
    val costoAnterior: Long?,
    val costoNuevo: Long?,
    val diferencia: Long
)

data class PlatoAfectado(
    val id: Long,
    val nombre: String,
    val costoAnterior: Long?,
    val costoNuevo: Long?,
    val precioVentaAnterior: Long?,
    val precioVentaNuevo: Long?
)
