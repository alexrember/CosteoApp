package com.mg.costeoapp.feature.tiendas.ui

import com.mg.costeoapp.core.database.entity.Tienda

data class TiendaListUiState(
    val tiendas: List<Tienda> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TiendaFormUiState(
    val tienda: Tienda? = null,
    val nombre: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    val isEditMode: Boolean get() = tienda != null
}
