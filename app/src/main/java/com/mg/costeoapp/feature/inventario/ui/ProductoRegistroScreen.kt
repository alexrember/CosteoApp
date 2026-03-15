package com.mg.costeoapp.feature.inventario.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun ProductoRegistroScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProductoRegistroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SaveSuccess -> onNavigateBack()
                is UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = "Registrar producto",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.codigoBarras.isNotBlank()) {
                Text(
                    text = "Codigo: ${uiState.codigoBarras}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            CosteoTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreChanged,
                label = "Nombre del producto *",
                error = uiState.fieldErrors["nombre"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoDropdown(
                selectedValue = uiState.unidadMedida,
                options = UnidadMedida.entries.toList(),
                onOptionSelected = viewModel::onUnidadMedidaChanged,
                label = "Unidad de medida",
                displayText = { it.nombreDisplay }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.cantidadPorEmpaque,
                onValueChange = viewModel::onCantidadPorEmpaqueChanged,
                label = "Cantidad por empaque *",
                keyboardType = KeyboardType.Decimal,
                error = uiState.fieldErrors["cantidadPorEmpaque"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.precio,
                onValueChange = viewModel::onPrecioChanged,
                label = "Precio (ej: 10.50) *",
                keyboardType = KeyboardType.Decimal,
                error = uiState.fieldErrors["precio"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.tiendasDisponibles.isNotEmpty()) {
                val selectedTienda = uiState.tiendasDisponibles.find { it.id == uiState.tiendaSeleccionadaId }
                CosteoDropdown(
                    selectedValue = selectedTienda,
                    options = uiState.tiendasDisponibles,
                    onOptionSelected = { tienda -> tienda?.let { viewModel.onTiendaSelected(it.id) } },
                    label = "Tienda *",
                    displayText = { it?.nombre ?: "Seleccionar tienda" },
                    error = uiState.fieldErrors["tienda"]
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Guardando..." else "Registrar producto")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
