package com.mg.costeoapp.feature.prefabricados.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun PrefabricadoListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: () -> Unit,
    viewModel: PrefabricadoListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToForm) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva receta")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CosteoSearchBar(
                query = uiState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                placeholder = "Buscar receta..."
            )

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.items.isEmpty() && uiState.searchQuery.isNotBlank() -> EmptyStateMessage(
                    message = "No se encontraron resultados para \"${uiState.searchQuery}\""
                )
                uiState.items.isEmpty() -> EmptyStateMessage(
                    message = "No hay recetas.\nCrea tu primera receta.",
                    actionLabel = "Crear receta",
                    onAction = onNavigateToForm
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.prefabricado.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.softDelete(item.prefabricado.id)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "\"${item.prefabricado.nombre}\" eliminado",
                                            actionLabel = "Deshacer",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restore(item.prefabricado.id)
                                        }
                                    }
                                }
                                false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Eliminar",
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { onNavigateToDetail(item.prefabricado.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Restaurant, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.prefabricado.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        item.prefabricado.descripcion?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    item.costoPorPorcion?.let { costo ->
                                        Text("${CurrencyFormatter.fromCents(costo)}/porcion",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (item.tieneAdvertencias) {
                                        Icon(Icons.Filled.Warning, contentDescription = "Advertencias",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
