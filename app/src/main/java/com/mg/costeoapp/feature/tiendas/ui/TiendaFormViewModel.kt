package com.mg.costeoapp.feature.tiendas.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.core.util.ValidationUtils
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TiendaFormViewModel @Inject constructor(
    private val repository: TiendaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TiendaFormUiState())
    val uiState: StateFlow<TiendaFormUiState> = _uiState.asStateFlow()

    init {
        val tiendaId = savedStateHandle.get<Long>("tiendaId")
        if (tiendaId != null && tiendaId != 0L) {
            loadTienda(tiendaId)
        }
    }

    private fun loadTienda(id: Long) {
        viewModelScope.launch {
            val tienda = repository.getById(id)
            if (tienda != null) {
                _uiState.update {
                    it.copy(
                        tienda = tienda,
                        nombre = tienda.nombre,
                        direccion = tienda.direccion ?: "",
                        notas = tienda.notas ?: "",
                        tipo = tienda.tipo,
                        telefono = tienda.telefono ?: "",
                        diasCredito = tienda.diasCredito?.toString() ?: ""
                    )
                }
            }
        }
    }

    fun onNombreChanged(value: String) {
        _uiState.update { it.copy(nombre = value, fieldErrors = it.fieldErrors - "nombre") }
    }

    fun onDireccionChanged(value: String) {
        _uiState.update { it.copy(direccion = value) }
    }

    fun onNotasChanged(value: String) {
        _uiState.update { it.copy(notas = value) }
    }

    fun onTipoChanged(value: String) {
        _uiState.update { it.copy(tipo = value) }
    }

    fun onTelefonoChanged(value: String) {
        _uiState.update { it.copy(telefono = value) }
    }

    fun onDiasCreditoChanged(value: String) {
        _uiState.update { it.copy(diasCredito = value) }
    }

    fun save() {
        if (!validate()) return

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val state = _uiState.value
            val tienda = Tienda(
                id = state.tienda?.id ?: 0,
                nombre = state.nombre.trim(),
                direccion = state.direccion.trim().ifBlank { null },
                notas = state.notas.trim().ifBlank { null },
                tipo = state.tipo,
                telefono = state.telefono.trim().ifBlank { null },
                diasCredito = state.diasCredito.toIntOrNull(),
                createdAt = state.tienda?.createdAt ?: System.currentTimeMillis()
            )

            val result = if (state.isEditMode) {
                repository.update(tienda)
            } else {
                repository.insert(tienda).map { }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        if (!ValidationUtils.isValidName(state.nombre)) {
            errors["nombre"] = "El nombre debe tener al menos 2 caracteres"
        }

        if (state.diasCredito.isNotBlank()) {
            val dias = state.diasCredito.toIntOrNull()
            if (dias == null || dias < 0) {
                errors["diasCredito"] = "Debe ser un numero positivo"
            }
        }

        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
