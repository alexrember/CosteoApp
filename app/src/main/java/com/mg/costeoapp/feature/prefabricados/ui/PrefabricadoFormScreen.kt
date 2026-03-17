package com.mg.costeoapp.feature.prefabricados.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.mg.costeoapp.core.ui.components.CosteoDropdown
import com.mg.costeoapp.core.ui.components.CosteoTextField
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.UnidadMedida

@Composable
fun PrefabricadoFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrefabricadoFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SaveSuccess -> onNavigateBack()
                is UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = when {
                    uiState.isDuplicateMode -> "Duplicar receta"
                    uiState.isEditMode -> "Editar receta"
                    else -> "Nueva receta"
                },
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // Nombre
            item {
                CosteoTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChanged,
                    label = "Nombre *",
                    error = uiState.fieldErrors["nombre"]
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Descripcion
            item {
                CosteoTextField(
                    value = uiState.descripcion,
                    onValueChange = viewModel::onDescripcionChanged,
                    label = "Descripcion",
                    singleLine = false,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Rendimiento
            item {
                CosteoTextField(
                    value = uiState.rendimientoPorciones,
                    onValueChange = viewModel::onRendimientoChanged,
                    label = "Porciones que rinde *",
                    keyboardType = KeyboardType.Decimal,
                    error = uiState.fieldErrors["rendimiento"]
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Costo fijo
            item {
                CosteoTextField(
                    value = uiState.costoFijo,
                    onValueChange = viewModel::onCostoFijoChanged,
                    label = "Costo fijo (gas, desechables, etc.)",
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Ingredientes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ingredientes", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = viewModel::onShowIngredientePicker) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar")
                    }
                }
                uiState.fieldErrors["ingredientes"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(uiState.ingredientes) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.producto.nombre, style = MaterialTheme.typography.bodyMedium)
                            Row {
                                CosteoTextField(
                                    value = item.cantidadUsada,
                                    onValueChange = { viewModel.onUpdateIngrediente(index, it, item.unidadUsada) },
                                    label = "Cantidad",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                CosteoDropdown(
                                    selectedValue = item.unidadUsada,
                                    options = UnidadMedida.entries.toList(),
                                    onOptionSelected = { viewModel.onUpdateIngrediente(index, item.cantidadUsada, it) },
                                    label = "Unidad",
                                    displayText = { it.nombreDisplay },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.onRemoveIngrediente(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Guardar
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar receta")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Ingrediente picker dialog
    if (uiState.showIngredientePicker) {
        IngredientePickerDialog(
            productos = uiState.productosDisponibles.filter {
                uiState.productoSearchQuery.isBlank() ||
                it.nombre.contains(uiState.productoSearchQuery, ignoreCase = true)
            },
            searchQuery = uiState.productoSearchQuery,
            onSearchChanged = viewModel::onProductoSearchChanged,
            onProductoSelected = { producto ->
                val unidad = UnidadMedida.fromCodigo(producto.unidadMedida) ?: UnidadMedida.LIBRA
                viewModel.onAddIngrediente(producto, "1", unidad)
            },
            onDismiss = viewModel::onDismissIngredientePicker
        )
    }
}
