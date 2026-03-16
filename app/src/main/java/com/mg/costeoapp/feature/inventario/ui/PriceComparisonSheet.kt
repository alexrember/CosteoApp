package com.mg.costeoapp.feature.inventario.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.DateFormatter

data class PrecioComparado(
    val tiendaNombre: String,
    val precio: Long,
    val fecha: Long?,
    val fuente: String // "local", "walmart_vtex", etc.
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceComparisonSheet(
    productoNombre: String,
    precios: List<PrecioComparado>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val precioMinimo = precios.minByOrNull { it.precio }?.precio

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Comparar precios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = productoNombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (precios.isEmpty()) {
                Text(
                    text = "No hay precios disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                precios.sortedBy { it.precio }.forEach { precio ->
                    val esMasBarato = precio.precio == precioMinimo
                    PrecioComparadoItem(precio, esMasBarato)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PrecioComparadoItem(precio: PrecioComparado, esMasBarato: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = precio.tiendaNombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (esMasBarato) FontWeight.Bold else FontWeight.Normal
            )
            Row {
                Text(
                    text = when (precio.fuente) {
                        "local" -> "Precio registrado"
                        else -> "Precio online"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (precio.fecha != null) {
                    Text(
                        text = " — ${DateFormatter.formatDate(precio.fecha)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                text = CurrencyFormatter.fromCents(precio.precio),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (esMasBarato) FontWeight.Bold else FontWeight.Normal,
                color = if (esMasBarato) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )
            if (esMasBarato) {
                Text(
                    text = "Mas barato",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
