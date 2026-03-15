package com.mg.costeoapp.feature.tiendas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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

private val TIPOS_TIENDA = listOf(
    "supermercado" to "Supermercado",
    "mercado" to "Mercado",
    "proveedor" to "Proveedor",
    "vendedor_ambulante" to "Vendedor ambulante",
    "otro" to "Otro"
)

@Composable
fun TiendaFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: TiendaFormViewModel = hiltViewModel()
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
                title = if (uiState.isEditMode) "Editar tienda" else "Nueva tienda",
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
            CosteoTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreChanged,
                label = "Nombre *",
                error = uiState.fieldErrors["nombre"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoDropdown(
                selectedValue = uiState.tipo,
                options = TIPOS_TIENDA.map { it.first },
                onOptionSelected = viewModel::onTipoChanged,
                label = "Tipo",
                displayText = { code ->
                    TIPOS_TIENDA.find { it.first == code }?.second ?: code
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.direccion,
                onValueChange = viewModel::onDireccionChanged,
                label = "Direccion"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.telefono,
                onValueChange = viewModel::onTelefonoChanged,
                label = "Telefono / WhatsApp",
                keyboardType = KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.diasCredito,
                onValueChange = viewModel::onDiasCreditoChanged,
                label = "Dias de credito (vacio = contado)",
                keyboardType = KeyboardType.Number,
                error = uiState.fieldErrors["diasCredito"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.notas,
                onValueChange = viewModel::onNotasChanged,
                label = "Notas",
                singleLine = false,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Guardando..." else "Guardar")
            }
        }
    }
}
