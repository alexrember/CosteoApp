package com.mg.costeoapp.feature.productos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.ui.components.CosteoDropdown
import com.mg.costeoapp.core.ui.components.CosteoTextField
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.util.UnidadMedida

@Composable
fun ProductoFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProductoFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var nutricionExpanded by rememberSaveable { mutableStateOf(false) }

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
                title = if (uiState.isEditMode) "Editar producto" else "Nuevo producto",
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

            CosteoTextField(
                value = uiState.codigoBarras,
                onValueChange = viewModel::onCodigoBarrasChanged,
                label = "Codigo de barras"
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
                value = uiState.factorMerma,
                onValueChange = viewModel::onFactorMermaChanged,
                label = "Factor de merma (%) — 0 si no aplica",
                keyboardType = KeyboardType.Number,
                error = uiState.fieldErrors["factorMerma"]
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Es servicio",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = uiState.esServicio,
                    onCheckedChange = viewModel::onEsServicioChanged
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CosteoTextField(
                value = uiState.notas,
                onValueChange = viewModel::onNotasChanged,
                label = "Notas",
                singleLine = false,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seccion nutricional colapsable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nutricionExpanded = !nutricionExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Informacion nutricional",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (nutricionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (nutricionExpanded) "Colapsar" else "Expandir"
                )
            }

            AnimatedVisibility(visible = nutricionExpanded) {
                Column {
                    val nutriFields = listOf(
                        Triple("nutricionPorcionG", "Porcion (g)", uiState.nutricionPorcionG) to viewModel::onNutricionPorcionGChanged,
                        Triple("nutricionCalorias", "Calorias", uiState.nutricionCalorias) to viewModel::onNutricionCaloriasChanged,
                        Triple("nutricionProteinasG", "Proteinas (g)", uiState.nutricionProteinasG) to viewModel::onNutricionProteinasGChanged,
                        Triple("nutricionCarbohidratosG", "Carbohidratos (g)", uiState.nutricionCarbohidratosG) to viewModel::onNutricionCarbohidratosGChanged,
                        Triple("nutricionGrasasG", "Grasas (g)", uiState.nutricionGrasasG) to viewModel::onNutricionGrasasGChanged,
                        Triple("nutricionFibraG", "Fibra (g)", uiState.nutricionFibraG) to viewModel::onNutricionFibraGChanged,
                        Triple("nutricionSodioMg", "Sodio (mg)", uiState.nutricionSodioMg) to viewModel::onNutricionSodioMgChanged,
                    )

                    nutriFields.forEach { (triple, onChange) ->
                        Spacer(modifier = Modifier.height(8.dp))
                        CosteoTextField(
                            value = triple.third,
                            onValueChange = onChange,
                            label = triple.second,
                            keyboardType = KeyboardType.Decimal,
                            error = uiState.fieldErrors[triple.first]
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CosteoTextField(
                        value = uiState.nutricionFuente,
                        onValueChange = viewModel::onNutricionFuenteChanged,
                        label = "Fuente de informacion"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Guardando..." else "Guardar")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
