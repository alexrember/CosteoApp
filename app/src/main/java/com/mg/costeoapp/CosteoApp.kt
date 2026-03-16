package com.mg.costeoapp

import android.app.Application
import com.mg.costeoapp.feature.inventario.data.CompraManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CosteoApp : Application() {

    @Inject lateinit var compraManager: CompraManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { compraManager.restaurarDesdeDb() }
    }
}
