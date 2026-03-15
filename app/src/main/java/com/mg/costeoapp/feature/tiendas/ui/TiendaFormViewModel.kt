package com.mg.costeoapp.feature.tiendas.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.core.ui.viewmodel.UiEvent
import com.mg.costeoapp.core.util.ValidationUtils
import com.mg.costeoapp.feature.tiendas.data.TiendaRepository
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
class TiendaFormViewModel @Inject constructor(
    private val repository: TiendaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TiendaFormUiState())
    val uiState: StateFlow<TiendaFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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
                    it.copy(tienda = tienda, nombre = tienda.nombre)
                }
            }
        }
    }

    fun onNombreChanged(value: String) {
        _uiState.update { it.copy(nombre = value, fieldErrors = it.fieldErrors - "nombre") }
    }

    fun save() {
        if (!validate()) return
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val tienda = Tienda(
                id = state.tienda?.id ?: 0,
                nombre = state.nombre.trim(),
                createdAt = state.tienda?.createdAt ?: System.currentTimeMillis()
            )

            val result = if (state.isEditMode) {
                repository.update(tienda)
            } else {
                repository.insert(tienda).map { }
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

        if (!ValidationUtils.isValidName(_uiState.value.nombre)) {
            errors["nombre"] = "El nombre debe tener al menos 2 caracteres"
        }

        _uiState.update { it.copy(fieldErrors = errors) }
        return errors.isEmpty()
    }
}
