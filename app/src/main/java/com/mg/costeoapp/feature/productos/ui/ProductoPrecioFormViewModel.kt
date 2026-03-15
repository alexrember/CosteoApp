package com.mg.costeoapp.feature.productos.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.util.CurrencyFormatter
import com.mg.costeoapp.feature.productos.data.ProductoRepository
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductoPrecioFormViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val tiendaRepository: TiendaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoPrecioFormUiState())
    val uiState: StateFlow<ProductoPrecioFormUiState> = _uiState.asStateFlow()

    init {
        val productoId = savedStateHandle.get<Long>("productoId")
        if (productoId != null && productoId != 0L) {
            loadData(productoId)
        }
    }

    private fun loadData(productoId: Long) {
        viewModelScope.launch {
            val producto = productoRepository.getById(productoId)
            val tiendas = tiendaRepository.getAll().first()
            _uiState.update {
                it.copy(
                    productoId = productoId,
                    productoNombre = producto?.nombre ?: "",
                    tiendasDisponibles = tiendas
                )
            }
        }
    }

    fun onTiendaSelected(tiendaId: Long) {
        _uiState.update { it.copy(tiendaSeleccionadaId = tiendaId, fieldErrors = it.fieldErrors - "tienda") }
    }

    fun onPrecioChanged(value: String) {
        _uiState.update { it.copy(precio = value, fieldErrors = it.fieldErrors - "precio") }
    }

    fun save() {
        if (!validate()) return

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val state = _uiState.value
            val precioCents = CurrencyFormatter.toCents(state.precio)

            if (precioCents == null) {
                _uiState.update { it.copy(isSaving = false, error = "Precio invalido") }
                return@launch
            }

            val productoTienda = ProductoTienda(
                productoId = state.productoId,
                tiendaId = state.tiendaSeleccionadaId!!,
                precio = precioCents
            )

            productoRepository.insertPrecio(productoTienda).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun resetSaveSuccess() { _uiState.update { it.copy(saveSuccess = false) } }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        if (state.tiendaSeleccionadaId == null) {
            errors["tienda"] = "Selecciona una tienda"
        }
        val cents = CurrencyFormatter.toCents(state.precio)
        if (cents == null || cents < 0) {
            errors["precio"] = "Ingresa un precio valido"
        }

        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
