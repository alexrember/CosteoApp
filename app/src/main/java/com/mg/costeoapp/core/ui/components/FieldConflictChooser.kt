package com.mg.costeoapp.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mg.costeoapp.core.domain.model.FieldOption

/**
 * Muestra opciones de diferentes fuentes cuando hay conflicto.
 * El usuario toca un chip para elegir cual usar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FieldConflictChooser(
    label: String,
    options: List<FieldOption<T>>,
    selectedValue: T?,
    displayText: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.value == selectedValue,
                    onClick = { onOptionSelected(option.value) },
                    label = {
                        Column {
                            Text(displayText(option.value), style = MaterialTheme.typography.bodyMedium)
                            Text(option.source, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }
}
