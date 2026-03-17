package com.mg.costeoapp.feature.prefabricados.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.ui.components.CosteoSearchBar
import com.mg.costeoapp.core.util.UnidadMedida

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientePickerDialog(
    productos: List<Producto>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onProductoSelected: (Producto) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        ) {
            Text(
                "Seleccionar ingrediente",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            CosteoSearchBar(
                query = searchQuery,
                onQueryChanged = onSearchChanged,
                placeholder = "Buscar producto..."
            )

            if (productos.isEmpty()) {
                Text(
                    "No hay productos disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(productos, key = { it.id }) { producto ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductoSelected(producto) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Inventory2, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(producto.nombre, style = MaterialTheme.typography.bodyLarge)
                                val unidad = UnidadMedida.fromCodigo(producto.unidadMedida)
                                Text("${producto.cantidadPorEmpaque} ${unidad?.nombreDisplay ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
