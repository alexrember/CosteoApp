package com.mg.costeoapp.feature.sync.data

import android.util.Log
import com.mg.costeoapp.core.database.dao.SyncMetadataDao
import com.mg.costeoapp.core.database.entity.SyncMetadata
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SyncManager v2 — adapted to the new Supabase schema.
 *
 * The old per-user tables (tiendas, productos, producto_tienda, inventario,
 * prefabricados, prefabricado_ingrediente, platos, plato_componente) were
 * dropped.  The new cloud tables are:
 *
 *   Global (read-only): global_products, global_stores, product_prices
 *   Per-user (sync):    user_product_aliases, user_store_aliases,
 *                        user_inventory, user_recipes,
 *                        user_recipe_ingredients, user_dishes,
 *                        user_dish_components
 *
 * Because the local Room DB still uses the old entity structure, a full
 * bidirectional sync requires mapping work that will come when the Room DB
 * is also restructured.  For now, sync is intentionally disabled (no-op)
 * so the app compiles and does not crash.
 */
@Singleton
class SyncManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val syncMetadataDao: SyncMetadataDao
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

        // Sync is temporarily disabled while the Room DB is restructured
        // to match the new Supabase schema (user_* tables).
        Log.d(TAG, "syncAll called — sync temporarily disabled pending Room DB restructure")
        return SyncResult(
            success = true,
            pushedCount = 0,
            pulledCount = 0,
            errors = emptyList()
        )
    }

    suspend fun pushAll(userId: String): SyncResult {
        Log.d(TAG, "pushAll called — sync temporarily disabled pending Room DB restructure")
        return SyncResult(success = true, pushedCount = 0, errors = emptyList())
    }

    suspend fun pullAll(userId: String): SyncResult {
        Log.d(TAG, "pullAll called — sync temporarily disabled pending Room DB restructure")
        return SyncResult(success = true, pulledCount = 0, errors = emptyList())
    }
}
