package com.mg.costeoapp.feature.sync.data

import android.util.Log
import com.mg.costeoapp.core.database.dao.InventarioDao
import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.SyncMetadataDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.SyncMetadata
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val syncMetadataDao: SyncMetadataDao,
    private val tiendaDao: TiendaDao,
    private val productoDao: ProductoDao,
    private val productoTiendaDao: ProductoTiendaDao,
    private val inventarioDao: InventarioDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val prefabricadoIngredienteDao: PrefabricadoIngredienteDao,
    private val platoDao: PlatoDao,
    private val platoComponenteDao: PlatoComponenteDao
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val syncTables = listOf(
        "tiendas",
        "productos",
        "producto_tienda",
        "inventario",
        "prefabricados",
        "prefabricado_ingrediente",
        "platos",
        "plato_componente"
    )

    suspend fun syncAll(userId: String): SyncResult {
        val pushResult = pushAll(userId)
        val pullResult = pullAll(userId)
        return pushResult + pullResult
    }

    suspend fun pushAll(userId: String): SyncResult {
        var totalPushed = 0
        val errors = mutableListOf<String>()

        for (table in syncTables) {
            try {
                val pushed = pushTable(table, userId)
                totalPushed += pushed
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing $table", e)
                errors.add("Push $table: ${e.message}")
            }
        }

        return SyncResult(
            success = errors.isEmpty(),
            pushedCount = totalPushed,
            errors = errors
        )
    }

    suspend fun pullAll(userId: String): SyncResult {
        var totalPulled = 0
        val errors = mutableListOf<String>()

        for (table in syncTables) {
            try {
                val pulled = pullTable(table, userId)
                totalPulled += pulled
            } catch (e: Exception) {
                Log.e(TAG, "Error pulling $table", e)
                errors.add("Pull $table: ${e.message}")
            }
        }

        return SyncResult(
            success = errors.isEmpty(),
            pulledCount = totalPulled,
            errors = errors
        )
    }

    private suspend fun pushTable(table: String, userId: String): Int {
        val metadata = syncMetadataDao.get(table) ?: SyncMetadata(tableName = table)
        val lastPushAt = metadata.lastPushAt

        val localRows = getModifiedRows(table, lastPushAt)
        if (localRows.isEmpty()) return 0

        val jsonRows = localRows.map { row -> mapToSupabaseJson(table, row, userId) }

        supabase.from(table).upsert(jsonRows) {
            onConflict = "user_id,local_id"
        }

        val now = System.currentTimeMillis()
        syncMetadataDao.upsert(metadata.copy(lastPushAt = now))

        Log.d(TAG, "Pushed ${jsonRows.size} rows to $table")
        return jsonRows.size
    }

    private suspend fun pullTable(table: String, userId: String): Int {
        val metadata = syncMetadataDao.get(table) ?: SyncMetadata(tableName = table)
        val lastPullAt = metadata.lastPullAt
        val lastPullIso = if (lastPullAt > 0) epochMillisToIso(lastPullAt) else "1970-01-01T00:00:00Z"

        val remoteRows: List<JsonObject> = supabase.from(table)
            .select {
                filter {
                    eq("user_id", userId)
                    gt("updated_at", lastPullIso)
                }
            }
            .decodeList()

        if (remoteRows.isEmpty()) return 0

        for (row in remoteRows) {
            upsertLocally(table, row)
        }

        val now = System.currentTimeMillis()
        syncMetadataDao.upsert(metadata.copy(lastPullAt = now))

        Log.d(TAG, "Pulled ${remoteRows.size} rows from $table")
        return remoteRows.size
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getModifiedRows(table: String, since: Long): List<Any> {
        return when (table) {
            "tiendas" -> if (since == 0L) tiendaDao.getAllOnce() else tiendaDao.getModifiedSince(since)
            "productos" -> if (since == 0L) productoDao.getAllOnce() else productoDao.getModifiedSince(since)
            "producto_tienda" -> if (since == 0L) productoTiendaDao.getAllOnce() else productoTiendaDao.getModifiedSince(since)
            "inventario" -> if (since == 0L) inventarioDao.getAllOnce() else inventarioDao.getModifiedSince(since)
            "prefabricados" -> if (since == 0L) prefabricadoDao.getAllOnce() else prefabricadoDao.getModifiedSince(since)
            "prefabricado_ingrediente" -> prefabricadoIngredienteDao.getAllOnce()
            "platos" -> if (since == 0L) platoDao.getAllPlatos() else platoDao.getModifiedSince(since)
            "plato_componente" -> platoComponenteDao.getAllOnce()
            else -> emptyList()
        }
    }

    private fun mapToSupabaseJson(table: String, row: Any, userId: String): JsonObject {
        return when (table) {
            "tiendas" -> (row as com.mg.costeoapp.core.database.entity.Tienda).toSupabaseJson(userId)
            "productos" -> (row as com.mg.costeoapp.core.database.entity.Producto).toSupabaseJson(userId)
            "producto_tienda" -> (row as com.mg.costeoapp.core.database.entity.ProductoTienda).toSupabaseJson(userId)
            "inventario" -> (row as com.mg.costeoapp.core.database.entity.Inventario).toSupabaseJson(userId)
            "prefabricados" -> (row as com.mg.costeoapp.core.database.entity.Prefabricado).toSupabaseJson(userId)
            "prefabricado_ingrediente" -> (row as com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente).toSupabaseJson(userId)
            "platos" -> (row as com.mg.costeoapp.core.database.entity.Plato).toSupabaseJson(userId)
            "plato_componente" -> (row as com.mg.costeoapp.core.database.entity.PlatoComponente).toSupabaseJson(userId)
            else -> throw IllegalArgumentException("Tabla desconocida: $table")
        }
    }

    private suspend fun upsertLocally(table: String, json: JsonObject) {
        when (table) {
            "tiendas" -> {
                val entity = json.toTienda()
                val existing = tiendaDao.getById(entity.id)
                if (existing == null) {
                    tiendaDao.insert(entity)
                } else if (entity.updatedAt > existing.updatedAt) {
                    tiendaDao.update(entity)
                }
            }
            "productos" -> {
                val entity = json.toProducto()
                val existing = productoDao.getById(entity.id)
                if (existing == null) {
                    productoDao.insert(entity)
                } else if (entity.updatedAt > existing.updatedAt) {
                    productoDao.update(entity)
                }
            }
            "producto_tienda" -> {
                val entity = json.toProductoTienda()
                val existing = productoTiendaDao.getPrecioActivo(entity.productoId, entity.tiendaId)
                if (existing == null || existing.id != entity.id) {
                    try {
                        productoTiendaDao.insert(entity)
                    } catch (_: Exception) {
                        productoTiendaDao.update(entity)
                    }
                } else if (entity.updatedAt > existing.updatedAt) {
                    productoTiendaDao.update(entity)
                }
            }
            "inventario" -> {
                val entity = json.toInventario()
                try {
                    inventarioDao.insert(entity)
                } catch (_: Exception) {
                    inventarioDao.update(entity)
                }
            }
            "prefabricados" -> {
                val entity = json.toPrefabricado()
                val existing = prefabricadoDao.getById(entity.id)
                if (existing == null) {
                    prefabricadoDao.insert(entity)
                } else if (entity.updatedAt > existing.updatedAt) {
                    prefabricadoDao.update(entity)
                }
            }
            "prefabricado_ingrediente" -> {
                val entity = json.toPrefabricadoIngrediente()
                try {
                    prefabricadoIngredienteDao.insert(entity)
                } catch (_: Exception) {
                    prefabricadoIngredienteDao.update(entity)
                }
            }
            "platos" -> {
                val entity = json.toPlato()
                val existing = platoDao.getById(entity.id)
                if (existing == null) {
                    platoDao.insert(entity)
                } else if (entity.updatedAt > existing.updatedAt) {
                    platoDao.update(entity)
                }
            }
            "plato_componente" -> {
                val entity = json.toPlatoComponente()
                try {
                    platoComponenteDao.insert(entity)
                } catch (_: Exception) {
                    platoComponenteDao.update(entity)
                }
            }
        }
    }
}
