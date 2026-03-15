package com.mg.costeoapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application principal de CosteoApp.
 * Inicializa Hilt para inyeccion de dependencias.
 */
@HiltAndroidApp
class CosteoApp : Application()
