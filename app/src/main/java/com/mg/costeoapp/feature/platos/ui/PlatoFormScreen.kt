package com.mg.costeoapp.feature.platos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import com.mg.costeoapp.core.ui.components.CosteoTextField
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter

@Composable
fun PlatoFormScreen(
    onNavigateBack: () -> Unit,
    onCreated: (Long) -> Unit = { onNavigateBack() },
    viewModel: PlatoFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SaveSuccess -> onNavigateBack()
                is UiEvent.SaveSuccessWithId -> onCreated(event.id)
                is UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = if (uiState.isEditMode) "Editar plato" else "Nuevo plato",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.componentes.isNotEmpty() && uiState.costoEnVivo != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Costo total", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(CurrencyFormatter.fromCents(uiState.costoEnVivo!!),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        if (uiState.precioVentaSugerido != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Precio venta sugerido", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(CurrencyFormatter.fromCents(uiState.precioVentaSugerido!!),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                CosteoTextField(value = uiState.nombre, onValueChange = viewModel::onNombreChanged,
                    label = "Nombre *", error = uiState.fieldErrors["nombre"])
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                CosteoTextField(value = uiState.descripcion, onValueChange = viewModel::onDescripcionChanged,
                    label = "Descripcion", singleLine = false, minLines = 2)
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                CosteoTextField(value = uiState.margenPorcentaje, onValueChange = viewModel::onMargenChanged,
                    label = "Margen food cost (%)", keyboardType = KeyboardType.Decimal,
                    error = uiState.fieldErrors["margen"])
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                CosteoTextField(value = uiState.precioVentaManual, onValueChange = viewModel::onPrecioVentaManualChanged,
                    label = "Precio de venta manual (vacio = calculado)", keyboardType = KeyboardType.Decimal)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Componentes
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Componentes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = viewModel::onShowComponentePicker) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
                uiState.fieldErrors["componentes"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(uiState.componentes) { index, item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.nombre, style = MaterialTheme.typography.bodyMedium)
                            Text(if (item.esPrefabricado) "Receta" else "Producto",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CosteoTextField(value = item.cantidad,
                                onValueChange = { viewModel.onUpdateCantidad(index, it) },
                                label = "Cantidad", keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.fillMaxWidth(0.5f))
                        }
                        IconButton(onClick = { viewModel.onRemoveComponente(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = viewModel::save, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar plato")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Componente picker
    if (uiState.showComponentePicker) {
        ComponentePickerDialog(
            prefabricados = uiState.prefabricadosDisponibles.filter {
                uiState.pickerSearchQuery.isBlank() || it.nombre.contains(uiState.pickerSearchQuery, ignoreCase = true)
            },
            productos = uiState.productosDisponibles.filter {
                uiState.pickerSearchQuery.isBlank() || it.nombre.contains(uiState.pickerSearchQuery, ignoreCase = true)
            },
            searchQuery = uiState.pickerSearchQuery,
            selectedTab = uiState.pickerTab,
            onSearchChanged = viewModel::onPickerSearchChanged,
            onTabChanged = viewModel::onPickerTabChanged,
            onPrefabricadoSelected = viewModel::onAddPrefabricado,
            onProductoSelected = viewModel::onAddProducto,
            onDismiss = viewModel::onDismissComponentePicker
        )
    }
}
