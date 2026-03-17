package com.mg.costeoapp.feature.platos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.ui.components.CosteoSearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentePickerDialog(
    prefabricados: List<Prefabricado>,
    productos: List<Producto>,
    searchQuery: String,
    selectedTab: Int,
    onSearchChanged: (String) -> Unit,
    onTabChanged: (Int) -> Unit,
    onPrefabricadoSelected: (Prefabricado) -> Unit,
    onProductoSelected: (Producto) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Agregar componente", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { onTabChanged(0) }, text = { Text("Recetas") })
                Tab(selected = selectedTab == 1, onClick = { onTabChanged(1) }, text = { Text("Productos") })
            }

            CosteoSearchBar(query = searchQuery, onQueryChanged = onSearchChanged, placeholder = "Buscar...")

            LazyColumn(modifier = Modifier.height(400.dp)) {
                if (selectedTab == 0) {
                    items(prefabricados, key = { it.id }) { pref ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onPrefabricadoSelected(pref) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pref.nombre, style = MaterialTheme.typography.bodyLarge)
                                Text("${pref.rendimientoPorciones} porciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(productos, key = { it.id }) { prod ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onProductoSelected(prod) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(prod.nombre, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
