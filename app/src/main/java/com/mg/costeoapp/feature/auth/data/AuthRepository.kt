package com.mg.costeoapp.feature.auth.data

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val createdAt: Long
)

sealed class AuthState {
    data object LocalOnly : AuthState()
    data object Loading : AuthState()
    data class LoggedIn(val user: AuthUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun signOut()
    fun getCurrentUser(): AuthUser?
}
