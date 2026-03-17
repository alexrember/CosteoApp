package com.mg.costeoapp.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mg.costeoapp.core.ui.components.BottomNavItem
import com.mg.costeoapp.core.ui.components.CosteoBottomNavBar
import com.mg.costeoapp.feature.dashboard.ui.DashboardScreen
import com.mg.costeoapp.feature.inventario.ui.CarritoScreen
import com.mg.costeoapp.feature.inventario.ui.InventarioListScreen
import com.mg.costeoapp.feature.inventario.ui.ProductoRegistroScreen
import com.mg.costeoapp.feature.inventario.ui.ScannerScreen
import com.mg.costeoapp.feature.inventario.ui.SeleccionTiendaScreen
import com.mg.costeoapp.feature.onboarding.ui.OnboardingScreen
import com.mg.costeoapp.feature.prefabricados.ui.PrefabricadoDetailScreen
import com.mg.costeoapp.feature.prefabricados.ui.PrefabricadoFormScreen
import com.mg.costeoapp.feature.prefabricados.ui.PrefabricadoListScreen
import com.mg.costeoapp.feature.productos.ui.ProductoDetailScreen
import com.mg.costeoapp.feature.productos.ui.ProductoFormScreen
import com.mg.costeoapp.feature.productos.ui.ProductoListScreen
import com.mg.costeoapp.feature.productos.ui.ProductoPrecioFormScreen
import com.mg.costeoapp.feature.platos.ui.PlatoDetailScreen
import com.mg.costeoapp.feature.platos.ui.PlatoFormScreen
import com.mg.costeoapp.feature.platos.ui.PlatoListScreen
import com.mg.costeoapp.feature.platos.ui.SimuladorScreen
import com.mg.costeoapp.feature.tiendas.ui.TiendaFormScreen
import com.mg.costeoapp.feature.tiendas.ui.TiendaListScreen

private val bottomNavItems = listOf(
    BottomNavItem("Inicio", Icons.Filled.Home, DashboardRoute),
    BottomNavItem("Tiendas", Icons.Filled.Store, TiendaListRoute),
    BottomNavItem("Productos", Icons.Filled.Inventory2, ProductoListRoute),
    BottomNavItem("Inventario", Icons.Filled.ShoppingCart, InventarioListRoute),
    BottomNavItem("Recetas", Icons.Filled.Restaurant, RecetaListRoute),
    BottomNavItem("Platos", Icons.Filled.Fastfood, PlatoListRoute)
)

private val bottomNavRoutes = setOf(
    DashboardRoute::class.qualifiedName,
    TiendaListRoute::class.qualifiedName,
    ProductoListRoute::class.qualifiedName,
    InventarioListRoute::class.qualifiedName,
    RecetaListRoute::class.qualifiedName,
    PlatoListRoute::class.qualifiedName
)

private const val PREFS_NAME = "costeo_prefs"
private const val KEY_ONBOARDING_COMPLETADO = "onboarding_completado"

private fun isOnboardingCompleted(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ONBOARDING_COMPLETADO, false)
}

private fun setOnboardingCompleted(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETADO, true).apply()
}

@Composable
fun CosteoNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val startDestination: Any = if (isOnboardingCompleted(context)) DashboardRoute else OnboardingRoute

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavRoutes.any { routeName ->
        currentRoute?.contains(routeName ?: "") == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CosteoBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = navBackStackEntry?.destination?.let { dest ->
                        bottomNavItems.find { item ->
                            dest.hasRoute(item.route::class)
                        }?.route?.let { it::class.qualifiedName }
                    },
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(DashboardRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onFinish = {
                        setOnboardingCompleted(context)
                        navController.navigate(DashboardRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateToTiendaForm = {
                        navController.navigate(TiendaFormRoute())
                    },
                    onNavigateToProductoForm = {
                        navController.navigate(ProductoFormRoute())
                    }
                )
            }

            composable<TiendaListRoute> {
                TiendaListScreen(
                    onNavigateToForm = { tiendaId ->
                        navController.navigate(TiendaFormRoute(tiendaId = tiendaId))
                    }
                )
            }

            composable<TiendaFormRoute> {
                TiendaFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ProductoListRoute> {
                ProductoListScreen(
                    onNavigateToDetail = { productoId ->
                        navController.navigate(ProductoDetailRoute(productoId = productoId))
                    },
                    onNavigateToForm = {
                        navController.navigate(ProductoFormRoute())
                    }
                )
            }

            composable<ProductoDetailRoute> {
                ProductoDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { productoId ->
                        navController.navigate(ProductoFormRoute(productoId = productoId))
                    },
                    onNavigateToAddPrecio = { productoId ->
                        navController.navigate(ProductoPrecioFormRoute(productoId = productoId))
                    }
                )
            }

            composable<ProductoFormRoute> {
                ProductoFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ProductoPrecioFormRoute> {
                ProductoPrecioFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // --- Fase 2: Inventario + Compras ---

            composable<InventarioListRoute> {
                InventarioListScreen(
                    onNavigateToScanner = {
                        navController.navigate(SeleccionTiendaCompraRoute)
                    },
                    onContinuarCompra = {
                        navController.navigate(ScannerRoute)
                    }
                )
            }

            composable<SeleccionTiendaCompraRoute> {
                SeleccionTiendaScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTiendaSeleccionada = {
                        navController.navigate(ScannerRoute) {
                            popUpTo(SeleccionTiendaCompraRoute) { inclusive = true }
                        }
                    }
                )
            }

            composable<ScannerRoute> {
                ScannerScreen(
                    onNavigateBack = {
                        // Volver al inventario — el carrito se mantiene en Room
                        navController.popBackStack(InventarioListRoute, inclusive = false)
                    },
                    onNavigateToRegistro = { barcode ->
                        navController.navigate(ProductoRegistroRoute(codigoBarras = barcode))
                    },
                    onNavigateToCarrito = {
                        navController.navigate(CarritoRoute)
                    }
                )
            }

            composable<ProductoRegistroRoute> {
                ProductoRegistroScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRegistroExitoso = {
                        // Volver al scanner para seguir escaneando
                        navController.popBackStack()
                    }
                )
            }

            composable<CarritoRoute> {
                CarritoScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = { navController.popBackStack() },
                    onCompraConfirmada = {
                        navController.popBackStack(InventarioListRoute, inclusive = false)
                    }
                )
            }

            // --- Fase 3: Prefabricados (Recetas) ---

            composable<RecetaListRoute> {
                PrefabricadoListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(RecetaDetailRoute(recetaId = id))
                    },
                    onNavigateToForm = {
                        navController.navigate(RecetaFormRoute())
                    }
                )
            }

            composable<RecetaDetailRoute> {
                PrefabricadoDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(RecetaFormRoute(recetaId = id))
                    },
                    onNavigateToDuplicate = { id ->
                        navController.navigate(RecetaFormRoute(duplicadoDeId = id))
                    }
                )
            }

            composable<RecetaFormRoute> {
                PrefabricadoFormScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCreated = { newId ->
                        navController.navigate(RecetaDetailRoute(recetaId = newId)) {
                            popUpTo(RecetaListRoute) { inclusive = false }
                        }
                    }
                )
            }

            // --- Fase 4: Platos ---

            composable<PlatoListRoute> {
                PlatoListScreen(
                    onNavigateToDetail = { id ->
                        navController.navigate(PlatoDetailRoute(platoId = id))
                    },
                    onNavigateToForm = {
                        navController.navigate(PlatoFormRoute())
                    }
                )
            }

            composable<PlatoDetailRoute> {
                PlatoDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(PlatoFormRoute(platoId = id))
                    }
                )
            }

            composable<PlatoFormRoute> {
                PlatoFormScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCreated = { newId ->
                        navController.navigate(PlatoDetailRoute(platoId = newId)) {
                            popUpTo(PlatoListRoute) { inclusive = false }
                        }
                    }
                )
            }

            // --- Fase 4: Simulador ---

            composable<SimuladorRoute> {
                SimuladorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
