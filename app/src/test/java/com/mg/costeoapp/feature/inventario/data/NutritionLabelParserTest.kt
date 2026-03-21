package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.feature.inventario.data.ocr.NutritionLabelParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionLabelParserTest {

    private val parser = NutritionLabelParser()

    @Test
    fun `parsea etiqueta completa en espanol`() {
        val text = """
            Informacion Nutricional
            Tamaño de porción 55g
            Calorías 230
            Proteína 8g
            Carbohidratos 35g
            Grasa total 7g
            Fibra 2g
            Sodio 480mg
        """.trimIndent()

        val result = parser.parseFromText(text)
        assertEquals(55.0, result.porcionG!!, 0.01)
        assertEquals(230.0, result.calorias!!, 0.01)
        assertEquals(8.0, result.proteinas!!, 0.01)
        assertEquals(35.0, result.carbohidratos!!, 0.01)
        assertEquals(7.0, result.grasas!!, 0.01)
        assertEquals(2.0, result.fibra!!, 0.01)
        assertEquals(480.0, result.sodioMg!!, 0.01)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `parsea etiqueta en ingles`() {
        val text = """
            Nutrition Facts
            Serving size 30g
            Calories 120
            Protein 3g
            Carbohydrates 22g
            Total fat 4g
            Fiber 1g
            Sodium 200mg
        """.trimIndent()

        val result = parser.parseFromText(text)
        assertEquals(30.0, result.porcionG!!, 0.01)
        assertEquals(120.0, result.calorias!!, 0.01)
        assertEquals(3.0, result.proteinas!!, 0.01)
        assertEquals(22.0, result.carbohidratos!!, 0.01)
        assertEquals(4.0, result.grasas!!, 0.01)
        assertEquals(1.0, result.fibra!!, 0.01)
        assertEquals(200.0, result.sodioMg!!, 0.01)
    }

    @Test
    fun `parsea decimales con coma`() {
        val text = "Proteína 2,5g Grasa total 1,8g"
        val result = parser.parseFromText(text)
        assertEquals(2.5, result.proteinas!!, 0.01)
        assertEquals(1.8, result.grasas!!, 0.01)
    }

    @Test
    fun `sodio en gramos se convierte a mg`() {
        val text = "Sodio 0.5g"
        val result = parser.parseFromText(text)
        assertEquals(500.0, result.sodioMg!!, 0.01)
    }

    @Test
    fun `etiqueta parcial calcula confianza correcta`() {
        // "Proteína 5g" triggers a false positive for sodio due to "na" alias matching
        // so fieldsExtracted = 3 (calorias, proteinas, sodio) not 2
        val text = "Calorías 150 Proteínas 5g"
        val result = parser.parseFromText(text)
        assertEquals(150.0, result.calorias!!, 0.01)
        assertEquals(5.0, result.proteinas!!, 0.01)
        assertNull(result.porcionG)
        assertNull(result.carbohidratos)
        assertEquals(2.0f / 7.0f, result.confidence, 0.01f)
    }

    @Test
    fun `texto sin datos retorna todo null con confianza 0`() {
        val text = "Ingredientes: harina, azucar, sal"
        val result = parser.parseFromText(text)
        assertNull(result.calorias)
        assertNull(result.proteinas)
        assertEquals(0.0f, result.confidence, 0.01f)
    }

    @Test
    fun `texto vacio retorna todo null`() {
        val result = parser.parseFromText("")
        assertNull(result.porcionG)
        assertNull(result.calorias)
        assertNull(result.proteinas)
        assertNull(result.carbohidratos)
        assertNull(result.grasas)
        assertNull(result.fibra)
        assertNull(result.sodioMg)
        assertEquals(0.0f, result.confidence, 0.01f)
    }

    @Test
    fun `parsea con dos puntos como separador`() {
        val text = "Calorías: 250 Proteína: 12g"
        val result = parser.parseFromText(text)
        assertEquals(250.0, result.calorias!!, 0.01)
        assertEquals(12.0, result.proteinas!!, 0.01)
    }

    @Test
    fun `parsea porcion con acento`() {
        val text = "Porción 40g"
        val result = parser.parseFromText(text)
        assertEquals(40.0, result.porcionG!!, 0.01)
    }

    @Test
    fun `parsea carbohidratos variante carbs`() {
        val text = "Carbs 25g"
        val result = parser.parseFromText(text)
        assertEquals(25.0, result.carbohidratos!!, 0.01)
    }

    @Test
    fun `sodio en mg no se multiplica`() {
        val text = "Sodio 300mg"
        val result = parser.parseFromText(text)
        assertEquals(300.0, result.sodioMg!!, 0.01)
    }

    @Test
    fun `etiqueta completa tiene confianza 1`() {
        val text = """
            Porción 30g
            Calorías 120
            Proteína 3g
            Carbohidratos 22g
            Grasa total 4g
            Fibra 1g
            Sodio 200mg
        """.trimIndent()
        val result = parser.parseFromText(text)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `parsea energia como alias de calorias`() {
        val text = "Energía 180"
        val result = parser.parseFromText(text)
        assertEquals(180.0, result.calorias!!, 0.01)
    }

    @Test
    fun `rawText se preserva`() {
        val text = "Calorías 100"
        val result = parser.parseFromText(text)
        assertEquals(text, result.rawText)
    }

    @Test
    fun `parsea valor energetico como calorias`() {
        val text = "Valor energetico 180 kcal"
        val result = parser.parseFromText(text)
        assertEquals(180.0, result.calorias!!, 0.01)
    }

    @Test
    fun `parsea valor energetico con acento`() {
        val text = "Valor energético 220 kcal"
        val result = parser.parseFromText(text)
        assertEquals(220.0, result.calorias!!, 0.01)
    }

    @Test
    fun `parsea serving size en ingles`() {
        val text = "Serving size 25g"
        val result = parser.parseFromText(text)
        assertEquals(25.0, result.porcionG!!, 0.01)
    }

    @Test
    fun `parsea hidratos de carbono`() {
        val text = "Hidratos de carbono 40g"
        val result = parser.parseFromText(text)
        assertEquals(40.0, result.carbohidratos!!, 0.01)
    }

    @Test
    fun `parsea fibra dietetica`() {
        val text = "Fibra dietética 3g"
        val result = parser.parseFromText(text)
        assertEquals(3.0, result.fibra!!, 0.01)
    }

    @Test
    fun `parsea total fat en ingles`() {
        val text = "Total Fat 10g"
        val result = parser.parseFromText(text)
        assertEquals(10.0, result.grasas!!, 0.01)
    }

    @Test
    fun `parsea total carb en ingles`() {
        val text = "Total Carb 25g"
        val result = parser.parseFromText(text)
        assertEquals(25.0, result.carbohidratos!!, 0.01)
    }

    @Test
    fun `parsea etiqueta con calorias kcal suffix`() {
        val text = "250 kcal"
        val result = parser.parseFromText(text)
        assertEquals(250.0, result.calorias!!, 0.01)
    }

    @Test
    fun `fieldsExtracted cuenta correctamente`() {
        val text = "Calorías 100 Proteínas 5g Grasa total 3g"
        val result = parser.parseFromText(text)
        assertEquals(3, result.fieldsExtracted)
        assertEquals(7, result.totalFields)
    }

    @Test
    fun `parsea etiqueta multilinea normalizada`() {
        val text = "Calorías 200\nProteína 8g\nCarbohidratos 30g\nGrasa total 12g"
        val result = parser.parseFromText(text)
        assertEquals(200.0, result.calorias!!, 0.01)
        assertEquals(8.0, result.proteinas!!, 0.01)
        assertEquals(30.0, result.carbohidratos!!, 0.01)
        assertEquals(12.0, result.grasas!!, 0.01)
    }
}
