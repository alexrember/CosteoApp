package com.mg.costeoapp.feature.sync.data

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

/**
 * EntityMappers v2 — utility functions for JSON ↔ epoch conversion.
 *
 * The old per-entity mappers (Tienda, Producto, ProductoTienda, Inventario,
 * Prefabricado, PrefabricadoIngrediente, Plato, PlatoComponente) were removed
 * because the Supabase tables they mapped to no longer exist.
 *
 * These helpers are kept because they are used by SyncManager and will be
 * needed again when mapping between Room entities and the new user_* tables.
 */

private val isoFormatter = DateTimeFormatter.ISO_INSTANT

fun epochMillisToIso(millis: Long): String =
    Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(isoFormatter)

fun isoToEpochMillis(iso: String): Long =
    Instant.parse(iso).toEpochMilli()

internal fun JsonObject.stringOrNull(key: String): String? {
    val el = get(key) ?: return null
    if (el is JsonPrimitive && el.isString) return el.content
    if (el is kotlinx.serialization.json.JsonNull) return null
    return el.jsonPrimitive.content
}

internal fun JsonObject.longVal(key: String): Long = get(key)!!.jsonPrimitive.long
internal fun JsonObject.intVal(key: String): Int = get(key)!!.jsonPrimitive.int
internal fun JsonObject.doubleVal(key: String): Double = get(key)!!.jsonPrimitive.double
internal fun JsonObject.boolVal(key: String): Boolean = get(key)!!.jsonPrimitive.boolean

internal fun JsonObject.longOrNull(key: String): Long? {
    val el = get(key) ?: return null
    if (el is kotlinx.serialization.json.JsonNull) return null
    return el.jsonPrimitive.long
}

internal fun JsonObject.doubleOrNull(key: String): Double? {
    val el = get(key) ?: return null
    if (el is kotlinx.serialization.json.JsonNull) return null
    return el.jsonPrimitive.double
}

internal fun JsonObject.isoToMillis(key: String): Long {
    val raw = stringOrNull(key) ?: return 0L
    return isoToEpochMillis(raw)
}

internal fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject {
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
