package com.mg.costeoapp.feature.sync.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mg.costeoapp.feature.auth.data.AuthRepository
import com.mg.costeoapp.feature.sync.data.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "costeo_periodic_sync"
        const val WORK_NAME_IMMEDIATE = "costeo_immediate_sync"
    }

    override suspend fun doWork(): Result {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            Log.d(TAG, "No hay usuario autenticado, omitiendo sync")
            return Result.success()
        }

        return try {
            val result = syncManager.syncAll(user.id)
            if (result.success) {
                Log.d(TAG, "Sync completado: pushed=${result.pushedCount}, pulled=${result.pulledCount}")
                Result.success()
            } else {
                Log.w(TAG, "Sync con errores: ${result.errors}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync fallo", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
