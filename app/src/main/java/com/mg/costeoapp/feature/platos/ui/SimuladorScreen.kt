package com.mg.costeoapp.feature.platos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.ui.components.CosteoTextField
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter

@Composable
fun SimuladorScreen(
    onNavigateBack: () -> Unit,
    viewModel: SimuladorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showPicker by viewModel.showPicker.collectAsStateWithLifecycle()
    val prefabricados by viewModel.prefabricados.collectAsStateWithLifecycle()
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    val pickerSearchQuery by viewModel.pickerSearchQuery.collectAsStateWithLifecycle()
    val pickerTab by viewModel.pickerTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.SaveSuccess -> snackbarHostState.showSnackbar("Plato guardado")
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = "Simulador",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.componentes.isNotEmpty()) {
                        IconButton(onClick = viewModel::onShowGuardarDialog) {
                            Icon(Icons.Filled.Save, contentDescription = "Guardar como plato")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Banner simulacion
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    "Modo simulacion — Los datos no se guardaran",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                // Margen
                item {
                    CosteoTextField(
                        value = uiState.margenPorcentaje,
                        onValueChange = viewModel::onMargenChanged,
                        label = "Margen food cost (%)",
                        keyboardType = KeyboardType.Decimal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Componentes
                item {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Componentes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = viewModel::onShowPicker) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(uiState.componentes) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text(if (item.esPrefabricado) "Receta" else "Producto",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CosteoTextField(
                                    value = item.cantidad,
                                    onValueChange = { viewModel.onUpdateCantidad(index, it) },
                                    label = "Cantidad",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.fillMaxWidth(0.5f)
                                )
                            }
                            IconButton(onClick = { viewModel.onRemoveComponente(index) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (uiState.componentes.isEmpty()) {
                    item {
                        Text(
                            "Agrega componentes para simular el costo de un plato",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                }
            }

            // Resumen de costo (sticky bottom)
            if (uiState.componentes.isNotEmpty()) {
                HorizontalDivider(thickness = 2.dp)
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Costo total", style = MaterialTheme.typography.titleMedium)
                        Text(CurrencyFormatter.fromCents(uiState.costoTotal),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    uiState.precioVentaSugerido?.let { precio ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Precio de venta sugerido", style = MaterialTheme.typography.titleMedium)
                            Text(CurrencyFormatter.fromCents(precio),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedButton(onClick = viewModel::onLimpiar, modifier = Modifier.weight(1f)) {
                        Text("Limpiar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = viewModel::onShowGuardarDialog, modifier = Modifier.weight(1f)) {
                        Text("Guardar como plato")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Picker
    if (showPicker) {
        ComponentePickerDialog(
            prefabricados = prefabricados,
            productos = productos,
            searchQuery = pickerSearchQuery,
            selectedTab = pickerTab,
            onSearchChanged = viewModel::onPickerSearchChanged,
            onTabChanged = viewModel::onPickerTabChanged,
            onPrefabricadoSelected = viewModel::onAddPrefabricado,
            onProductoSelected = viewModel::onAddProducto,
            onDismiss = viewModel::onDismissPicker
        )
    }

    // Guardar dialog
    if (uiState.showGuardarDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissGuardarDialog,
            title = { Text("Guardar como plato") },
            text = {
                CosteoTextField(
                    value = uiState.nombreParaGuardar,
                    onValueChange = viewModel::onNombreParaGuardarChanged,
                    label = "Nombre del plato *"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::guardarComoPlato) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissGuardarDialog) { Text("Cancelar") }
            }
        )
    }
}
