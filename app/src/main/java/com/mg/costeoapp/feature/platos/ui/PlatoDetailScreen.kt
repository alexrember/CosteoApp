package com.mg.costeoapp.feature.platos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.domain.model.Advertencia
import com.mg.costeoapp.core.ui.components.ConfirmDeleteDialog
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.components.LoadingIndicator
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.formatDisplay
import com.mg.costeoapp.feature.platos.data.TipoComponente

@Composable
fun PlatoDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: PlatoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> onNavigateBack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = uiState.plato?.nombre ?: "Plato",
                onNavigateBack = onNavigateBack,
                actions = {
                    uiState.plato?.let { plato ->
                        IconButton(onClick = { onNavigateToEdit(plato.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
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

        val plato = uiState.plato ?: return@Scaffold

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Precio de venta
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Precio de venta", style = MaterialTheme.typography.labelMedium)
                        Text(
                            uiState.precioVenta?.let { CurrencyFormatter.fromCents(it) } ?: "N/A",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        uiState.costeo?.let {
                            Text("Costo: ${CurrencyFormatter.fromCents(it.costoTotal)}", style = MaterialTheme.typography.bodySmall)
                        }
                        plato.margenPorcentaje?.let {
                            Text("Margen: ${it.formatDisplay()}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Componentes
            item {
                Text("Componentes", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.componentesDetalle) { comp ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(comp.nombre, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${comp.componente.cantidad.formatDisplay()} ${if (comp.tipo == TipoComponente.PREFABRICADO) "porciones" else "unidades"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    comp.costoTotal?.let {
                        Text(CurrencyFormatter.fromCents(it), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    } ?: Text("—", style = MaterialTheme.typography.titleSmall)
                }
                HorizontalDivider()
            }

            // Advertencias
            uiState.costeo?.advertencias?.let { advs ->
                if (advs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Advertencias", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
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

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            itemName = uiState.plato?.nombre ?: "",
            onConfirm = {
                showDeleteDialog = false
                viewModel.softDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
