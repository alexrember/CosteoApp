package com.mg.costeoapp.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.feature.search.GlobalSearchBar

@Composable
fun DashboardScreen(
    onNavigateToCompras: () -> Unit,
    onNavigateToNuevaReceta: () -> Unit,
    onNavigateToNuevoPlato: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSimulador: () -> Unit,
    onNavigateToProductoDetail: (Long) -> Unit = {},
    onNavigateToRecetaDetail: (Long) -> Unit = {},
    onNavigateToPlatoDetail: (Long) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.propagacionNotificaciones.collect { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CosteoApp",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Calcula el costo real de tus platos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GlobalSearchBar(
                    onProductoSelected = onNavigateToProductoDetail,
                    onRecetaSelected = onNavigateToRecetaDetail,
                    onPlatoSelected = onNavigateToPlatoDetail
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Productos",
                        count = uiState.totalProductos,
                        icon = Icons.Filled.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Recetas",
                        count = uiState.totalRecetas,
                        icon = Icons.Filled.Restaurant,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Platos",
                        count = uiState.totalPlatos,
                        icon = Icons.Filled.Fastfood,
                        modifier = Modifier.weight(1f)
                    )
                }

                val hayAlertas = uiState.productosSinPrecio > 0 ||
                    uiState.productosConMermaAlta > 0 ||
                    uiState.productosConStockBajo > 0 ||
                    uiState.recetasConIngredientesInactivos > 0 ||
                    uiState.costoMermaMensual > 0

                if (hayAlertas) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Alertas",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.productosSinPrecio > 0) {
                        AlertCard(
                            message = "${uiState.productosSinPrecio} productos sin precio registrado"
                        )
                    }
                    if (uiState.productosConMermaAlta > 0) {
                        AlertCard(
                            message = "${uiState.productosConMermaAlta} productos con merma alta (>15%)"
                        )
                    }
                    if (uiState.productosConStockBajo > 0) {
                        AlertCard(
                            message = "${uiState.productosConStockBajo} productos con stock bajo"
                        )
                    }
                    if (uiState.recetasConIngredientesInactivos > 0) {
                        AlertCard(
                            message = "${uiState.recetasConIngredientesInactivos} recetas con ingredientes inactivos"
                        )
                    }
                    if (uiState.costoMermaMensual > 0) {
                        AlertCard(
                            message = "Costo estimado de merma mensual: ${CurrencyFormatter.fromCents(uiState.costoMermaMensual)}"
                        )
                    }
                }

                if (uiState.topPlatosCaros.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RankingSection(
                        title = "Top 5 platos mas caros",
                        icon = Icons.Filled.TrendingUp,
                        items = uiState.topPlatosCaros
                    )
                }

                if (uiState.topPlatosBaratos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RankingSection(
                        title = "Top 5 platos mas baratos",
                        icon = Icons.Filled.TrendingDown,
                        items = uiState.topPlatosBaratos
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Accesos rapidos",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            QuickActionButton(
                text = "Ir de compras",
                icon = Icons.Filled.ShoppingCart,
                onClick = onNavigateToCompras
            )
            QuickActionButton(
                text = "Nueva receta",
                icon = Icons.Filled.Restaurant,
                onClick = onNavigateToNuevaReceta
            )
            QuickActionButton(
                text = "Nuevo plato",
                icon = Icons.Filled.Fastfood,
                onClick = onNavigateToNuevoPlato
            )
            QuickActionButton(
                text = "Simulador",
                icon = Icons.Filled.Calculate,
                onClick = onNavigateToSimulador
            )
        }
    }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun AlertCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RankingSection(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, Long>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, (nombre, costo) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. $nombre",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = CurrencyFormatter.fromCents(costo),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
