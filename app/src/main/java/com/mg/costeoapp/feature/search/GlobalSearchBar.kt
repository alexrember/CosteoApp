package com.mg.costeoapp.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchBar(
    onProductoSelected: (Long) -> Unit,
    onRecetaSelected: (Long) -> Unit,
    onPlatoSelected: (Long) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!uiState.isActive) {
        IconButton(onClick = viewModel::onActivate) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar")
        }
        return
    }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChanged,
                onSearch = {},
                expanded = true,
                onExpandedChange = { if (!it) viewModel.onDismiss() },
                placeholder = { Text("Buscar productos, recetas, platos...") },
                leadingIcon = {
                    IconButton(onClick = viewModel::onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                    }
                }
            )
        },
        expanded = true,
        onExpandedChange = { if (!it) viewModel.onDismiss() }
    ) {
        if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.results.isEmpty() && uiState.query.length >= 2) {
            Text(
                "Sin resultados para \"${uiState.query}\"",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.results) { item ->
                    when (item) {
                        is SearchResultItem.ProductoResult -> {
                            ListItem(
                                headlineContent = { Text(item.producto.nombre) },
                                supportingContent = { Text("Producto") },
                                leadingContent = { Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    viewModel.onDismiss()
                                    onProductoSelected(item.producto.id)
                                }
                            )
                        }
                        is SearchResultItem.RecetaResult -> {
                            ListItem(
                                headlineContent = { Text(item.prefabricado.nombre) },
                                supportingContent = { Text("Receta") },
                                leadingContent = { Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                modifier = Modifier.clickable {
                                    viewModel.onDismiss()
                                    onRecetaSelected(item.prefabricado.id)
                                }
                            )
                        }
                        is SearchResultItem.PlatoResult -> {
                            ListItem(
                                headlineContent = { Text(item.plato.nombre) },
                                supportingContent = { Text("Plato") },
                                leadingContent = { Icon(Icons.Filled.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                                modifier = Modifier.clickable {
                                    viewModel.onDismiss()
                                    onPlatoSelected(item.plato.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
