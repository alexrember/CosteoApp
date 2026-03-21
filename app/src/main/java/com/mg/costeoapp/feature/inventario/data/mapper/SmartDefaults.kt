package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.core.util.UnidadMedida

object SmartDefaults {
    fun suggestUnit(productName: String): UnidadMedida {
        val lower = productName.lowercase()
        return when {
            lower.contains("leche") || lower.contains("jugo") || lower.contains("aceite") || lower.contains("refresco") -> UnidadMedida.MILILITRO
            lower.contains("arroz") || lower.contains("frijol") || lower.contains("azucar") || lower.contains("harina") || lower.contains("pollo") || lower.contains("carne") || lower.contains("queso") -> UnidadMedida.LIBRA
            lower.contains("sal") || lower.contains("cafe") || lower.contains("pasta") || lower.contains("cereal") -> UnidadMedida.GRAMO
            else -> UnidadMedida.UNIDAD
        }
    }
}
