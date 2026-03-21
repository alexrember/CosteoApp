package com.mg.costeoapp.feature.inventario.data.ocr

import com.google.mlkit.vision.text.Text

class NutritionLabelParser {

    companion object {
        private const val NUM = """(\d+(?:[.,]\d+)?)"""
        private const val SEP = """[:\s]*"""
        private const val OPT_SPACE = """\s*"""

        private val PORCION_PATTERN = Regex(
            """(?:porci[oó]n|serving\s*size|tama[nñ]o\s*de\s*(?:la\s*)?porci[oó]n|ration|raci[oó]n)$SEP$NUM$OPT_SPACE(?:g(?:r)?|gramos?)""",
            RegexOption.IGNORE_CASE
        )

        private val CALORIAS_PATTERN = Regex(
            """(?:calor[ií]as?|calories|energ[ií]a|energy|valor\s*energ[eé]tico)$SEP$NUM$OPT_SPACE(?:kcal|cal|kj)?""",
            RegexOption.IGNORE_CASE
        )

        private val CALORIAS_SUFFIX_PATTERN = Regex(
            """$NUM$OPT_SPACE(?:kcal|cal)\b""",
            RegexOption.IGNORE_CASE
        )

        private val PROTEINAS_PATTERN = Regex(
            """(?:prote[ií]nas?|proteins?|prot[.]?)$SEP$NUM$OPT_SPACE g""",
            RegexOption.IGNORE_CASE
        )

        private val PROTEINAS_PATTERN_NO_UNIT = Regex(
            """(?:prote[ií]nas?|proteins?|prot[.]?)$SEP$NUM$OPT_SPACE(?:g(?:r)?|gramos?)""",
            RegexOption.IGNORE_CASE
        )

        private val CARBOHIDRATOS_PATTERN = Regex(
            """(?:carbohidratos?\s*(?:totales?)?|carbohydrates?|total\s*carb(?:ohidrato)?s?|hidratos?\s*de\s*carbono|h[.]?\s*de\s*c[.]?|carbs?|gluc[ií]dos?)$SEP$NUM$OPT_SPACE(?:g(?:r)?|gramos?)""",
            RegexOption.IGNORE_CASE
        )

        private val GRASAS_PATTERN = Regex(
            """(?:grasas?\s*(?:totale?s?)?|total\s*fat|fat|l[ií]pidos?|mat(?:eria)?\s*grasa|grasa)$SEP$NUM$OPT_SPACE(?:g(?:r)?|gramos?)""",
            RegexOption.IGNORE_CASE
        )

        private val FIBRA_PATTERN = Regex(
            """(?:fibra\s*(?:diet[eé]tica)?|fibra\s*dietetica|dietary\s*fiber|fibre|fiber|fibra\s*alimentaria|fibra)$SEP$NUM$OPT_SPACE(?:g(?:r)?|gramos?)""",
            RegexOption.IGNORE_CASE
        )

        private val SODIO_MG_PATTERN = Regex(
            """(?:sodio|sodium|na)$SEP$NUM$OPT_SPACE(?:mg|miligramos?)""",
            RegexOption.IGNORE_CASE
        )

        private val SODIO_G_PATTERN = Regex(
            """(?:sodio|sodium|na)$SEP$NUM$OPT_SPACE(?:g(?:r)?|gramos?)(?![\w])""",
            RegexOption.IGNORE_CASE
        )
    }

    fun parse(textResult: Text): NutricionOcrResult {
        return parseFromText(textResult.text)
    }

    fun parseFromText(text: String): NutricionOcrResult {
        val normalizedText = normalizeText(text)
        var matchCount = 0
        val totalFields = 7

        val porcion = extractFirst(listOf(PORCION_PATTERN), normalizedText)?.also { matchCount++ }

        val calorias = (extractFirst(listOf(CALORIAS_PATTERN), normalizedText)
            ?: extractFirst(listOf(CALORIAS_SUFFIX_PATTERN), normalizedText))
            ?.also { matchCount++ }

        val proteinas = extractFirst(
            listOf(PROTEINAS_PATTERN, PROTEINAS_PATTERN_NO_UNIT), normalizedText
        )?.also { matchCount++ }

        val carbohidratos = extractFirst(
            listOf(CARBOHIDRATOS_PATTERN), normalizedText
        )?.also { matchCount++ }

        val grasas = extractFirst(
            listOf(GRASAS_PATTERN), normalizedText
        )?.also { matchCount++ }

        val fibra = extractFirst(
            listOf(FIBRA_PATTERN), normalizedText
        )?.also { matchCount++ }

        var sodio = extractFirst(listOf(SODIO_MG_PATTERN), normalizedText)
        if (sodio == null) {
            val sodioG = extractFirst(listOf(SODIO_G_PATTERN), normalizedText)
            if (sodioG != null) {
                sodio = sodioG * 1000.0
            }
        }
        if (sodio != null) matchCount++

        return NutricionOcrResult(
            porcionG = porcion,
            calorias = calorias,
            proteinas = proteinas,
            carbohidratos = carbohidratos,
            grasas = grasas,
            fibra = fibra,
            sodioMg = sodio,
            rawText = text,
            confidence = matchCount.toFloat() / totalFields.toFloat(),
            fieldsExtracted = matchCount,
            totalFields = totalFields
        )
    }

    private fun normalizeText(text: String): String {
        return text
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .replace(Regex("""\s{2,}"""), " ")
    }

    private fun extractFirst(patterns: List<Regex>, text: String): Double? {
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues.getOrNull(1) ?: continue
            val value = raw.replace(",", ".").toDoubleOrNull()
            if (value != null) return value
        }
        return null
    }
}
