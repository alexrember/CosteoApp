package com.mg.costeoapp.feature.productos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun ProductoPrecioFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProductoPrecioFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = "Registrar precio",
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
        ) {
            Text(
                text = uiState.productoNombre,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

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
            } else {
                Text(
                    text = "No hay tiendas registradas. Agrega una primero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.precio,
                onValueChange = viewModel::onPrecioChanged,
                label = "Precio (ej: 10.50) *",
                keyboardType = KeyboardType.Decimal,
                error = uiState.fieldErrors["precio"]
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving && uiState.tiendasDisponibles.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Guardando..." else "Guardar precio")
            }
        }
    }
}
