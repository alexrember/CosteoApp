package com.mg.costeoapp.feature.inventario.data.mapper

import com.mg.costeoapp.core.util.UnidadMedida

object SmartDefaults {

    private val englishToSpanish = mapOf(
        "gum" to "chicle", "sugarfree" to "sin azucar", "sugar free" to "sin azucar",
        "milk" to "leche", "cheese" to "queso", "butter" to "mantequilla",
        "chicken" to "pollo", "rice" to "arroz", "beans" to "frijoles",
        "bread" to "pan", "juice" to "jugo", "water" to "agua",
        "oil" to "aceite", "salt" to "sal", "sugar" to "azucar",
        "flour" to "harina", "coffee" to "cafe", "tea" to "te",
        "chocolate" to "chocolate", "cream" to "crema", "egg" to "huevo",
        "cereal" to "cereal", "pasta" to "pasta", "sauce" to "salsa",
        "soap" to "jabon", "shampoo" to "shampoo", "cookie" to "galleta",
        "cracker" to "galleta", "chips" to "papas", "soda" to "refresco",
        "yogurt" to "yogurt", "ham" to "jamon", "tuna" to "atun"
    )

    /**
     * Traduce terminos comunes en ingles a español para mejorar busqueda en tiendas SV.
     * "Sugarfree gum" → "chicle sin azucar"
     */
    fun translateForSearch(name: String): String {
        var result = name.lowercase()
        for ((en, es) in englishToSpanish) {
            result = result.replace(en, es)
        }
        return result.trim()
    }

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
