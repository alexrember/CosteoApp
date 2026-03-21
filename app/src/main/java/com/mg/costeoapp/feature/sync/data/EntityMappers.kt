package com.mg.costeoapp.feature.sync.data

import com.mg.costeoapp.core.database.entity.Inventario
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.database.entity.Tienda
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val isoFormatter = DateTimeFormatter.ISO_INSTANT

fun epochMillisToIso(millis: Long): String =
    Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(isoFormatter)

fun isoToEpochMillis(iso: String): Long =
    Instant.parse(iso).toEpochMilli()

private fun JsonObject.stringOrNull(key: String): String? {
    val el = get(key) ?: return null
    if (el is JsonPrimitive && el.isString) return el.content
    if (el is kotlinx.serialization.json.JsonNull) return null
    return el.jsonPrimitive.content
}

private fun JsonObject.longVal(key: String): Long = get(key)!!.jsonPrimitive.long
private fun JsonObject.intVal(key: String): Int = get(key)!!.jsonPrimitive.int
private fun JsonObject.doubleVal(key: String): Double = get(key)!!.jsonPrimitive.double
private fun JsonObject.boolVal(key: String): Boolean = get(key)!!.jsonPrimitive.boolean

private fun JsonObject.longOrNull(key: String): Long? {
    val el = get(key) ?: return null
    if (el is kotlinx.serialization.json.JsonNull) return null
    return el.jsonPrimitive.long
}

private fun JsonObject.doubleOrNull(key: String): Double? {
    val el = get(key) ?: return null
    if (el is kotlinx.serialization.json.JsonNull) return null
    return el.jsonPrimitive.double
}

private fun JsonObject.isoToMillis(key: String): Long {
    val raw = stringOrNull(key) ?: return 0L
    return isoToEpochMillis(raw)
}

private fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject {
    val map = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
    for ((k, v) in pairs) {
        map[k] = when (v) {
            null -> kotlinx.serialization.json.JsonNull
            is String -> JsonPrimitive(v)
            is Long -> JsonPrimitive(v)
            is Int -> JsonPrimitive(v)
            is Double -> JsonPrimitive(v)
            is Boolean -> JsonPrimitive(v)
            else -> JsonPrimitive(v.toString())
        }
    }
    return JsonObject(map)
}

// ---------------------------------------------------------------------------
// Tienda
// ---------------------------------------------------------------------------
fun Tienda.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "nombre" to nombre,
    "activo" to activo,
    "version" to version,
    "created_at" to epochMillisToIso(createdAt),
    "updated_at" to epochMillisToIso(updatedAt)
)

fun JsonObject.toTienda(): Tienda = Tienda(
    id = longVal("local_id"),
    nombre = stringOrNull("nombre")!!,
    activo = boolVal("activo"),
    createdAt = isoToMillis("created_at"),
    updatedAt = isoToMillis("updated_at"),
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// Producto
// ---------------------------------------------------------------------------
fun Producto.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "nombre" to nombre,
    "codigo_barras" to codigoBarras,
    "unidad_medida" to unidadMedida,
    "cantidad_por_empaque" to cantidadPorEmpaque,
    "unidades_por_empaque" to unidadesPorEmpaque,
    "es_servicio" to esServicio,
    "notas" to notas,
    "factor_merma" to factorMerma,
    "nutricion_porcion_g" to nutricionPorcionG,
    "nutricion_calorias" to nutricionCalorias,
    "nutricion_proteinas_g" to nutricionProteinasG,
    "nutricion_carbohidratos_g" to nutricionCarbohidratosG,
    "nutricion_grasas_g" to nutricionGrasasG,
    "nutricion_fibra_g" to nutricionFibraG,
    "nutricion_sodio_mg" to nutricionSodioMg,
    "nutricion_fuente" to nutricionFuente,
    "activo" to activo,
    "version" to version,
    "created_at" to epochMillisToIso(createdAt),
    "updated_at" to epochMillisToIso(updatedAt)
)

fun JsonObject.toProducto(): Producto = Producto(
    id = longVal("local_id"),
    nombre = stringOrNull("nombre")!!,
    codigoBarras = stringOrNull("codigo_barras"),
    unidadMedida = stringOrNull("unidad_medida")!!,
    cantidadPorEmpaque = doubleVal("cantidad_por_empaque"),
    unidadesPorEmpaque = intVal("unidades_por_empaque"),
    esServicio = boolVal("es_servicio"),
    notas = stringOrNull("notas"),
    factorMerma = intVal("factor_merma"),
    nutricionPorcionG = doubleOrNull("nutricion_porcion_g"),
    nutricionCalorias = doubleOrNull("nutricion_calorias"),
    nutricionProteinasG = doubleOrNull("nutricion_proteinas_g"),
    nutricionCarbohidratosG = doubleOrNull("nutricion_carbohidratos_g"),
    nutricionGrasasG = doubleOrNull("nutricion_grasas_g"),
    nutricionFibraG = doubleOrNull("nutricion_fibra_g"),
    nutricionSodioMg = doubleOrNull("nutricion_sodio_mg"),
    nutricionFuente = stringOrNull("nutricion_fuente"),
    activo = boolVal("activo"),
    createdAt = isoToMillis("created_at"),
    updatedAt = isoToMillis("updated_at"),
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// ProductoTienda
// ---------------------------------------------------------------------------
fun ProductoTienda.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "producto_id" to productoId,
    "tienda_id" to tiendaId,
    "precio" to precio,
    "fecha_registro" to epochMillisToIso(fechaRegistro),
    "activo" to activo,
    "version" to version,
    "created_at" to epochMillisToIso(createdAt),
    "updated_at" to epochMillisToIso(updatedAt)
)

fun JsonObject.toProductoTienda(): ProductoTienda = ProductoTienda(
    id = longVal("local_id"),
    productoId = longVal("producto_id"),
    tiendaId = longVal("tienda_id"),
    precio = longVal("precio"),
    fechaRegistro = isoToMillis("fecha_registro"),
    activo = boolVal("activo"),
    createdAt = isoToMillis("created_at"),
    updatedAt = isoToMillis("updated_at"),
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// Inventario
// ---------------------------------------------------------------------------
fun Inventario.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "producto_id" to productoId,
    "tienda_id" to tiendaId,
    "cantidad" to cantidad,
    "precio_compra" to precioCompra,
    "fecha_compra" to epochMillisToIso(fechaCompra),
    "agotado" to agotado,
    "activo" to activo,
    "version" to version,
    "created_at" to epochMillisToIso(createdAt),
    "updated_at" to epochMillisToIso(updatedAt)
)

fun JsonObject.toInventario(): Inventario = Inventario(
    id = longVal("local_id"),
    productoId = longVal("producto_id"),
    tiendaId = longVal("tienda_id"),
    cantidad = doubleVal("cantidad"),
    precioCompra = longVal("precio_compra"),
    fechaCompra = isoToMillis("fecha_compra"),
    agotado = boolVal("agotado"),
    activo = boolVal("activo"),
    createdAt = isoToMillis("created_at"),
    updatedAt = isoToMillis("updated_at"),
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// Prefabricado
// ---------------------------------------------------------------------------
fun Prefabricado.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "nombre" to nombre,
    "descripcion" to descripcion,
    "duplicado_de" to duplicadoDe,
    "costo_fijo" to costoFijo,
    "rendimiento_porciones" to rendimientoPorciones,
    "activo" to activo,
    "version" to version,
    "created_at" to epochMillisToIso(createdAt),
    "updated_at" to epochMillisToIso(updatedAt)
)

fun JsonObject.toPrefabricado(): Prefabricado = Prefabricado(
    id = longVal("local_id"),
    nombre = stringOrNull("nombre")!!,
    descripcion = stringOrNull("descripcion"),
    duplicadoDe = longOrNull("duplicado_de"),
    costoFijo = longVal("costo_fijo"),
    rendimientoPorciones = doubleVal("rendimiento_porciones"),
    activo = boolVal("activo"),
    createdAt = isoToMillis("created_at"),
    updatedAt = isoToMillis("updated_at"),
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// PrefabricadoIngrediente (no tiene created_at/updated_at en Room)
// ---------------------------------------------------------------------------
fun PrefabricadoIngrediente.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "prefabricado_id" to prefabricadoId,
    "producto_id" to productoId,
    "cantidad_usada" to cantidadUsada,
    "unidad_usada" to unidadUsada,
    "version" to version
)

fun JsonObject.toPrefabricadoIngrediente(): PrefabricadoIngrediente = PrefabricadoIngrediente(
    id = longVal("local_id"),
    prefabricadoId = longVal("prefabricado_id"),
    productoId = longVal("producto_id"),
    cantidadUsada = doubleVal("cantidad_usada"),
    unidadUsada = stringOrNull("unidad_usada")!!,
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// Plato
// ---------------------------------------------------------------------------
fun Plato.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "nombre" to nombre,
    "descripcion" to descripcion,
    "margen_porcentaje" to margenPorcentaje,
    "precio_venta_manual" to precioVentaManual,
    "activo" to activo,
    "version" to version,
    "created_at" to epochMillisToIso(createdAt),
    "updated_at" to epochMillisToIso(updatedAt)
)

fun JsonObject.toPlato(): Plato = Plato(
    id = longVal("local_id"),
    nombre = stringOrNull("nombre")!!,
    descripcion = stringOrNull("descripcion"),
    margenPorcentaje = doubleOrNull("margen_porcentaje"),
    precioVentaManual = longOrNull("precio_venta_manual"),
    activo = boolVal("activo"),
    createdAt = isoToMillis("created_at"),
    updatedAt = isoToMillis("updated_at"),
    version = intVal("version")
)

// ---------------------------------------------------------------------------
// PlatoComponente (no tiene created_at/updated_at en Room)
// ---------------------------------------------------------------------------
fun PlatoComponente.toSupabaseJson(userId: String): JsonObject = jsonOf(
    "user_id" to userId,
    "local_id" to id,
    "plato_id" to platoId,
    "prefabricado_id" to prefabricadoId,
    "producto_id" to productoId,
    "cantidad" to cantidad,
    "notas" to notas,
    "version" to version
)

fun JsonObject.toPlatoComponente(): PlatoComponente = PlatoComponente(
    id = longVal("local_id"),
    platoId = longVal("plato_id"),
    prefabricadoId = longOrNull("prefabricado_id"),
    productoId = longOrNull("producto_id"),
    cantidad = doubleVal("cantidad"),
    notas = stringOrNull("notas"),
    version = intVal("version")
)
