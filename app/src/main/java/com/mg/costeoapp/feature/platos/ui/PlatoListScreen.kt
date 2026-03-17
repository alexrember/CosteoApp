package com.mg.costeoapp.feature.platos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.ui.components.CosteoSearchBar
import com.mg.costeoapp.core.ui.components.EmptyStateMessage
import com.mg.costeoapp.core.ui.components.LoadingIndicator
import com.mg.costeoapp.core.util.CurrencyFormatter

@Composable
fun PlatoListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: () -> Unit,
    viewModel: PlatoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToForm) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo plato")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CosteoSearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                placeholder = "Buscar plato..."
            )
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.items.isEmpty() && uiState.searchQuery.isNotBlank() -> EmptyStateMessage(
                    message = "No se encontraron resultados para \"${uiState.searchQuery}\""
                )
                uiState.items.isEmpty() -> EmptyStateMessage(
                    message = "No hay platos.\nCrea tu primer plato.",
                    actionLabel = "Crear plato",
                    onAction = onNavigateToForm
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.plato.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onNavigateToDetail(item.plato.id) }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.plato.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    item.costoTotal?.let {
                                        Text("Costo: ${CurrencyFormatter.fromCents(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                item.precioVenta?.let {
                                    Text(CurrencyFormatter.fromCents(it), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (item.tieneAdvertencias) {
                                    Icon(Icons.Filled.Warning, contentDescription = "Advertencias", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
