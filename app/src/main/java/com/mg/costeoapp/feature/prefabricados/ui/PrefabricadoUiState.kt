package com.mg.costeoapp.feature.prefabricados.ui

import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.relation.IngredienteConProducto
import com.mg.costeoapp.core.domain.model.Advertencia
import com.mg.costeoapp.core.domain.model.CosteoResult
import com.mg.costeoapp.core.domain.model.FuentePrecio
import com.mg.costeoapp.core.domain.model.NutricionResumen
import com.mg.costeoapp.core.util.UnidadMedida

data class PrefabricadoConCosto(
    val prefabricado: Prefabricado,
    val costoPorPorcion: Long? = null,
    val tieneAdvertencias: Boolean = false
)

data class PrefabricadoListUiState(
    val items: List<PrefabricadoConCosto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PrefabricadoDetailUiState(
    val prefabricado: Prefabricado? = null,
    val ingredientes: List<IngredienteConProducto> = emptyList(),
    val costeo: CosteoResult? = null,
    val nutricion: NutricionResumen? = null,
    val variantes: List<Prefabricado> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class IngredienteFormItem(
    val producto: Producto,
    val cantidadUsada: String = "",
    val unidadUsada: UnidadMedida = UnidadMedida.LIBRA
)

data class PrefabricadoFormUiState(
    val prefabricado: Prefabricado? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val rendimientoPorciones: String = "",
    val costoFijo: String = "0",
    val ingredientes: List<IngredienteFormItem> = emptyList(),
    val costeoEnVivo: CosteoResult? = null,
    val showIngredientePicker: Boolean = false,
    val productosDisponibles: List<Producto> = emptyList(),
    val productoSearchQuery: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isDuplicateMode: Boolean = false,
    val duplicadoDeNombre: String? = null
) {
    val isEditMode: Boolean get() = prefabricado != null && !isDuplicateMode
}
