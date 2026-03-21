package com.mg.costeoapp.feature.auth.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> {
        return supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = getCurrentUser()
                    if (user != null) AuthState.LoggedIn(user) else AuthState.LocalOnly
                }
                is SessionStatus.NotAuthenticated -> AuthState.LocalOnly
                is SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.RefreshFailure -> AuthState.Error("Error al renovar sesion")
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = getCurrentUser() ?: return Result.failure(Exception("No se pudo obtener usuario"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapAuthError(e))
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = getCurrentUser() ?: return Result.failure(Exception("Cuenta creada. Verifica tu correo."))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapAuthError(e))
        }
    }

    override suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (_: Exception) { }
    }

    override fun getCurrentUser(): AuthUser? {
        val session = supabaseClient.auth.currentUserOrNull() ?: return null
        return AuthUser(
            id = session.id,
            email = session.email ?: "",
            displayName = session.userMetadata?.get("display_name")?.toString(),
            createdAt = session.createdAt?.toEpochMilliseconds() ?: 0L
        )
    }

    private fun mapAuthError(e: Exception): Exception {
        val message = when {
            e.message?.contains("Invalid login", ignoreCase = true) == true ->
                "Correo o contrasena incorrectos"
            e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                "Verifica tu correo antes de iniciar sesion"
            e.message?.contains("User already registered", ignoreCase = true) == true ->
                "Ya existe una cuenta con este correo"
            e.message?.contains("Password", ignoreCase = true) == true ->
                "La contrasena debe tener al menos 6 caracteres"
            e.message?.contains("network", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve", ignoreCase = true) == true ->
                "Sin conexion a internet"
            else -> e.message ?: "Error desconocido"
        }
        return Exception(message)
    }
}
