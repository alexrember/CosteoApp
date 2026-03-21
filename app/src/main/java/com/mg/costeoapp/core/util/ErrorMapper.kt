package com.mg.costeoapp.core.util

object ErrorMapper {
    fun toUserMessage(e: Throwable): String = when {
        e is java.net.UnknownHostException -> "Sin conexion a internet"
        e is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
        e is android.database.sqlite.SQLiteConstraintException -> "Este registro ya existe"
        e.message?.contains("UNIQUE constraint") == true -> "Ya existe un registro con ese nombre"
        else -> "Error inesperado. Intenta de nuevo."
    }
}
