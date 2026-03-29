package com.mg.costeoapp.feature.sync.data

import android.util.Log
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.SyncMetadataDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class GlobalProductRow(
    val id: String,
    val ean: String
)

@Serializable
private data class GlobalStoreRow(
    val id: String,
    @SerialName("name") val name: String
)

@Serializable
private data class UserProductAliasDto(
    @SerialName("user_id") val userId: String,
    @SerialName("global_product_id") val globalProductId: String,
    val alias: String? = null,
    @SerialName("factor_merma") val factorMerma: Int = 0,
    val notes: String? = null
)

@Serializable
private data class UserStoreAliasDto(
    @SerialName("user_id") val userId: String,
    @SerialName("global_store_id") val globalStoreId: String,
    val alias: String
)

/**
 * SyncManager v3 — links local Room entities to Supabase global catalog.
 *
 * Linking flow:
 *   - Products with a barcode are matched to global_products by EAN
 *   - Stores are matched to global_stores by name
 *   - User aliases (product alias, factor_merma, store alias) are pushed
 *
 * Full inventory/recipe/dish sync is deferred to a later phase.
 */
@Singleton
class SyncManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val syncMetadataDao: SyncMetadataDao,
    private val productoDao: ProductoDao,
    private val tiendaDao: TiendaDao
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    suspend fun syncAll(userId: String): SyncResult {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            return SyncResult(
                success = false,
                errors = listOf("No hay sesion activa. Inicia sesion para sincronizar.")
            )
        }

        var result = SyncResult(success = true)
        result = result + linkProducts()
        result = result + linkStores()
        result = result + pushProductAliases(userId)
        result = result + pushStoreAliases(userId)
        return result
    }

    suspend fun pushAll(userId: String): SyncResult {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            return SyncResult(
                success = false,
                errors = listOf("No hay sesion activa. Inicia sesion para sincronizar.")
            )
        }

        var result = SyncResult(success = true)
        result = result + linkProducts()
        result = result + linkStores()
        result = result + pushProductAliases(userId)
        result = result + pushStoreAliases(userId)
        return result
    }

    suspend fun pullAll(userId: String): SyncResult {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            return SyncResult(
                success = false,
                errors = listOf("No hay sesion activa. Inicia sesion para sincronizar.")
            )
        }

        var result = SyncResult(success = true)
        result = result + linkProducts()
        result = result + linkStores()
        return result
    }

    /**
     * For each local product with a barcode but no globalProductId,
     * query global_products by EAN. If found, save the UUID locally.
     */
    private suspend fun linkProducts(): SyncResult {
        return try {
            val allProducts = productoDao.getAllOnce()
            val unlinked = allProducts.filter { it.codigoBarras != null && it.globalProductId == null }

            if (unlinked.isEmpty()) {
                return SyncResult(success = true)
            }

            var linked = 0
            for (producto in unlinked) {
                try {
                    val rows = supabase.from("global_products")
                        .select { filter { eq("ean", producto.codigoBarras!!) } }
                        .decodeList<GlobalProductRow>()

                    if (rows.isNotEmpty()) {
                        val globalId = rows.first().id
                        productoDao.update(
                            producto.copy(
                                globalProductId = globalId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        linked++
                        Log.d(TAG, "Linked producto '${producto.nombre}' -> $globalId")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error linking producto '${producto.nombre}': ${e.message}")
                }
            }

            Log.d(TAG, "linkProducts: $linked/${unlinked.size} linked")
            SyncResult(success = true, pulledCount = linked)
        } catch (e: Exception) {
            Log.e(TAG, "linkProducts failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error vinculando productos: ${e.message}"))
        }
    }

    /**
     * For each local store without a globalStoreId,
     * match by name against global_stores. If found, save the UUID locally.
     */
    private suspend fun linkStores(): SyncResult {
        return try {
            val allStores = tiendaDao.getAllOnce()
            val unlinked = allStores.filter { it.globalStoreId == null }

            if (unlinked.isEmpty()) {
                return SyncResult(success = true)
            }

            var linked = 0
            for (tienda in unlinked) {
                try {
                    val rows = supabase.from("global_stores")
                        .select { filter { ilike("name", tienda.nombre) } }
                        .decodeList<GlobalStoreRow>()

                    if (rows.isNotEmpty()) {
                        val globalId = rows.first().id
                        tiendaDao.update(
                            tienda.copy(
                                globalStoreId = globalId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        linked++
                        Log.d(TAG, "Linked tienda '${tienda.nombre}' -> $globalId")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error linking tienda '${tienda.nombre}': ${e.message}")
                }
            }

            Log.d(TAG, "linkStores: $linked/${unlinked.size} linked")
            SyncResult(success = true, pulledCount = linked)
        } catch (e: Exception) {
            Log.e(TAG, "linkStores failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error vinculando tiendas: ${e.message}"))
        }
    }

    /**
     * Push user_product_aliases for all linked products that have
     * an alias, factor_merma > 0, or notes.
     */
    private suspend fun pushProductAliases(userId: String): SyncResult {
        return try {
            val allProducts = productoDao.getAllOnce()
            val linked = allProducts.filter { it.globalProductId != null }

            val toUpsert = linked
                .filter { it.alias != null || it.factorMerma > 0 || it.notas != null }
                .map { p ->
                    UserProductAliasDto(
                        userId = userId,
                        globalProductId = p.globalProductId!!,
                        alias = p.alias,
                        factorMerma = p.factorMerma,
                        notes = p.notas
                    )
                }

            if (toUpsert.isEmpty()) {
                return SyncResult(success = true)
            }

            supabase.from("user_product_aliases").upsert(toUpsert) {
                onConflict = "user_id,global_product_id"
            }

            Log.d(TAG, "pushProductAliases: ${toUpsert.size} upserted")
            SyncResult(success = true, pushedCount = toUpsert.size)
        } catch (e: Exception) {
            Log.e(TAG, "pushProductAliases failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error subiendo alias de productos: ${e.message}"))
        }
    }

    /**
     * Push user_store_aliases for all linked stores.
     */
    private suspend fun pushStoreAliases(userId: String): SyncResult {
        return try {
            val allStores = tiendaDao.getAllOnce()
            val toUpsert = allStores
                .filter { it.globalStoreId != null }
                .map { t ->
                    UserStoreAliasDto(
                        userId = userId,
                        globalStoreId = t.globalStoreId!!,
                        alias = t.nombre
                    )
                }

            if (toUpsert.isEmpty()) {
                return SyncResult(success = true)
            }

            supabase.from("user_store_aliases").upsert(toUpsert) {
                onConflict = "user_id,global_store_id"
            }

            Log.d(TAG, "pushStoreAliases: ${toUpsert.size} upserted")
            SyncResult(success = true, pushedCount = toUpsert.size)
        } catch (e: Exception) {
            Log.e(TAG, "pushStoreAliases failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error subiendo alias de tiendas: ${e.message}"))
        }
    }
}
