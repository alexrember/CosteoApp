package com.mg.costeoapp.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mg.costeoapp.feature.auth.data.AuthRepository
import com.mg.costeoapp.feature.sync.data.SyncManager
import com.mg.costeoapp.feature.sync.worker.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncAt: Long? = null,
    val error: String? = null,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val syncScheduler: SyncScheduler,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun syncNow() {
        val user = authRepository.getCurrentUser() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            try {
                val result = syncManager.syncAll(user.id)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncAt = System.currentTimeMillis(),
                        pushedCount = result.pushedCount,
                        pulledCount = result.pulledCount,
                        error = if (result.errors.isNotEmpty()) result.errors.joinToString("; ") else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = e.message ?: "Error desconocido al sincronizar"
                    )
                }
            }
        }
    }

    fun schedulePeriodicSync() {
        syncScheduler.schedulePeriodicSync()
    }
}
