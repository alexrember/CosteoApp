package com.mg.costeoapp.feature.prefabricados.ui

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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.domain.model.Advertencia
import com.mg.costeoapp.core.domain.model.FuentePrecio
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.components.LoadingIndicator
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.UnidadMedida
import com.mg.costeoapp.core.util.formatDisplay

@Composable
fun PrefabricadoDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToDuplicate: (Long) -> Unit,
    viewModel: PrefabricadoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onNavigateBack()
                else -> {}
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = uiState.prefabricado?.nombre ?: "Receta",
                onNavigateBack = onNavigateBack,
                actions = {
                    uiState.prefabricado?.let { pref ->
                        IconButton(onClick = { onNavigateToDuplicate(pref.id) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar")
                        }
                        IconButton(onClick = { onNavigateToEdit(pref.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { viewModel.softDelete() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val pref = uiState.prefabricado ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // Info basica
            item {
                Text("${pref.rendimientoPorciones.formatDisplay()} porciones",
                    style = MaterialTheme.typography.bodyLarge)
                pref.descripcion?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Costo por porcion destacado
            uiState.costeo?.let { costeo ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Costo por porcion", style = MaterialTheme.typography.labelMedium)
                            Text(
                                costeo.costoPorPorcion?.let { CurrencyFormatter.fromCents(it) } ?: "N/A",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("Total: ${CurrencyFormatter.fromCents(costeo.costoTotal)}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Ingredientes
            item {
                Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.ingredientesCosto) { item ->
                val unidad = UnidadMedida.fromCodigo(item.unidadUsada)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productoNombre, style = MaterialTheme.typography.bodyMedium)
                        Text("${item.cantidadUsada.formatDisplay()} ${unidad?.nombreDisplay ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            when (item.fuente) {
                                FuentePrecio.INVENTARIO -> "Inventario"
                                FuentePrecio.PRECIO_RECIENTE -> "Precio reciente"
                                FuentePrecio.SIN_PRECIO -> "Sin precio"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (item.fuente) {
                                FuentePrecio.INVENTARIO -> MaterialTheme.colorScheme.secondary
                                FuentePrecio.PRECIO_RECIENTE -> MaterialTheme.colorScheme.tertiary
                                FuentePrecio.SIN_PRECIO -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        item.costoTotal?.let {
                            Text(CurrencyFormatter.fromCents(it),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary)
                        } ?: Text("—", style = MaterialTheme.typography.titleSmall)
                    }
                }
                HorizontalDivider()
            }

            // Nutricion
            uiState.nutricion?.let { nut ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nutricion por porcion", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!nut.esCompleto) {
                        Text("Info parcial. Faltan: ${nut.productosSinInfo.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    nut.calorias?.let { Text("Calorias: ${it.formatDisplay()} kcal") }
                    nut.proteinas?.let { Text("Proteinas: ${it.formatDisplay()}g") }
                    nut.carbohidratos?.let { Text("Carbohidratos: ${it.formatDisplay()}g") }
                    nut.grasas?.let { Text("Grasas: ${it.formatDisplay()}g") }
                    nut.fibra?.let { Text("Fibra: ${it.formatDisplay()}g") }
                    nut.sodioMg?.let { Text("Sodio: ${it.formatDisplay()}mg") }
                }
            }

            // Advertencias
            uiState.costeo?.advertencias?.let { advs ->
                if (advs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Advertencias", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error)
                        advs.forEach { adv ->
                            Text(
                                when (adv) {
                                    is Advertencia.SinPrecio -> "Sin precio: ${adv.nombreProducto}"
                                    is Advertencia.SinStock -> "Sin stock: ${adv.nombreProducto}"
                                    is Advertencia.IngredienteInactivo -> "Inactivo: ${adv.nombreProducto}"
                                    is Advertencia.NutricionIncompleta -> "Sin nutricion: ${adv.nombreProducto}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
