package com.mg.costeoapp.feature.inventario.data.ocr

import com.google.mlkit.vision.text.Text

class NutritionLabelParser {

    companion object {
        private val PORCION_PATTERN = Regex(
            """(?:porci[oó]n|serving\s*size|tama[nñ]o\s*de\s*porci[oó]n)[:\s]*(\d+(?:[.,]\d+)?)\s*g""",
            RegexOption.IGNORE_CASE
        )
        private val CALORIAS_PATTERN = Regex(
            """(?:calor[ií]as|calories|energ[ií]a|energy)[:\s]*(\d+(?:[.,]\d+)?)""",
            RegexOption.IGNORE_CASE
        )
        private val PROTEINAS_PATTERN = Regex(
            """(?:prote[ií]na|protein)[s]?[:\s]*(\d+(?:[.,]\d+)?)\s*g""",
            RegexOption.IGNORE_CASE
        )
        private val CARBOHIDRATOS_PATTERN = Regex(
            """(?:carbohidrato|carbohydrate|carbs|h\.\s*de\s*c)[s]?[:\s]*(\d+(?:[.,]\d+)?)\s*g""",
            RegexOption.IGNORE_CASE
        )
        private val GRASAS_PATTERN = Regex(
            """(?:grasa\s*total|total\s*fat|grasas|fat)[:\s]*(\d+(?:[.,]\d+)?)\s*g""",
            RegexOption.IGNORE_CASE
        )
        private val FIBRA_PATTERN = Regex(
            """(?:fibra|fiber)[:\s]*(\d+(?:[.,]\d+)?)\s*g""",
            RegexOption.IGNORE_CASE
        )
        private val SODIO_PATTERN = Regex(
            """(?:sodio|sodium)[:\s]*(\d+(?:[.,]\d+)?)\s*(?:mg|g)""",
            RegexOption.IGNORE_CASE
        )
        private val SODIO_EN_GRAMOS = Regex(
            """(?:sodio|sodium)[:\s]*\d+(?:[.,]\d+)?\s*g(?!r)""",
            RegexOption.IGNORE_CASE
        )
    }

    fun parse(textResult: Text): NutricionOcrResult {
        val fullText = textResult.text
        var matchCount = 0
        val totalFields = 7

        val porcion = extractDouble(PORCION_PATTERN, fullText)?.also { matchCount++ }
        val calorias = extractDouble(CALORIAS_PATTERN, fullText)?.also { matchCount++ }
        val proteinas = extractDouble(PROTEINAS_PATTERN, fullText)?.also { matchCount++ }
        val carbohidratos = extractDouble(CARBOHIDRATOS_PATTERN, fullText)?.also { matchCount++ }
        val grasas = extractDouble(GRASAS_PATTERN, fullText)?.also { matchCount++ }
        val fibra = extractDouble(FIBRA_PATTERN, fullText)?.also { matchCount++ }

        var sodio = extractDouble(SODIO_PATTERN, fullText)?.also { matchCount++ }
        if (sodio != null && SODIO_EN_GRAMOS.containsMatchIn(fullText)) {
            sodio = sodio * 1000
        }

        return NutricionOcrResult(
            porcionG = porcion,
            calorias = calorias,
            proteinas = proteinas,
            carbohidratos = carbohidratos,
            grasas = grasas,
            fibra = fibra,
            sodioMg = sodio,
            rawText = fullText,
            confidence = matchCount.toFloat() / totalFields.toFloat()
        )
    }

    fun parseFromText(text: String): NutricionOcrResult {
        var matchCount = 0
        val totalFields = 7

        val porcion = extractDouble(PORCION_PATTERN, text)?.also { matchCount++ }
        val calorias = extractDouble(CALORIAS_PATTERN, text)?.also { matchCount++ }
        val proteinas = extractDouble(PROTEINAS_PATTERN, text)?.also { matchCount++ }
        val carbohidratos = extractDouble(CARBOHIDRATOS_PATTERN, text)?.also { matchCount++ }
        val grasas = extractDouble(GRASAS_PATTERN, text)?.also { matchCount++ }
        val fibra = extractDouble(FIBRA_PATTERN, text)?.also { matchCount++ }

        var sodio = extractDouble(SODIO_PATTERN, text)?.also { matchCount++ }
        if (sodio != null && SODIO_EN_GRAMOS.containsMatchIn(text)) {
            sodio = sodio * 1000
        }

        return NutricionOcrResult(
            porcionG = porcion,
            calorias = calorias,
            proteinas = proteinas,
            carbohidratos = carbohidratos,
            grasas = grasas,
            fibra = fibra,
            sodioMg = sodio,
            rawText = text,
            confidence = matchCount.toFloat() / totalFields.toFloat()
        )
    }

    private fun extractDouble(pattern: Regex, text: String): Double? {
        return pattern.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
    }
}
