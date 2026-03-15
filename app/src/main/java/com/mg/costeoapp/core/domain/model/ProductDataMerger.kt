package com.mg.costeoapp.core.domain.model

/**
 * Combina datos de multiples fuentes y resuelve conflictos.
 *
 * Reglas:
 * - Si solo 1 fuente tiene el dato → Resolved
 * - Si multiples fuentes coinciden → Resolved (usa la primera)
 * - Si multiples fuentes difieren → Conflict (usuario elige)
 * - Si ninguna fuente tiene el dato → Empty
 *
 * Para textos, "coinciden" significa que son similares (normalizado, >=80% match).
 * Para numeros, "coinciden" si son exactamente iguales.
 */
object ProductDataMerger {

    fun merge(sources: List<ProductDataSource>): MergedProductData {
        if (sources.isEmpty()) return MergedProductData()

        return MergedProductData(
            nombre = resolveField(sources) { it.nombre },
            precio = resolveField(sources) { it.precio },
            unidadMedida = resolveField(sources) { it.unidadMedida },
            cantidadPorEmpaque = resolveField(sources) { it.cantidadPorEmpaque },
            unidadesPorEmpaque = resolveField(sources) { it.unidadesPorEmpaque },
            sources = sources.map { it.sourceName }
        )
    }

    private fun <T> resolveField(
        sources: List<ProductDataSource>,
        extractor: (ProductDataSource) -> T?
    ): FieldResolution<T> {
        val options = sources.mapNotNull { source ->
            extractor(source)?.let { value ->
                FieldOption(value = value, source = source.sourceName)
            }
        }

        return when {
            options.isEmpty() -> FieldResolution.Empty
            options.size == 1 -> FieldResolution.Resolved(options[0].value, options[0].source)
            allEqual(options.map { it.value }) -> FieldResolution.Resolved(options[0].value, options[0].source)
            else -> {
                // Deduplicar valores similares para textos
                val deduplicated = deduplicateOptions(options)
                if (deduplicated.size == 1) {
                    FieldResolution.Resolved(deduplicated[0].value, deduplicated[0].source)
                } else {
                    FieldResolution.Conflict(deduplicated)
                }
            }
        }
    }

    private fun <T> allEqual(values: List<T>): Boolean {
        if (values.isEmpty()) return true
        val first = values[0]
        return values.all {
            when {
                first is String && it is String -> normalizeText(first) == normalizeText(it)
                else -> first == it
            }
        }
    }

    private fun <T> deduplicateOptions(options: List<FieldOption<T>>): List<FieldOption<T>> {
        val seen = mutableSetOf<String>()
        return options.filter { option ->
            val key = when (val v = option.value) {
                is String -> normalizeText(v)
                else -> v.toString()
            }
            seen.add(key)
        }
    }

    private fun normalizeText(text: String): String =
        text.trim().lowercase().replace(Regex("\\s+"), " ")
}
