package com.mg.costeoapp.feature.sync.data

import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappersTest {

    private val userId = "test-user-id"

    // -----------------------------------------------------------------------
    // Timestamp conversion
    // -----------------------------------------------------------------------

    @Test
    fun `epochMillisToIso convierte correctamente`() {
        val millis = 1700000000000L
        val iso = epochMillisToIso(millis)
        assertTrue("ISO string debe contener T: $iso", iso.contains("T"))
        assertTrue("ISO string debe contener Z: $iso", iso.contains("Z"))
    }

    @Test
    fun `isoToEpochMillis convierte correctamente`() {
        val iso = "2023-11-14T22:13:20Z"
        val millis = isoToEpochMillis(iso)
        assertEquals(1700000000000L, millis)
    }

    @Test
    fun `roundtrip epoch to ISO y de vuelta`() {
        val original = 1700000000000L
        val iso = epochMillisToIso(original)
        val back = isoToEpochMillis(iso)
        assertEquals(original, back)
    }

    @Test
    fun `epoch cero convierte a ISO correctamente`() {
        val iso = epochMillisToIso(0L)
        assertEquals("1970-01-01T00:00:00Z", iso)
    }

    // -----------------------------------------------------------------------
    // Tienda
    // -----------------------------------------------------------------------

    @Test
    fun `Tienda toSupabaseJson mapea todos los campos`() {
        val tienda = Tienda(
            id = 42L,
            nombre = "Super Selectos",
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000001000L,
            version = 3
        )
        val json = tienda.toSupabaseJson(userId)
        assertEquals(userId, json["user_id"]!!.jsonPrimitive.content)
        assertEquals(42L, json["local_id"]!!.jsonPrimitive.content.toLong())
        assertEquals("Super Selectos", json["nombre"]!!.jsonPrimitive.content)
        assertEquals(true, json["activo"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(3, json["version"]!!.jsonPrimitive.content.toInt())
        assertNotNull(json["created_at"])
        assertNotNull(json["updated_at"])
    }

    @Test
    fun `Tienda roundtrip toJson y toTienda`() {
        val original = Tienda(
            id = 10L,
            nombre = "PriceSmart",
            activo = false,
            createdAt = 1700000000000L,
            updatedAt = 1700000002000L,
            version = 2
        )
        val json = original.toSupabaseJson(userId)
        val restored = json.toTienda()
        assertEquals(original.id, restored.id)
        assertEquals(original.nombre, restored.nombre)
        assertEquals(original.activo, restored.activo)
        assertEquals(original.version, restored.version)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt, restored.updatedAt)
    }

    @Test
    fun `Tienda con nombre vacio`() {
        val tienda = Tienda(id = 1L, nombre = "", activo = true, createdAt = 0L, updatedAt = 0L, version = 1)
        val json = tienda.toSupabaseJson(userId)
        assertEquals("", json["nombre"]!!.jsonPrimitive.content)
        val restored = json.toTienda()
        assertEquals("", restored.nombre)
    }

    // -----------------------------------------------------------------------
    // Producto
    // -----------------------------------------------------------------------

    @Test
    fun `Producto toSupabaseJson mapea todos los campos incluyendo nutricion`() {
        val producto = Producto(
            id = 99L,
            nombre = "Arroz",
            codigoBarras = "1234567890",
            unidadMedida = "lb",
            cantidadPorEmpaque = 5.0,
            unidadesPorEmpaque = 1,
            esServicio = false,
            notas = "Arroz de primera",
            factorMerma = 10,
            nutricionPorcionG = 56.0,
            nutricionCalorias = 206.0,
            nutricionProteinasG = 4.3,
            nutricionCarbohidratosG = 44.5,
            nutricionGrasasG = 0.4,
            nutricionFibraG = 0.6,
            nutricionSodioMg = 1.0,
            nutricionFuente = "USDA",
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            version = 1
        )
        val json = producto.toSupabaseJson(userId)
        assertEquals("Arroz", json["nombre"]!!.jsonPrimitive.content)
        assertEquals("1234567890", json["codigo_barras"]!!.jsonPrimitive.content)
        assertEquals("lb", json["unidad_medida"]!!.jsonPrimitive.content)
        assertEquals(5.0, json["cantidad_por_empaque"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals(1, json["unidades_por_empaque"]!!.jsonPrimitive.content.toInt())
        assertEquals(false, json["es_servicio"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Arroz de primera", json["notas"]!!.jsonPrimitive.content)
        assertEquals(10, json["factor_merma"]!!.jsonPrimitive.content.toInt())
        assertEquals(56.0, json["nutricion_porcion_g"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals(206.0, json["nutricion_calorias"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals("USDA", json["nutricion_fuente"]!!.jsonPrimitive.content)
    }

    @Test
    fun `Producto con campos nulos mapea a JsonNull`() {
        val producto = Producto(
            id = 1L,
            nombre = "Agua",
            unidadMedida = "unidad",
            cantidadPorEmpaque = 1.0,
            codigoBarras = null,
            notas = null,
            nutricionPorcionG = null,
            nutricionCalorias = null,
            nutricionProteinasG = null,
            nutricionCarbohidratosG = null,
            nutricionGrasasG = null,
            nutricionFibraG = null,
            nutricionSodioMg = null,
            nutricionFuente = null,
            activo = true,
            createdAt = 0L,
            updatedAt = 0L,
            version = 1
        )
        val json = producto.toSupabaseJson(userId)
        assertTrue(json["codigo_barras"] is JsonNull)
        assertTrue(json["notas"] is JsonNull)
        assertTrue(json["nutricion_porcion_g"] is JsonNull)
        assertTrue(json["nutricion_calorias"] is JsonNull)
        assertTrue(json["nutricion_fuente"] is JsonNull)
    }

    @Test
    fun `Producto roundtrip preserva todos los campos`() {
        val original = Producto(
            id = 5L,
            nombre = "Frijoles",
            codigoBarras = "999",
            unidadMedida = "lb",
            cantidadPorEmpaque = 2.0,
            unidadesPorEmpaque = 1,
            esServicio = false,
            notas = "Nota",
            factorMerma = 5,
            nutricionPorcionG = 100.0,
            nutricionCalorias = 347.0,
            nutricionProteinasG = 21.0,
            nutricionCarbohidratosG = 63.0,
            nutricionGrasasG = 1.2,
            nutricionFibraG = 15.0,
            nutricionSodioMg = 5.0,
            nutricionFuente = "manual",
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            version = 2
        )
        val json = original.toSupabaseJson(userId)
        val restored = json.toProducto()
        assertEquals(original.id, restored.id)
        assertEquals(original.nombre, restored.nombre)
        assertEquals(original.codigoBarras, restored.codigoBarras)
        assertEquals(original.unidadMedida, restored.unidadMedida)
        assertEquals(original.cantidadPorEmpaque, restored.cantidadPorEmpaque, 0.001)
        assertEquals(original.unidadesPorEmpaque, restored.unidadesPorEmpaque)
        assertEquals(original.esServicio, restored.esServicio)
        assertEquals(original.notas, restored.notas)
        assertEquals(original.factorMerma, restored.factorMerma)
        assertEquals(original.nutricionPorcionG, restored.nutricionPorcionG)
        assertEquals(original.nutricionCalorias, restored.nutricionCalorias)
        assertEquals(original.nutricionFuente, restored.nutricionFuente)
        assertEquals(original.version, restored.version)
    }

    @Test
    fun `Producto con valores cero`() {
        val producto = Producto(
            id = 0L,
            nombre = "Test",
            unidadMedida = "g",
            cantidadPorEmpaque = 0.0,
            unidadesPorEmpaque = 0,
            factorMerma = 0,
            activo = true,
            createdAt = 0L,
            updatedAt = 0L,
            version = 0
        )
        val json = producto.toSupabaseJson(userId)
        val restored = json.toProducto()
        assertEquals(0L, restored.id)
        assertEquals(0.0, restored.cantidadPorEmpaque, 0.001)
        assertEquals(0, restored.unidadesPorEmpaque)
        assertEquals(0, restored.factorMerma)
    }

    // -----------------------------------------------------------------------
    // Prefabricado
    // -----------------------------------------------------------------------

    @Test
    fun `Prefabricado roundtrip con campos opcionales nulos`() {
        val original = Prefabricado(
            id = 7L,
            nombre = "Salsa roja",
            descripcion = null,
            duplicadoDe = null,
            costoFijo = 0L,
            rendimientoPorciones = 10.0,
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            version = 1
        )
        val json = original.toSupabaseJson(userId)
        assertTrue(json["descripcion"] is JsonNull)
        assertTrue(json["duplicado_de"] is JsonNull)
        val restored = json.toPrefabricado()
        assertEquals(original.id, restored.id)
        assertEquals(original.nombre, restored.nombre)
        assertEquals(original.descripcion, restored.descripcion)
        assertEquals(original.duplicadoDe, restored.duplicadoDe)
        assertEquals(original.rendimientoPorciones, restored.rendimientoPorciones, 0.001)
    }

    // -----------------------------------------------------------------------
    // PrefabricadoIngrediente (sin timestamps created_at/updated_at en mapper)
    // -----------------------------------------------------------------------

    @Test
    fun `PrefabricadoIngrediente roundtrip`() {
        val original = PrefabricadoIngrediente(
            id = 3L,
            prefabricadoId = 7L,
            productoId = 99L,
            cantidadUsada = 2.5,
            unidadUsada = "lb",
            version = 1
        )
        val json = original.toSupabaseJson(userId)
        val restored = json.toPrefabricadoIngrediente()
        assertEquals(original.id, restored.id)
        assertEquals(original.prefabricadoId, restored.prefabricadoId)
        assertEquals(original.productoId, restored.productoId)
        assertEquals(original.cantidadUsada, restored.cantidadUsada, 0.001)
        assertEquals(original.unidadUsada, restored.unidadUsada)
        assertEquals(original.version, restored.version)
    }

    // -----------------------------------------------------------------------
    // Plato
    // -----------------------------------------------------------------------

    @Test
    fun `Plato roundtrip con campos opcionales`() {
        val original = Plato(
            id = 1L,
            nombre = "Pupusa de queso",
            descripcion = "Clasica",
            margenPorcentaje = 35.0,
            precioVentaManual = 75L,
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            version = 1
        )
        val json = original.toSupabaseJson(userId)
        val restored = json.toPlato()
        assertEquals(original.id, restored.id)
        assertEquals(original.nombre, restored.nombre)
        assertEquals(original.descripcion, restored.descripcion)
        assertEquals(original.margenPorcentaje, restored.margenPorcentaje)
        assertEquals(original.precioVentaManual, restored.precioVentaManual)
    }

    @Test
    fun `Plato con opcionales nulos`() {
        val original = Plato(
            id = 2L,
            nombre = "Plato test",
            descripcion = null,
            margenPorcentaje = null,
            precioVentaManual = null,
            activo = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            version = 1
        )
        val json = original.toSupabaseJson(userId)
        assertTrue(json["descripcion"] is JsonNull)
        assertTrue(json["margen_porcentaje"] is JsonNull)
        assertTrue(json["precio_venta_manual"] is JsonNull)
        val restored = json.toPlato()
        assertEquals(null, restored.descripcion)
        assertEquals(null, restored.margenPorcentaje)
        assertEquals(null, restored.precioVentaManual)
    }

    // -----------------------------------------------------------------------
    // PlatoComponente
    // -----------------------------------------------------------------------

    @Test
    fun `PlatoComponente roundtrip con prefabricado`() {
        val original = PlatoComponente(
            id = 10L,
            platoId = 1L,
            prefabricadoId = 7L,
            productoId = null,
            cantidad = 2.0,
            notas = "doble porcion",
            version = 1
        )
        val json = original.toSupabaseJson(userId)
        val restored = json.toPlatoComponente()
        assertEquals(original.id, restored.id)
        assertEquals(original.platoId, restored.platoId)
        assertEquals(original.prefabricadoId, restored.prefabricadoId)
        assertEquals(original.productoId, restored.productoId)
        assertEquals(original.cantidad, restored.cantidad, 0.001)
        assertEquals(original.notas, restored.notas)
    }

    @Test
    fun `PlatoComponente con producto directo`() {
        val original = PlatoComponente(
            id = 11L,
            platoId = 1L,
            prefabricadoId = null,
            productoId = 99L,
            cantidad = 0.5,
            notas = null,
            version = 1
        )
        val json = original.toSupabaseJson(userId)
        assertTrue(json["prefabricado_id"] is JsonNull)
        assertTrue(json["notas"] is JsonNull)
        val restored = json.toPlatoComponente()
        assertEquals(null, restored.prefabricadoId)
        assertEquals(99L, restored.productoId)
        assertEquals(null, restored.notas)
    }
}
