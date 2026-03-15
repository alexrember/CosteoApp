package com.mg.costeoapp.feature.productos.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.UnidadMedida
import com.mg.costeoapp.core.util.ValidationUtils
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductoFormViewModel @Inject constructor(
    private val repository: ProductoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoFormUiState())
    val uiState: StateFlow<ProductoFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val productoId = savedStateHandle.get<Long>("productoId")
        if (productoId != null && productoId != 0L) {
            loadProducto(productoId)
        }
    }

    private fun loadProducto(id: Long) {
        viewModelScope.launch {
            val producto = repository.getById(id)
            if (producto != null) {
                _uiState.update {
                    it.copy(
                        producto = producto,
                        nombre = producto.nombre,
                        codigoBarras = producto.codigoBarras ?: "",
                        unidadMedida = UnidadMedida.fromCodigo(producto.unidadMedida) ?: UnidadMedida.LIBRA,
                        cantidadPorEmpaque = producto.cantidadPorEmpaque.toString(),
                        esServicio = producto.esServicio,
                        notas = producto.notas ?: "",
                        factorMerma = producto.factorMerma.toString(),
                        nutricionPorcionG = producto.nutricionPorcionG?.toString() ?: "",
                        nutricionCalorias = producto.nutricionCalorias?.toString() ?: "",
                        nutricionProteinasG = producto.nutricionProteinasG?.toString() ?: "",
                        nutricionCarbohidratosG = producto.nutricionCarbohidratosG?.toString() ?: "",
                        nutricionGrasasG = producto.nutricionGrasasG?.toString() ?: "",
                        nutricionFibraG = producto.nutricionFibraG?.toString() ?: "",
                        nutricionSodioMg = producto.nutricionSodioMg?.toString() ?: "",
                        nutricionFuente = producto.nutricionFuente ?: ""
                    )
                }
            }
        }
    }

    fun onNombreChanged(value: String) {
        _uiState.update { it.copy(nombre = value, fieldErrors = it.fieldErrors - "nombre") }
    }

    fun onUnidadMedidaChanged(value: UnidadMedida) {
        _uiState.update { it.copy(unidadMedida = value) }
    }

    fun onCantidadPorEmpaqueChanged(value: String) {
        _uiState.update { it.copy(cantidadPorEmpaque = value, fieldErrors = it.fieldErrors - "cantidadPorEmpaque") }
    }

    fun save() {
        if (!validate()) return
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val producto = Producto(
                id = state.producto?.id ?: 0,
                nombre = state.nombre.trim(),
                codigoBarras = state.codigoBarras.trim().ifBlank { null },
                unidadMedida = state.unidadMedida.codigo,
                cantidadPorEmpaque = state.cantidadPorEmpaque.toDouble(),
                esServicio = state.esServicio,
                notas = state.notas.trim().ifBlank { null },
                factorMerma = state.factorMerma.toIntOrNull() ?: 0,
                nutricionPorcionG = state.nutricionPorcionG.toDoubleOrNull(),
                nutricionCalorias = state.nutricionCalorias.toDoubleOrNull(),
                nutricionProteinasG = state.nutricionProteinasG.toDoubleOrNull(),
                nutricionCarbohidratosG = state.nutricionCarbohidratosG.toDoubleOrNull(),
                nutricionGrasasG = state.nutricionGrasasG.toDoubleOrNull(),
                nutricionFibraG = state.nutricionFibraG.toDoubleOrNull(),
                nutricionSodioMg = state.nutricionSodioMg.toDoubleOrNull(),
                nutricionFuente = state.nutricionFuente.trim().ifBlank { null },
                createdAt = state.producto?.createdAt ?: System.currentTimeMillis()
            )

            val result = if (state.isEditMode) {
                repository.update(producto)
            } else {
                repository.insert(producto).map { }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(UiEvent.SaveSuccess)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(UiEvent.ShowError(e.message ?: "Error al guardar"))
                }
            )
        }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        if (!ValidationUtils.isValidName(state.nombre)) {
            errors["nombre"] = "El nombre debe tener al menos 2 caracteres"
        }
        if (!ValidationUtils.isPositiveNumber(state.cantidadPorEmpaque)) {
            errors["cantidadPorEmpaque"] = "Debe ser un numero mayor a 0"
        }

        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
