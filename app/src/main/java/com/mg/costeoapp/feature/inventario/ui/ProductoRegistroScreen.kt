package com.mg.costeoapp.feature.inventario.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mg.costeoapp.feature.inventario.data.ocr.OcrNutritionProcessor
import com.mg.costeoapp.feature.inventario.data.voice.VoiceInputParser
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.core.content.FileProvider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProductoRegistroScreen(
    onNavigateBack: () -> Unit,
    onRegistroExitoso: () -> Unit = onNavigateBack,
    viewModel: ProductoRegistroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val ocrProcessor = remember { OcrNutritionProcessor(context) }

    // Crear archivo temporal fresco para cada foto
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    fun createPhotoUri(): Uri {
        val file = File(context.cacheDir, "ocr_nutrition_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val parsed = VoiceInputParser.parse(spoken)
                viewModel.onVoiceResult(parsed)
                scope.launch {
                    snackbarHostState.showSnackbar("Voz: \"$spoken\"")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            scope.launch {
                val result = ocrProcessor.processImage(photoUri!!)
                if (result != null) {
                    viewModel.onOcrResult(result)
                } else {
                    snackbarHostState.showSnackbar("No se pudo leer la etiqueta. Intenta con mejor luz.")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SaveSuccess -> onRegistroExitoso()
                is UiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
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

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                CosteoTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChanged,
                    label = "Nombre del producto *",
                    error = uiState.fieldErrors["nombre"],
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-SV")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta producto, cantidad y precio")
                        }
                        speechLauncher.launch(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Entrada por voz")
                }
            }

            if (uiState.sugerencias.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        uiState.sugerencias.forEach { producto ->
                            ListItem(
                                headlineContent = { Text(producto.nombre) },
                                supportingContent = {
                                    Text("${producto.unidadMedida} - ${producto.cantidadPorEmpaque}")
                                },
                                modifier = Modifier.clickable {
                                    viewModel.onSugerenciaSelected(producto)
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.ultimoPrecioSugerencia != null) {
                Text(
                    text = "Ultimo precio: ${uiState.ultimoPrecioSugerencia}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val uri = createPhotoUri()
                    photoUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null,
                    modifier = androidx.compose.ui.Modifier.padding(end = 8.dp))
                Text("Escanear etiqueta nutricional")
            }

            if (uiState.productosFrequentes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Productos frecuentes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(uiState.productosFrequentes) { producto ->
                        AssistChip(
                            onClick = { viewModel.onSelectFrequentProduct(producto) },
                            label = { Text(producto.nombre) }
                        )
                    }
                }
            }

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
