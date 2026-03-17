package com.mg.costeoapp.feature.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.components.EmptyStateMessage
import com.mg.costeoapp.core.ui.components.LoadingIndicator
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.DateFormatter

@Composable
fun HistorialPreciosScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistorialPreciosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = "Historial de precios",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        if (uiState.precios.isEmpty()) {
            EmptyStateMessage(
                message = "No hay historial de precios para ${uiState.productoNombre}",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // Nombre del producto
            item {
                Text(uiState.productoNombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Resumen de precios
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.precioMin?.let {
                        PrecioResumenCard("Minimo", it, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                    uiState.precioMax?.let {
                        PrecioResumenCard("Maximo", it, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    }
                    uiState.precioActual?.let {
                        PrecioResumenCard("Actual", it, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lista de precios
            item {
                Text("Detalle", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.precios) { precio ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(precio.tiendaNombre, style = MaterialTheme.typography.bodyMedium)
                        Text(DateFormatter.formatDate(precio.fechaRegistro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(CurrencyFormatter.fromCents(precio.precio),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PrecioResumenCard(
    label: String,
    precio: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(CurrencyFormatter.fromCents(precio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
