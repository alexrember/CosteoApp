package com.mg.costeoapp.feature.prefabricados.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.core.util.ErrorMapper
import com.mg.costeoapp.core.util.UnidadMedida
import com.mg.costeoapp.core.util.ValidationUtils
import com.mg.costeoapp.feature.prefabricados.data.PrefabricadoRepository
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Extract duplicar logic (loadForDuplicate) into DuplicarRecetaUseCase
@HiltViewModel
class PrefabricadoFormViewModel @Inject constructor(
    private val repository: PrefabricadoRepository,
    private val productoRepository: ProductoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrefabricadoFormUiState())
    val uiState: StateFlow<PrefabricadoFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val recetaId: Long = savedStateHandle.get<Long>("recetaId") ?: 0L
    private val duplicadoDeId: Long = savedStateHandle.get<Long>("duplicadoDeId") ?: 0L

    init {
        viewModelScope.launch {
            when {
                duplicadoDeId != 0L -> loadForDuplicate(duplicadoDeId)
                recetaId != 0L -> loadForEdit(recetaId)
            }
        }
    }

    private suspend fun loadForEdit(id: Long) {
        val data = repository.getConIngredientes(id) ?: return
        _uiState.update {
            it.copy(
                prefabricado = data.prefabricado,
                nombre = data.prefabricado.nombre,
                descripcion = data.prefabricado.descripcion ?: "",
                rendimientoPorciones = data.prefabricado.rendimientoPorciones.toString(),
                costoFijo = CurrencyFormatter.fromCents(data.prefabricado.costoFijo).replace("$", ""),
                ingredientes = data.ingredientes.map { ing ->
                    IngredienteFormItem(
                        producto = ing.producto,
                        cantidadUsada = ing.ingrediente.cantidadUsada.toString(),
                        unidadUsada = UnidadMedida.fromCodigo(ing.ingrediente.unidadUsada) ?: UnidadMedida.LIBRA
                    )
                }
            )
        }
    }

    private suspend fun loadForDuplicate(sourceId: Long) {
        val data = repository.getConIngredientes(sourceId) ?: return
        _uiState.update {
            it.copy(
                isDuplicateMode = true,
                duplicadoDeNombre = data.prefabricado.nombre,
                nombre = "${data.prefabricado.nombre} (copia)",
                descripcion = data.prefabricado.descripcion ?: "",
                rendimientoPorciones = data.prefabricado.rendimientoPorciones.toString(),
                costoFijo = CurrencyFormatter.fromCents(data.prefabricado.costoFijo).replace("$", ""),
                ingredientes = data.ingredientes.map { ing ->
                    IngredienteFormItem(
                        producto = ing.producto,
                        cantidadUsada = ing.ingrediente.cantidadUsada.toString(),
                        unidadUsada = UnidadMedida.fromCodigo(ing.ingrediente.unidadUsada) ?: UnidadMedida.LIBRA
                    )
                }
            )
        }
    }

    fun onNombreChanged(v: String) { _uiState.update { it.copy(nombre = v, fieldErrors = it.fieldErrors - "nombre") } }
    fun onDescripcionChanged(v: String) { _uiState.update { it.copy(descripcion = v) } }
    fun onRendimientoChanged(v: String) { _uiState.update { it.copy(rendimientoPorciones = v, fieldErrors = it.fieldErrors - "rendimiento") } }
    fun onCostoFijoChanged(v: String) { _uiState.update { it.copy(costoFijo = v) } }

    fun onShowIngredientePicker() {
        viewModelScope.launch {
            val productos = productoRepository.getAll().first().filter { p ->
                _uiState.value.ingredientes.none { it.producto.id == p.id }
            }
            _uiState.update { it.copy(showIngredientePicker = true, productosDisponibles = productos, productoSearchQuery = "") }
        }
    }

    fun onDismissIngredientePicker() {
        _uiState.update { it.copy(showIngredientePicker = false) }
    }

    fun onProductoSearchChanged(query: String) {
        _uiState.update { it.copy(productoSearchQuery = query) }
    }

    fun onAddIngrediente(producto: com.mg.costeoapp.core.database.entity.Producto, cantidad: String, unidad: UnidadMedida) {
        _uiState.update {
            it.copy(
                ingredientes = it.ingredientes + IngredienteFormItem(producto, cantidad, unidad),
                showIngredientePicker = false
            )
        }
    }

    fun onUpdateIngrediente(index: Int, cantidad: String, unidad: UnidadMedida) {
        _uiState.update {
            it.copy(ingredientes = it.ingredientes.toMutableList().apply {
                this[index] = this[index].copy(cantidadUsada = cantidad, unidadUsada = unidad)
            })
        }
    }

    fun onRemoveIngrediente(index: Int) {
        _uiState.update {
            it.copy(ingredientes = it.ingredientes.toMutableList().apply { removeAt(index) })
        }
    }

    fun save() {
        if (!validate()) return
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val costoFijoCents = CurrencyFormatter.toCents(state.costoFijo) ?: 0L

            val prefabricado = Prefabricado(
                id = if (state.isEditMode) state.prefabricado!!.id else 0,
                nombre = state.nombre.trim(),
                descripcion = state.descripcion.trim().ifBlank { null },
                duplicadoDe = if (state.isDuplicateMode) duplicadoDeId.takeIf { it != 0L } else state.prefabricado?.duplicadoDe,
                costoFijo = costoFijoCents,
                rendimientoPorciones = state.rendimientoPorciones.toDouble(),
                createdAt = state.prefabricado?.createdAt ?: System.currentTimeMillis()
            )

            val ingredientes = state.ingredientes.map {
                PrefabricadoIngrediente(
                    prefabricadoId = 0,
                    productoId = it.producto.id,
                    cantidadUsada = it.cantidadUsada.toDoubleOrNull() ?: 0.0,
                    unidadUsada = it.unidadUsada.codigo
                )
            }

            if (state.isEditMode) {
                repository.update(prefabricado, ingredientes).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(UiEvent.SaveSuccess)
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(UiEvent.ShowError(ErrorMapper.toUserMessage(e)))
                    }
                )
            } else {
                repository.create(prefabricado, ingredientes).fold(
                    onSuccess = { newId ->
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(UiEvent.SaveSuccessWithId(newId))
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isSaving = false) }
                        _events.send(UiEvent.ShowError(ErrorMapper.toUserMessage(e)))
                    }
                )
            }
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        if (!ValidationUtils.isValidName(state.nombre)) {
            errors["nombre"] = "El nombre debe tener al menos 2 caracteres"
        }
        if (!ValidationUtils.isPositiveNumber(state.rendimientoPorciones)) {
            errors["rendimiento"] = "Debe ser mayor a 0"
        }
        if (state.ingredientes.isEmpty()) {
            errors["ingredientes"] = "Agrega al menos un ingrediente"
        }
        state.ingredientes.forEachIndexed { index, item ->
            val cantidad = item.cantidadUsada.toDoubleOrNull()
            if (cantidad == null || cantidad <= 0) {
                errors["ingrediente_$index"] = "Cantidad invalida en ${item.producto.nombre}"
            }
        }

        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
