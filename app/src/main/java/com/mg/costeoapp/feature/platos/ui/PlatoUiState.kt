package com.mg.costeoapp.feature.platos.ui

import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.NutricionResumen
import com.mg.costeoapp.feature.platos.data.ComponenteDetalle

data class PlatoConCosto(
    val plato: Plato,
    val costoTotal: Long? = null,
    val precioVenta: Long? = null,
    val tieneAdvertencias: Boolean = false
)

data class PlatoListUiState(
    val items: List<PlatoConCosto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PlatoDetailUiState(
    val plato: Plato? = null,
    val componentesDetalle: List<ComponenteDetalle> = emptyList(),
    val costeo: CosteoResult? = null,
    val nutricion: NutricionResumen? = null,
    val precioVenta: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ComponenteFormItem(
    val prefabricado: Prefabricado? = null,
    val producto: Producto? = null,
    val cantidad: String = "1",
    val nombre: String = ""
) {
    val esPrefabricado: Boolean get() = prefabricado != null
}

data class PlatoFormUiState(
    val plato: Plato? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val margenPorcentaje: String = "35",
    val precioVentaManual: String = "",
    val componentes: List<ComponenteFormItem> = emptyList(),
    val showComponentePicker: Boolean = false,
    val prefabricadosDisponibles: List<Prefabricado> = emptyList(),
    val productosDisponibles: List<Producto> = emptyList(),
    val pickerSearchQuery: String = "",
    val pickerTab: Int = 0,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false
) {
    val isEditMode: Boolean get() = plato != null
}

// Simulador usa el mismo form state
data class SimuladorUiState(
    val componentes: List<ComponenteFormItem> = emptyList(),
    val margenPorcentaje: String = "35",
    val costoTotal: Long = 0,
    val precioVentaSugerido: Long? = null,
    val showGuardarDialog: Boolean = false,
    val nombreParaGuardar: String = ""
)
