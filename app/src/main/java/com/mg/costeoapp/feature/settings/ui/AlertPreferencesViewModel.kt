package com.mg.costeoapp.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.settings.data.AlertPreferences
import com.mg.costeoapp.feature.settings.data.AlertPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertPreferencesUiState(
    val priceDropThreshold: Int = 10,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val alertsEnabled: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class AlertPreferencesViewModel @Inject constructor(
    private val repository: AlertPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertPreferencesUiState())
    val uiState: StateFlow<AlertPreferencesUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.load()
                .onSuccess { prefs ->
                    _uiState.update {
                        it.copy(
                            priceDropThreshold = prefs.priceDropThreshold,
                            quietHoursStart = prefs.quietHoursStart,
                            quietHoursEnd = prefs.quietHoursEnd,
                            alertsEnabled = prefs.alertsEnabled,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Error al cargar preferencias: ${e.message}"
                        )
                    }
                }
        }
    }

    fun setPriceDropThreshold(value: Int) {
        _uiState.update { it.copy(priceDropThreshold = value) }
    }

    fun setQuietHoursStart(value: Int) {
        _uiState.update { it.copy(quietHoursStart = value) }
    }

    fun setQuietHoursEnd(value: Int) {
        _uiState.update { it.copy(quietHoursEnd = value) }
    }

    fun setAlertsEnabled(value: Boolean) {
        _uiState.update { it.copy(alertsEnabled = value) }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val prefs = AlertPreferences(
                priceDropThreshold = state.priceDropThreshold,
                quietHoursStart = state.quietHoursStart,
                quietHoursEnd = state.quietHoursEnd,
                alertsEnabled = state.alertsEnabled
            )
            repository.save(prefs)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, message = "Preferencias guardadas")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = "Error al guardar: ${e.message}")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
