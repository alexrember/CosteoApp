package com.mg.costeoapp.feature.settings.data

import com.mg.costeoapp.feature.auth.data.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

data class AlertPreferences(
    val priceDropThreshold: Int = 10,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    val alertsEnabled: Boolean = true
)

@Serializable
data class AlertPreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("price_drop_threshold") val priceDropThreshold: Int = 10,
    @SerialName("quiet_hours_start") val quietHoursStart: Int = 22,
    @SerialName("quiet_hours_end") val quietHoursEnd: Int = 7,
    @SerialName("alerts_enabled") val alertsEnabled: Boolean = true
)

private fun AlertPreferencesDto.toDomain() = AlertPreferences(
    priceDropThreshold = priceDropThreshold,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    alertsEnabled = alertsEnabled
)

@Singleton
class AlertPreferencesRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TABLE = "user_alert_preferences"
    }

    suspend fun load(): Result<AlertPreferences> = runCatching {
        withContext(Dispatchers.IO) {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("Usuario no autenticado")

            val dto = supabase.from(TABLE)
                .select { filter { eq("user_id", user.id) } }
                .decodeSingleOrNull<AlertPreferencesDto>()

            dto?.toDomain() ?: AlertPreferences()
        }
    }

    suspend fun save(prefs: AlertPreferences): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val user = authRepository.getCurrentUser()
                ?: throw IllegalStateException("Usuario no autenticado")

            val dto = AlertPreferencesDto(
                userId = user.id,
                priceDropThreshold = prefs.priceDropThreshold,
                quietHoursStart = prefs.quietHoursStart,
                quietHoursEnd = prefs.quietHoursEnd,
                alertsEnabled = prefs.alertsEnabled
            )

            supabase.from(TABLE).upsert(dto) {
                onConflict = "user_id"
            }
        }
    }
}
