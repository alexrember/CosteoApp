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
import androidx.compose.material3.LinearProgressIndicator
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
import com.mg.costeoapp.core.domain.model.FieldResolution
import com.mg.costeoapp.core.ui.components.CosteoDropdown
import com.mg.costeoapp.core.ui.components.CosteoTextField
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.components.FieldConflictChooser
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.UnidadMedida

@Composable
fun ProductoRegistroScreen(
    onNavigateBack: () -> Unit,
    onRegistroExitoso: () -> Unit = onNavigateBack,
    viewModel: ProductoRegistroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SaveSuccess -> onRegistroExitoso()
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
            }

            if (uiState.tiendaNombre.isNotBlank()) {
                Text(
                    text = "Tienda: ${uiState.tiendaNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.mergedData?.sources?.isNotEmpty() == true) {
                Text(
                    text = "Fuentes: ${uiState.mergedData!!.sources.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.buscandoEnApi) {
                Text(
                    text = "Buscando producto...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Nombre: si hay conflicto, mostrar opciones
            val nombreResolution = uiState.mergedData?.nombre
            if (nombreResolution is FieldResolution.Conflict) {
                FieldConflictChooser(
                    label = "Nombre — elige una opcion:",
                    options = nombreResolution.options,
                    selectedValue = uiState.nombre,
                    displayText = { it },
                    onOptionSelected = viewModel::onNombreSelected
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                label = "Contenido por unidad *",
                keyboardType = KeyboardType.Decimal,
                error = uiState.fieldErrors["cantidadPorEmpaque"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.unidadesPorEmpaque,
                onValueChange = viewModel::onUnidadesPorEmpaqueChanged,
                label = "Unidades por empaque (1 si es individual)",
                keyboardType = KeyboardType.Number,
                error = uiState.fieldErrors["unidadesPorEmpaque"]
            )

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
                enabled = !uiState.isSaving && !uiState.buscandoEnApi,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Guardando..." else "Registrar producto")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
