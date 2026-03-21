package com.mg.costeoapp.feature.inventario.data.voice

import com.mg.costeoapp.core.util.UnidadMedida

data class VoiceParseResult(
    val nombre: String? = null,
    val cantidad: Double? = null,
    val unidad: UnidadMedida? = null,
    val precio: Double? = null
)

object VoiceInputParser {

    private val numberWords = mapOf(
        "cero" to 0.0, "un" to 1.0, "uno" to 1.0, "una" to 1.0,
        "dos" to 2.0, "tres" to 3.0, "cuatro" to 4.0, "cinco" to 5.0,
        "seis" to 6.0, "siete" to 7.0, "ocho" to 8.0, "nueve" to 9.0,
        "diez" to 10.0, "once" to 11.0, "doce" to 12.0, "trece" to 13.0,
        "catorce" to 14.0, "quince" to 15.0, "dieciseis" to 16.0,
        "diecisiete" to 17.0, "dieciocho" to 18.0, "diecinueve" to 19.0,
        "veinte" to 20.0, "veintiuno" to 21.0, "veintidos" to 22.0,
        "veintitres" to 23.0, "veinticuatro" to 24.0, "veinticinco" to 25.0,
        "veintiseis" to 26.0, "veintisiete" to 27.0, "veintiocho" to 28.0,
        "veintinueve" to 29.0, "treinta" to 30.0, "cuarenta" to 40.0,
        "cincuenta" to 50.0, "sesenta" to 60.0, "setenta" to 70.0,
        "ochenta" to 80.0, "noventa" to 90.0, "cien" to 100.0,
        "ciento" to 100.0, "doscientos" to 200.0, "trescientos" to 300.0,
        "cuatrocientos" to 400.0, "quinientos" to 500.0, "seiscientos" to 600.0,
        "setecientos" to 700.0, "ochocientos" to 800.0, "novecientos" to 900.0,
        "mil" to 1000.0, "medio" to 0.5, "media" to 0.5,
        "cuarto" to 0.25
    )

    private val unitKeywords = mapOf(
        "libra" to UnidadMedida.LIBRA, "libras" to UnidadMedida.LIBRA,
        "kilo" to UnidadMedida.KILOGRAMO, "kilos" to UnidadMedida.KILOGRAMO,
        "kilogramo" to UnidadMedida.KILOGRAMO, "kilogramos" to UnidadMedida.KILOGRAMO,
        "gramo" to UnidadMedida.GRAMO, "gramos" to UnidadMedida.GRAMO,
        "litro" to UnidadMedida.LITRO, "litros" to UnidadMedida.LITRO,
        "mililitro" to UnidadMedida.MILILITRO, "mililitros" to UnidadMedida.MILILITRO,
        "onza" to UnidadMedida.ONZA, "onzas" to UnidadMedida.ONZA,
        "unidad" to UnidadMedida.UNIDAD, "unidades" to UnidadMedida.UNIDAD
    )

    private val priceIndicators = setOf(
        "dolar", "dolares", "dólar", "dólares", "precio", "cuesta", "vale", "por", "a"
    )

    fun parse(input: String): VoiceParseResult {
        val normalized = input.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            .replace(",", " ").trim()

        val tokens = normalized.split("\\s+".toRegex())

        val precio = extractPrice(tokens, normalized)
        val unitResult = extractUnit(tokens)
        val cantidad = extractQuantity(tokens, unitResult?.tokenIndex)
        val nombre = extractName(tokens, cantidad, unitResult, precio)

        return VoiceParseResult(
            nombre = nombre?.replaceFirstChar { it.uppercase() },
            cantidad = cantidad?.value,
            unidad = unitResult?.unit,
            precio = precio?.value
        )
    }

    private data class UnitMatch(val unit: UnidadMedida, val tokenIndex: Int)
    private data class NumberMatch(val value: Double, val startIndex: Int, val endIndex: Int)
    private data class PriceMatch(val value: Double, val startIndex: Int, val endIndex: Int)

    private fun extractPrice(tokens: List<String>, raw: String): PriceMatch? {
        // Pattern: "N dolares con N" or "N dolares N centavos"
        for (i in tokens.indices) {
            if (tokens[i] in setOf("dolar", "dolares", "dollar", "dollars")) {
                val beforeNum = parseNumberBackward(tokens, i - 1)
                if (beforeNum != null) {
                    var cents = 0.0
                    // Check for "con X" or "X centavos" after
                    val afterIdx = i + 1
                    if (afterIdx < tokens.size && tokens[afterIdx] == "con") {
                        val centsNum = parseNumberForward(tokens, afterIdx + 1)
                        if (centsNum != null) {
                            cents = if (centsNum.value < 1.0) centsNum.value else centsNum.value / 100.0
                            return PriceMatch(beforeNum.value + cents, beforeNum.startIndex, centsNum.endIndex)
                        }
                    }
                    if (afterIdx < tokens.size) {
                        val centsNum = parseNumberForward(tokens, afterIdx)
                        if (centsNum != null && centsNum.endIndex < tokens.size &&
                            tokens[centsNum.endIndex] in setOf("centavos", "centavo")) {
                            cents = centsNum.value / 100.0
                            return PriceMatch(beforeNum.value + cents, beforeNum.startIndex, centsNum.endIndex)
                        }
                    }
                    return PriceMatch(beforeNum.value, beforeNum.startIndex, i)
                }
            }
        }

        // Pattern: "a N" or "por N" or "precio N"
        for (i in tokens.indices) {
            if (tokens[i] in setOf("a", "por", "precio", "cuesta", "vale") && i + 1 < tokens.size) {
                val num = parseNumberForward(tokens, i + 1)
                if (num != null) {
                    // Check for "con X" after
                    val afterIdx = num.endIndex + 1
                    if (afterIdx < tokens.size && tokens[num.endIndex] == "con") {
                        val centsNum = parseNumberForward(tokens, afterIdx)
                        if (centsNum != null) {
                            val cents = if (centsNum.value < 1.0) centsNum.value else centsNum.value / 100.0
                            return PriceMatch(num.value + cents, i, centsNum.endIndex)
                        }
                    }
                    // Skip "dolares" if present after the number
                    val nextIdx = num.endIndex
                    val finalIdx = if (nextIdx < tokens.size && tokens[nextIdx] in setOf("dolar", "dolares")) nextIdx else num.endIndex - 1
                    return PriceMatch(num.value, i, finalIdx)
                }
            }
        }

        // Pattern: decimal number like "3.50" or "10.99" at the end
        val decimalPattern = Regex("""(\d+\.\d{1,2})""")
        val decimalMatch = decimalPattern.findAll(raw).lastOrNull()
        if (decimalMatch != null) {
            val value = decimalMatch.value.toDoubleOrNull()
            if (value != null) {
                val matchStart = decimalMatch.range.first
                val tokenIdx = raw.substring(0, matchStart).split("\\s+".toRegex()).size - 1
                return PriceMatch(value, maxOf(0, tokenIdx), tokenIdx)
            }
        }

        return null
    }

    private fun extractUnit(tokens: List<String>): UnitMatch? {
        for (i in tokens.indices) {
            val unit = unitKeywords[tokens[i]]
            if (unit != null) return UnitMatch(unit, i)
        }
        return null
    }

    private fun extractQuantity(tokens: List<String>, unitTokenIndex: Int?): NumberMatch? {
        if (unitTokenIndex != null && unitTokenIndex > 0) {
            val num = parseNumberBackward(tokens, unitTokenIndex - 1)
            if (num != null) return num
        }
        return null
    }

    private fun extractName(
        tokens: List<String>,
        cantidad: NumberMatch?,
        unitResult: UnitMatch?,
        precio: PriceMatch?
    ): String? {
        val excludedIndices = mutableSetOf<Int>()

        if (cantidad != null) {
            for (i in cantidad.startIndex..cantidad.endIndex) excludedIndices.add(i)
        }
        if (unitResult != null) {
            excludedIndices.add(unitResult.tokenIndex)
        }
        if (precio != null) {
            for (i in precio.startIndex..precio.endIndex) excludedIndices.add(i)
            // Also exclude "dolares", "con", "centavos" around price
            for (i in maxOf(0, precio.startIndex - 1)..minOf(tokens.size - 1, precio.endIndex + 2)) {
                if (i < tokens.size && tokens[i] in setOf("dolar", "dolares", "con", "centavos", "centavo", "a", "por", "precio", "cuesta", "vale")) {
                    excludedIndices.add(i)
                }
            }
        }

        val nameTokens = tokens.filterIndexed { index, token ->
            index !in excludedIndices &&
                token !in priceIndicators &&
                token !in setOf("dolar", "dolares", "con", "centavos", "centavo") &&
                token !in unitKeywords.keys
        }

        val name = nameTokens.joinToString(" ").trim()
        return name.ifBlank { null }
    }

    private fun parseNumberForward(tokens: List<String>, startIndex: Int): NumberMatch? {
        if (startIndex >= tokens.size) return null

        // Try parsing as a numeric literal first
        val numericVal = tokens[startIndex].toDoubleOrNull()
        if (numericVal != null) {
            return NumberMatch(numericVal, startIndex, startIndex + 1)
        }

        // Try word-based numbers
        var total = 0.0
        var current = 0.0
        var endIdx = startIndex
        var found = false

        for (i in startIndex until tokens.size) {
            val wordVal = numberWords[tokens[i]]
            if (wordVal == null) break
            found = true
            endIdx = i + 1

            when {
                wordVal == 1000.0 -> {
                    current = if (current == 0.0) 1000.0 else current * 1000.0
                    total += current
                    current = 0.0
                }
                wordVal >= 100.0 && wordVal < 1000.0 -> {
                    current += wordVal
                }
                wordVal < 1.0 -> {
                    current += wordVal
                }
                else -> {
                    current += wordVal
                }
            }

            // Handle "y" connector: "treinta y cinco"
            if (i + 1 < tokens.size && tokens[i + 1] == "y" && i + 2 < tokens.size) {
                val nextVal = numberWords[tokens[i + 2]]
                if (nextVal != null && nextVal < 10.0) {
                    current += nextVal
                    endIdx = i + 3
                    break
                }
            }
        }

        if (!found) return null
        total += current
        return NumberMatch(total, startIndex, endIdx)
    }

    private fun parseNumberBackward(tokens: List<String>, endIndex: Int): NumberMatch? {
        if (endIndex < 0 || endIndex >= tokens.size) return null

        // Try numeric literal at endIndex
        val numericVal = tokens[endIndex].toDoubleOrNull()
        if (numericVal != null) {
            return NumberMatch(numericVal, endIndex, endIndex)
        }

        // Collect consecutive number words going backward, then parse forward
        var startIdx = endIndex
        for (i in endIndex downTo 0) {
            if (numberWords.containsKey(tokens[i]) || tokens[i] == "y") {
                startIdx = i
            } else {
                break
            }
        }

        if (startIdx > endIndex) return null
        val result = parseNumberForward(tokens, startIdx) ?: return null
        return result.copy(endIndex = endIndex)
    }
}
