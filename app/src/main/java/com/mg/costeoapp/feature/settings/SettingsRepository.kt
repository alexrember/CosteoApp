package com.mg.costeoapp.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2);
    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private val themeKey = intPreferencesKey("theme_mode")
    private val stockThresholdKey = doublePreferencesKey("stock_bajo_threshold")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromValue(prefs[themeKey] ?: 0)
    }

    val stockBajoThreshold: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[stockThresholdKey] ?: 5.0
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.value }
    }

    suspend fun setStockBajoThreshold(threshold: Double) {
        context.dataStore.edit { it[stockThresholdKey] = threshold }
    }
}
