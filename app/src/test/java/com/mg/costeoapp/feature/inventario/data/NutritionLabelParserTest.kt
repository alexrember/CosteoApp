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
        val text = "Calorías 150 Proteína 5g"
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
}
