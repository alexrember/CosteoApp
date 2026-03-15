package com.mg.costeoapp.core.util

enum class UnidadMedida(
    val codigo: String,
    val nombreDisplay: String,
    val factorAGramos: Double?
) {
    LIBRA("lb", "Libra", 453.592),
    KILOGRAMO("kg", "Kilogramo", 1000.0),
    GRAMO("g", "Gramo", 1.0),
    ONZA("oz", "Onza", 28.3495),
    LITRO("l", "Litro", 1000.0),
    MILILITRO("ml", "Mililitro", 1.0),
    UNIDAD("unidad", "Unidad", null);

    companion object {
        fun fromCodigo(codigo: String): UnidadMedida? =
            entries.find { it.codigo == codigo.lowercase() }
    }
}
