package com.mg.costeoapp.feature.auth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMappingTest {

    /**
     * Replica la logica de mapAuthError de SupabaseAuthRepository
     * para poder testearla sin necesitar un SupabaseClient real.
     * Si la logica en el repo cambia, estos tests deben actualizarse.
     */
    private fun mapAuthError(e: Exception): Exception {
        val message = when {
            e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                "Credenciales incorrectas"
            e.message?.contains("User already registered", ignoreCase = true) == true ->
                "Este correo ya tiene cuenta"
            e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                "Confirma tu correo electronico primero"
            e.message?.contains("Password should be at least", ignoreCase = true) == true ->
                "La contrasena debe tener al menos 6 caracteres"
            e.message?.contains("network", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve", ignoreCase = true) == true ||
            e.message?.contains("timeout", ignoreCase = true) == true ||
            e.message?.contains("UnknownHost", ignoreCase = true) == true ->
                "Sin conexion a internet"
            else -> "Error: ${e.message ?: "desconocido"}"
        }
        return Exception(message)
    }

    private fun mapMessage(input: String?): String {
        return mapAuthError(Exception(input)).message!!
    }

    @Test
    fun `credenciales invalidas`() {
        assertEquals("Credenciales incorrectas", mapMessage("Invalid login credentials"))
    }

    @Test
    fun `usuario ya registrado`() {
        assertEquals("Este correo ya tiene cuenta", mapMessage("User already registered"))
    }

    @Test
    fun `email no confirmado`() {
        assertEquals("Confirma tu correo electronico primero", mapMessage("Email not confirmed"))
    }

    @Test
    fun `contrasena muy corta`() {
        assertEquals(
            "La contrasena debe tener al menos 6 caracteres",
            mapMessage("Password should be at least 6 characters")
        )
    }

    @Test
    fun `error de red con network`() {
        assertEquals("Sin conexion a internet", mapMessage("network error occurred"))
    }

    @Test
    fun `error de red con Unable to resolve`() {
        assertEquals("Sin conexion a internet", mapMessage("Unable to resolve host"))
    }

    @Test
    fun `error de red con timeout`() {
        assertEquals("Sin conexion a internet", mapMessage("Connection timeout"))
    }

    @Test
    fun `error de red con UnknownHost`() {
        assertEquals("Sin conexion a internet", mapMessage("java.net.UnknownHostException"))
    }

    @Test
    fun `error desconocido incluye mensaje original`() {
        assertEquals("Error: algo raro paso", mapMessage("algo raro paso"))
    }

    @Test
    fun `error con mensaje null`() {
        assertEquals("Error: desconocido", mapMessage(null))
    }

    @Test
    fun `case insensitive funciona`() {
        assertEquals("Credenciales incorrectas", mapMessage("INVALID LOGIN CREDENTIALS"))
        assertEquals("Este correo ya tiene cuenta", mapMessage("user already registered"))
    }

    // -----------------------------------------------------------------------
    // AuthState sealed class behavior
    // -----------------------------------------------------------------------

    @Test
    fun `AuthState LocalOnly es singleton`() {
        val a = AuthState.LocalOnly
        val b = AuthState.LocalOnly
        assertTrue(a === b)
    }

    @Test
    fun `AuthState Loading es singleton`() {
        val a = AuthState.Loading
        val b = AuthState.Loading
        assertTrue(a === b)
    }

    @Test
    fun `AuthState LoggedIn contiene usuario`() {
        val user = AuthUser(id = "abc", email = "test@test.com", displayName = "Test", createdAt = 0L)
        val state = AuthState.LoggedIn(user)
        assertEquals("abc", state.user.id)
        assertEquals("test@test.com", state.user.email)
        assertEquals("Test", state.user.displayName)
    }

    @Test
    fun `AuthState Error contiene mensaje`() {
        val state = AuthState.Error("Algo fallo")
        assertEquals("Algo fallo", state.message)
    }

    @Test
    fun `AuthState subtipos son distintos`() {
        val states: List<AuthState> = listOf(
            AuthState.LocalOnly,
            AuthState.Loading,
            AuthState.LoggedIn(AuthUser("1", "a@b.com", null, 0L)),
            AuthState.Error("err")
        )
        assertEquals(4, states.size)
        assertTrue(states[0] is AuthState.LocalOnly)
        assertTrue(states[1] is AuthState.Loading)
        assertTrue(states[2] is AuthState.LoggedIn)
        assertTrue(states[3] is AuthState.Error)
    }

    @Test
    fun `AuthUser con displayName null`() {
        val user = AuthUser(id = "x", email = "x@x.com", displayName = null, createdAt = 0L)
        assertEquals(null, user.displayName)
    }
}
