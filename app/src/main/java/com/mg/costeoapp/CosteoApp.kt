package com.mg.costeoapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mg.costeoapp.feature.inventario.data.CompraManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CosteoApp : Application(), Configuration.Provider {

    @Inject lateinit var compraManager: CompraManager
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncManager: com.mg.costeoapp.feature.sync.data.SyncManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { compraManager.restaurarDesdeDb() }
        syncManager.pushInBackground(delayMs = 3000)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
