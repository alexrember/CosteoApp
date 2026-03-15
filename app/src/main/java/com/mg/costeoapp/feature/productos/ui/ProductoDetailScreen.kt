package com.mg.costeoapp.feature.productos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.relation.PrecioConTienda
import com.mg.costeoapp.core.ui.components.ConfirmDeleteDialog
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.components.LoadingIndicator
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.DateFormatter
import com.mg.costeoapp.core.util.UnidadMedida

@Composable
fun ProductoDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToAddPrecio: (Long) -> Unit,
    viewModel: ProductoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = uiState.producto?.nombre ?: "Producto",
                onNavigateBack = onNavigateBack,
                actions = {
                    uiState.producto?.let { producto ->
                        IconButton(onClick = { onNavigateToEdit(producto.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            uiState.producto?.let { producto ->
                FloatingActionButton(onClick = { onNavigateToAddPrecio(producto.id) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar precio")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val producto = uiState.producto
        if (producto == null) {
            Text("Producto no encontrado", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item { ProductoInfoCard(producto) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { NutricionSection(producto) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Text(
                    text = "Precios por tienda",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (uiState.preciosConTienda.isEmpty()) {
                item {
                    Text(
                        text = "Sin precios registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.preciosConTienda) { precioConTienda ->
                    PrecioItem(precioConTienda)
                }
            }
        }
    }

    if (showDeleteDialog) {
        uiState.producto?.let { producto ->
            ConfirmDeleteDialog(
                itemName = producto.nombre,
                onConfirm = {
                    viewModel.softDelete(producto.id)
                    showDeleteDialog = false
                    onNavigateBack()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun ProductoInfoCard(producto: Producto) {
    val unidad = UnidadMedida.fromCodigo(producto.unidadMedida)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow("Unidad", "${producto.cantidadPorEmpaque} ${unidad?.nombreDisplay ?: producto.unidadMedida}")
            if (!producto.codigoBarras.isNullOrBlank()) {
                DetailRow("Codigo de barras", producto.codigoBarras)
            }
            if (producto.esServicio) {
                DetailRow("Tipo", "Servicio")
            }
            if (producto.factorMerma > 0) {
                DetailRow("Merma", "${producto.factorMerma}%")
            }
            if (!producto.notas.isNullOrBlank()) {
                DetailRow("Notas", producto.notas)
            }
        }
    }
}

@Composable
private fun NutricionSection(producto: Producto) {
    val hasNutricion = producto.nutricionCalorias != null ||
            producto.nutricionProteinasG != null ||
            producto.nutricionCarbohidratosG != null ||
            producto.nutricionGrasasG != null

    if (!hasNutricion) {
        Text(
            text = "Sin informacion nutricional",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Informacion nutricional", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            producto.nutricionPorcionG?.let { DetailRow("Porcion", "${it}g") }
            producto.nutricionCalorias?.let { DetailRow("Calorias", "$it") }
            producto.nutricionProteinasG?.let { DetailRow("Proteinas", "${it}g") }
            producto.nutricionCarbohidratosG?.let { DetailRow("Carbohidratos", "${it}g") }
            producto.nutricionGrasasG?.let { DetailRow("Grasas", "${it}g") }
            producto.nutricionFibraG?.let { DetailRow("Fibra", "${it}g") }
            producto.nutricionSodioMg?.let { DetailRow("Sodio", "${it}mg") }
            producto.nutricionFuente?.let { DetailRow("Fuente", it) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrecioItem(precioConTienda: PrecioConTienda) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = precioConTienda.tiendaNombre,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CurrencyFormatter.fromCents(precioConTienda.productoTienda.precio),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = DateFormatter.formatDate(precioConTienda.productoTienda.fechaRegistro),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}
