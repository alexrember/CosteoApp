package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.feature.inventario.data.dto.OffNutriments
import com.mg.costeoapp.feature.inventario.data.dto.OffProduct
import com.mg.costeoapp.feature.inventario.data.mapper.parseServingSizeToGrams
import com.mg.costeoapp.feature.inventario.data.mapper.toNutricionExterna
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutricionMapperTest {

    @Test
    fun `parseServingSizeToGrams con gramos simples`() {
        assertEquals(55.0, parseServingSizeToGrams("55g")!!, 0.01)
    }

    @Test
    fun `parseServingSizeToGrams con mililitros`() {
        assertEquals(240.0, parseServingSizeToGrams("240ml")!!, 0.01)
    }

    @Test
    fun `parseServingSizeToGrams con espacios`() {
        assertEquals(30.0, parseServingSizeToGrams("30 g")!!, 0.01)
    }

    @Test
    fun `parseServingSizeToGrams con decimal`() {
        assertEquals(27.5, parseServingSizeToGrams("27.5g")!!, 0.01)
    }

    @Test
    fun `parseServingSizeToGrams con texto adicional`() {
        assertEquals(55.0, parseServingSizeToGrams("1 cup (55g)")!!, 0.01)
    }

    @Test
    fun `parseServingSizeToGrams sin unidad reconocida retorna null`() {
        assertNull(parseServingSizeToGrams("2 slices"))
    }

    @Test
    fun `parseServingSizeToGrams con string vacio retorna null`() {
        assertNull(parseServingSizeToGrams(""))
    }

    @Test
    fun `parseServingSizeToGrams con solo texto retorna null`() {
        assertNull(parseServingSizeToGrams("una porcion"))
    }

    @Test
    fun `toNutricionExterna con nutriments completos`() {
        val product = OffProduct(
            productName = "Cereal Test",
            servingSize = "30g",
            quantity = "500g",
            nutriments = OffNutriments(
                energyKcal100g = 400.0,
                proteins100g = 10.0,
                carbohydrates100g = 70.0,
                fat100g = 8.0,
                fiber100g = 5.0,
                sodium100g = 0.5
            )
        )
        val result = product.toNutricionExterna()

        assertEquals("Cereal Test", result.nombreProducto)
        assertEquals("500g", result.cantidad)
        assertEquals(30.0, result.porcionG!!, 0.01)
        assertEquals(120.0, result.calorias!!, 0.01) // 400 * 0.3
        assertEquals(3.0, result.proteinas!!, 0.01) // 10 * 0.3
        assertEquals(21.0, result.carbohidratos!!, 0.01) // 70 * 0.3
        assertEquals(2.4, result.grasas!!, 0.01) // 8 * 0.3
        assertEquals(1.5, result.fibra!!, 0.01) // 5 * 0.3
        assertEquals(150.0, result.sodioMg!!, 0.01) // 0.5 * 0.3 * 1000
        assertEquals("open_food_facts", result.fuente)
    }

    @Test
    fun `toNutricionExterna sin servingSize usa 100g por defecto`() {
        val product = OffProduct(
            productName = "Test",
            nutriments = OffNutriments(
                energyKcal100g = 200.0,
                proteins100g = 5.0
            )
        )
        val result = product.toNutricionExterna()

        assertEquals(100.0, result.porcionG!!, 0.01)
        assertEquals(200.0, result.calorias!!, 0.01) // factor = 1.0
        assertEquals(5.0, result.proteinas!!, 0.01)
    }

    @Test
    fun `toNutricionExterna sin nutriments retorna nulls`() {
        val product = OffProduct(
            productName = "Test",
            servingSize = "50g"
        )
        val result = product.toNutricionExterna()

        assertEquals(50.0, result.porcionG!!, 0.01)
        assertNull(result.calorias)
        assertNull(result.proteinas)
        assertNull(result.carbohidratos)
        assertNull(result.grasas)
        assertNull(result.fibra)
        assertNull(result.sodioMg)
    }

    @Test
    fun `toNutricionExterna con servingSize no parseable usa 100g`() {
        val product = OffProduct(
            productName = "Test",
            servingSize = "2 slices",
            nutriments = OffNutriments(
                energyKcal100g = 300.0
            )
        )
        val result = product.toNutricionExterna()

        assertEquals(100.0, result.porcionG!!, 0.01)
        assertEquals(300.0, result.calorias!!, 0.01)
    }

    @Test
    fun `toNutricionExterna con todo null`() {
        val product = OffProduct()
        val result = product.toNutricionExterna()

        assertNull(result.nombreProducto)
        assertNull(result.cantidad)
        assertEquals(100.0, result.porcionG!!, 0.01)
        assertNull(result.calorias)
        assertEquals("open_food_facts", result.fuente)
    }
}
