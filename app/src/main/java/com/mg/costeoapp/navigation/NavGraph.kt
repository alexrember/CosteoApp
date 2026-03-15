package com.mg.costeoapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mg.costeoapp.core.ui.components.BottomNavItem
import com.mg.costeoapp.core.ui.components.CosteoBottomNavBar
import com.mg.costeoapp.feature.dashboard.ui.DashboardScreen
import com.mg.costeoapp.feature.productos.ui.ProductoDetailScreen
import com.mg.costeoapp.feature.productos.ui.ProductoFormScreen
import com.mg.costeoapp.feature.productos.ui.ProductoListScreen
import com.mg.costeoapp.feature.productos.ui.ProductoPrecioFormScreen
import com.mg.costeoapp.feature.tiendas.ui.TiendaFormScreen
import com.mg.costeoapp.feature.tiendas.ui.TiendaListScreen

private val bottomNavItems = listOf(
    BottomNavItem("Inicio", Icons.Filled.Home, DashboardRoute),
    BottomNavItem("Tiendas", Icons.Filled.Store, TiendaListRoute),
    BottomNavItem("Productos", Icons.Filled.Inventory2, ProductoListRoute)
)

private val bottomNavRoutes = setOf(
    DashboardRoute::class.qualifiedName,
    TiendaListRoute::class.qualifiedName,
    ProductoListRoute::class.qualifiedName
)

@Composable
fun CosteoNavGraph(
    navController: NavHostController = rememberNavController()
) {
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
            startDestination = DashboardRoute,
            modifier = Modifier.padding(padding)
        ) {
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
        }
    }
}
