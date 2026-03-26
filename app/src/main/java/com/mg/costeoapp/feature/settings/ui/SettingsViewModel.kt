package com.mg.costeoapp.feature.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.settings.SettingsRepository
import com.mg.costeoapp.feature.settings.ThemeMode
import com.mg.costeoapp.feature.settings.data.BackupRestoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val stockBajoThreshold: Double = 5.0,
    val backupMessage: String? = null,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
    val importSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val backupRestoreService: BackupRestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            repository.stockBajoThreshold.collect { threshold ->
                _uiState.update { it.copy(stockBajoThreshold = threshold) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setStockBajoThreshold(threshold: Double) {
        viewModelScope.launch { repository.setStockBajoThreshold(threshold) }
    }

    fun exportBackup(context: Context): Intent? {
        val uri = backupRestoreService.exportDatabase(context)
        if (uri == null) {
            _uiState.update { it.copy(backupMessage = "Error al exportar la base de datos") }
            return null
        }
        _uiState.update { it.copy(backupMessage = "El respaldo no esta encriptado. No lo compartas con personas no confiables.") }
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun requestImportBackup(uri: Uri) {
        _uiState.update { it.copy(showImportConfirmDialog = true, pendingImportUri = uri) }
    }

    fun confirmImportBackup(context: Context) {
        val uri = _uiState.value.pendingImportUri ?: return
        _uiState.update { it.copy(showImportConfirmDialog = false, pendingImportUri = null) }

        viewModelScope.launch {
            val result = backupRestoreService.importDatabase(context, uri)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(backupMessage = "Importando base de datos... La app se reiniciara automaticamente.", importSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(backupMessage = error.message ?: "Error al importar") }
                }
            )
        }
    }

    fun cancelImportBackup() {
        _uiState.update { it.copy(showImportConfirmDialog = false, pendingImportUri = null) }
    }

    fun clearBackupMessage() {
        _uiState.update { it.copy(backupMessage = null) }
    }
}
