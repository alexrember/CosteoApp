package com.mg.costeoapp.navigation

import kotlinx.serialization.Serializable

// --- Onboarding ---
@Serializable object OnboardingRoute

// --- Fase 0 ---
@Serializable object DashboardRoute

// --- Fase 1 ---
@Serializable object TiendaListRoute
@Serializable data class TiendaFormRoute(val tiendaId: Long? = null)
@Serializable object ProductoListRoute
@Serializable data class ProductoDetailRoute(val productoId: Long)
@Serializable data class ProductoFormRoute(val productoId: Long? = null)
@Serializable data class ProductoPrecioFormRoute(val productoId: Long)

// --- Fase 2 ---
@Serializable object InventarioListRoute
@Serializable object SeleccionTiendaCompraRoute
@Serializable object ScannerRoute
@Serializable object CarritoRoute
@Serializable data class ProductoRegistroRoute(val codigoBarras: String? = null)
@Serializable object RecetaListRoute
@Serializable data class RecetaDetailRoute(val recetaId: Long)
@Serializable data class RecetaFormRoute(val recetaId: Long? = null, val duplicadoDeId: Long? = null)
@Serializable object PlatoListRoute
@Serializable data class PlatoDetailRoute(val platoId: Long)
@Serializable data class PlatoFormRoute(val platoId: Long? = null)
@Serializable object SimuladorRoute
