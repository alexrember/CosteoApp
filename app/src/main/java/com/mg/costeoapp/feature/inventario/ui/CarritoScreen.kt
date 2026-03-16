package com.mg.costeoapp.feature.inventario.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mg.costeoapp.core.ui.components.CosteoTopAppBar
import com.mg.costeoapp.core.ui.components.EmptyStateMessage
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter

@Composable
fun CarritoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onCompraConfirmada: () -> Unit,
    viewModel: CarritoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var itemToRemoveIndex by remember { mutableIntStateOf(-1) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.SaveSuccess -> onCompraConfirmada()
                is UiEvent.ShowError -> {
                    if (event.message.startsWith("CONFIRM_REMOVE:")) {
                        val idx = event.message.substringAfter(":").toIntOrNull() ?: -1
                        if (idx >= 0) itemToRemoveIndex = idx
                    } else {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CosteoTopAppBar(
                title = "Carrito de compras",
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isEmpty) {
                EmptyStateMessage(
                    message = "El carrito esta vacio.\nEscanea productos para agregarlos.",
                    actionLabel = "Escanear",
                    onAction = onNavigateToScanner,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Tienda seleccionada
                uiState.tienda?.let { tienda ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Comprando en: ${tienda.nombre}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(uiState.items) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.producto.nombre,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${CurrencyFormatter.fromCents(item.precioUnitario)} c/u",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Controles de cantidad: - [N] +
                            IconButton(onClick = { viewModel.disminuirCantidad(index) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Menos")
                            }
                            Text(
                                text = "${item.cantidad.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(onClick = { viewModel.aumentarCantidad(index) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Mas")
                            }
                            // Subtotal
                            Text(
                                text = CurrencyFormatter.fromCents(item.subtotal),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        HorizontalDivider()
                    }
                }

                // Total + boton confirmar
                HorizontalDivider(thickness = 2.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = CurrencyFormatter.fromCents(uiState.total),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = viewModel::confirmarCompra,
                    enabled = !uiState.isConfirming,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(if (uiState.isConfirming) "Confirmando..." else "Confirmar compra")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Dialogo de confirmacion para quitar item
    if (itemToRemoveIndex >= 0 && itemToRemoveIndex < uiState.items.size) {
        val itemName = uiState.items[itemToRemoveIndex].producto.nombre
        com.mg.costeoapp.core.ui.components.ConfirmDeleteDialog(
            itemName = itemName,
            onConfirm = {
                viewModel.removerItem(itemToRemoveIndex)
                itemToRemoveIndex = -1
            },
            onDismiss = { itemToRemoveIndex = -1 }
        )
    }

}
