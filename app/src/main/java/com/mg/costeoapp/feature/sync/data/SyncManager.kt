package com.mg.costeoapp.feature.sync.data

import android.util.Log
import com.mg.costeoapp.core.database.dao.PlatoComponenteDao
import com.mg.costeoapp.core.database.dao.PlatoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoDao
import com.mg.costeoapp.core.database.dao.PrefabricadoIngredienteDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.ProductoTiendaDao
import com.mg.costeoapp.core.database.dao.SyncMetadataDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.Plato
import com.mg.costeoapp.core.database.entity.PlatoComponente
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.entity.ProductoTienda
import kotlinx.coroutines.launch
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
    val ean: String,
    val nombre: String? = null,
    @SerialName("unidad_medida") val unidadMedida: String? = null,
    @SerialName("cantidad_por_empaque") val cantidadPorEmpaque: Double? = null,
    @SerialName("unidades_por_empaque") val unidadesPorEmpaque: Int? = null,
    val marca: String? = null,
    @SerialName("imagen_url") val imagenUrl: String? = null
)

@Serializable
private data class GlobalStoreRow(
    val id: String,
    val nombre: String
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
private data class ProductPriceRow(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("store_name") val storeName: String,
    val price: Long,
    @SerialName("is_available") val isAvailable: Boolean = true
)

@Serializable
private data class UserProductAliasWithProduct(
    val id: String,
    @SerialName("product_id") val productId: String,
    val alias: String? = null,
    @SerialName("factor_merma") val factorMerma: Int = 0,
    val notas: String? = null,
    @SerialName("global_products") val product: GlobalProductRow? = null
)

@Serializable
private data class UserStoreAliasDto(
    @SerialName("user_id") val userId: String,
    @SerialName("store_id") val globalStoreId: String,
    val alias: String,
    val activo: Boolean = true
)

@Serializable
private data class UserRecipeDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerialName("costo_fijo") val costoFijo: Long = 0,
    @SerialName("rendimiento_porciones") val rendimientoPorciones: Double = 1.0,
    val activo: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class UserRecipeIngredientDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("cantidad_usada") val cantidadUsada: Double,
    @SerialName("unidad_usada") val unidadUsada: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class UserDishDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerialName("margen_porcentaje") val margenPorcentaje: Double? = null,
    @SerialName("precio_venta_manual") val precioVentaManual: Long? = null,
    val activo: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class UserDishComponentDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("dish_id") val dishId: String,
    @SerialName("recipe_id") val recipeId: String? = null,
    @SerialName("product_id") val productId: String? = null,
    val cantidad: Double,
    val notas: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
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
    private val productoTiendaDao: ProductoTiendaDao,
    private val tiendaDao: TiendaDao,
    private val prefabricadoDao: PrefabricadoDao,
    private val prefabricadoIngredienteDao: PrefabricadoIngredienteDao,
    private val platoDao: PlatoDao,
    private val platoComponenteDao: PlatoComponenteDao
) {
    private val bgScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
    )

    companion object {
        private const val TAG = "SyncManager"
    }

    fun pushInBackground(delayMs: Long = 0) {
        bgScope.launch {
            try {
                if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
                val userId = supabase.auth.currentSessionOrNull()?.user?.id
                if (userId == null) {
                    Log.d(TAG, "Auto-push: no session, skipping")
                    return@launch
                }
                val result = pushAll(userId)
                Log.d(TAG, "Auto-push: pushed=${result.pushedCount}, errors=${result.errors}")
            } catch (e: Exception) {
                Log.w(TAG, "Auto-push failed: ${e.message}")
            }
        }
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
        result = result + pushRecipes(userId)
        result = result + pushDishes(userId)
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
        result = result + pushRecipes(userId)
        result = result + pushDishes(userId)
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
        result = result + pullRecipes(userId)
        result = result + pullDishes(userId)
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
                        .select { filter { ilike("nombre", "%${tienda.nombre}%") } }
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
     * Pull user's products from Supabase and create local Room entries.
     * Called after login to restore the user's catalog.
     */
    suspend fun pullUserData(userId: String): SyncResult {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            return SyncResult(success = false, errors = listOf("No hay sesion activa"))
        }

        return try {
            val aliases = supabase.from("user_product_aliases")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserProductAliasWithProduct>()

            if (aliases.isEmpty()) {
                Log.d(TAG, "pullUserData: no aliases found for user")
                return SyncResult(success = true)
            }

            // Get all product IDs from aliases
            val productIds = aliases.mapNotNull { it.productId }
            if (productIds.isEmpty()) return SyncResult(success = true)

            // Fetch global products
            val globalProducts = supabase.from("global_products")
                .select { filter { isIn("id", productIds) } }
                .decodeList<GlobalProductRow>()

            val existingProducts = productoDao.getAllOnce()
            val existingEans = existingProducts.mapNotNull { it.codigoBarras }.toSet()

            var created = 0
            for (gp in globalProducts) {
                if (gp.ean in existingEans) {
                    // Already exists locally, just link globalProductId
                    val local = existingProducts.first { it.codigoBarras == gp.ean }
                    if (local.globalProductId == null) {
                        productoDao.update(local.copy(globalProductId = gp.id, updatedAt = System.currentTimeMillis()))
                    }
                    continue
                }

                val alias = aliases.find { it.productId == gp.id }
                val producto = com.mg.costeoapp.core.database.entity.Producto(
                    nombre = gp.nombre ?: "Producto ${gp.ean}",
                    codigoBarras = gp.ean,
                    unidadMedida = gp.unidadMedida ?: "unidad",
                    cantidadPorEmpaque = gp.cantidadPorEmpaque ?: 1.0,
                    unidadesPorEmpaque = gp.unidadesPorEmpaque ?: 1,
                    factorMerma = alias?.factorMerma ?: 0,
                    notas = alias?.notas,
                    alias = alias?.alias,
                    globalProductId = gp.id
                )
                productoDao.insert(producto)
                created++
                Log.d(TAG, "Created local producto from cloud: ${gp.nombre} (${gp.ean})")
            }

            // Fetch prices for all pulled products
            var pricesCreated = 0
            if (productIds.isNotEmpty()) {
                try {
                    val prices = supabase.from("product_prices")
                        .select { filter { isIn("product_id", productIds) } }
                        .decodeList<ProductPriceRow>()

                    val allLocalProducts = productoDao.getAllOnce()
                    val allLocalStores = tiendaDao.getAllOnce()

                    for (pp in prices) {
                        if (!pp.isAvailable || pp.price <= 0) continue
                        val gp = globalProducts.find { it.id == pp.productId } ?: continue
                        val localProduct = allLocalProducts.find { it.codigoBarras == gp.ean } ?: continue
                        val localStore = allLocalStores.find {
                            it.nombre.contains(pp.storeName.take(8), ignoreCase = true) ||
                            pp.storeName.contains(it.nombre.take(8), ignoreCase = true)
                        } ?: continue

                        val existingPrice = productoTiendaDao.getPrecioActivo(localProduct.id, localStore.id)
                        if (existingPrice == null) {
                            productoTiendaDao.insert(ProductoTienda(
                                productoId = localProduct.id,
                                tiendaId = localStore.id,
                                precio = pp.price
                            ))
                            pricesCreated++
                            Log.d(TAG, "Created price: ${gp.nombre} @ ${pp.storeName} = ${pp.price}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching prices: ${e.message}")
                }
            }

            Log.d(TAG, "pullUserData: $created productos, $pricesCreated precios from ${aliases.size} aliases")
            var result = SyncResult(success = true, pulledCount = created + pricesCreated)
            result = result + pullRecipes(userId)
            result = result + pullDishes(userId)
            result
        } catch (e: Exception) {
            Log.e(TAG, "pullUserData failed: ${e.message}", e)
            SyncResult(success = false, errors = listOf("Error descargando datos: ${e.message}"))
        }
    }

    /**
     * Push user_store_aliases for all linked stores.
     */
    private suspend fun pushStoreAliases(userId: String): SyncResult {
        return try {
            val allStores = tiendaDao.getAllIncludingInactiveOnce()
            val toUpsert = allStores
                .filter { it.globalStoreId != null }
                .map { t ->
                    UserStoreAliasDto(
                        userId = userId,
                        globalStoreId = t.globalStoreId!!,
                        alias = t.nombre,
                        activo = t.activo
                    )
                }

            if (toUpsert.isEmpty()) {
                return SyncResult(success = true)
            }

            supabase.from("user_store_aliases").upsert(toUpsert) {
                onConflict = "user_id,store_id"
            }

            Log.d(TAG, "pushStoreAliases: ${toUpsert.size} upserted")
            SyncResult(success = true, pushedCount = toUpsert.size)
        } catch (e: Exception) {
            Log.e(TAG, "pushStoreAliases failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error subiendo alias de tiendas: ${e.message}"))
        }
    }

    /**
     * Build a map of local product Long ID -> global UUID for all linked products.
     */
    private suspend fun buildProductIdMap(): Map<Long, String> {
        return productoDao.getAllOnce()
            .filter { it.globalProductId != null }
            .associate { it.id to it.globalProductId!! }
    }

    /**
     * Build a reverse map of global product UUID -> local Long ID.
     */
    private suspend fun buildProductUuidToLocalMap(): Map<String, Long> {
        return productoDao.getAllOnce()
            .filter { it.globalProductId != null }
            .associate { it.globalProductId!! to it.id }
    }

    /**
     * Push all local recipes (prefabricados) and their ingredients to Supabase.
     * Uses recipe name + user_id as conflict key for upsert.
     */
    private suspend fun pushRecipes(userId: String): SyncResult {
        return try {
            val allRecipes = prefabricadoDao.getAllOnce()
            if (allRecipes.isEmpty()) return SyncResult(success = true)

            val productIdMap = buildProductIdMap()
            var pushed = 0

            for (recipe in allRecipes) {
                try {
                    val recipeDto = UserRecipeDto(
                        userId = userId,
                        nombre = recipe.nombre,
                        descripcion = recipe.descripcion,
                        costoFijo = recipe.costoFijo,
                        rendimientoPorciones = recipe.rendimientoPorciones,
                        activo = recipe.activo
                    )

                    supabase.from("user_recipes").upsert(recipeDto) {
                        onConflict = "user_id,nombre"
                    }

                    val insertedRecipes = supabase.from("user_recipes")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("nombre", recipe.nombre)
                            }
                        }
                        .decodeList<UserRecipeDto>()

                    val remoteRecipeId = insertedRecipes.firstOrNull()?.id ?: continue

                    val ingredients = prefabricadoIngredienteDao.getAllOnce()
                        .filter { it.prefabricadoId == recipe.id }

                    if (ingredients.isNotEmpty()) {
                        supabase.from("user_recipe_ingredients")
                            .delete {
                                filter {
                                    eq("user_id", userId)
                                    eq("recipe_id", remoteRecipeId)
                                }
                            }

                        val ingredientDtos = ingredients.mapNotNull { ing ->
                            val globalProductId = productIdMap[ing.productoId] ?: return@mapNotNull null
                            UserRecipeIngredientDto(
                                userId = userId,
                                recipeId = remoteRecipeId,
                                productId = globalProductId,
                                cantidadUsada = ing.cantidadUsada,
                                unidadUsada = ing.unidadUsada
                            )
                        }

                        if (ingredientDtos.isNotEmpty()) {
                            supabase.from("user_recipe_ingredients").insert(ingredientDtos)
                        }
                    }

                    pushed++
                    Log.d(TAG, "Pushed recipe '${recipe.nombre}' with ${ingredients.size} ingredients")
                } catch (e: Exception) {
                    Log.w(TAG, "Error pushing recipe '${recipe.nombre}': ${e.message}")
                }
            }

            Log.d(TAG, "pushRecipes: $pushed/${allRecipes.size} pushed")
            SyncResult(success = true, pushedCount = pushed)
        } catch (e: Exception) {
            Log.e(TAG, "pushRecipes failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error subiendo recetas: ${e.message}"))
        }
    }

    /**
     * Push all local dishes (platos) and their components to Supabase.
     */
    private suspend fun pushDishes(userId: String): SyncResult {
        return try {
            val allDishes = platoDao.getAllPlatos()
            if (allDishes.isEmpty()) return SyncResult(success = true)

            val productIdMap = buildProductIdMap()

            val allRecipesLocal = prefabricadoDao.getAllOnce()
            val remoteRecipes = supabase.from("user_recipes")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserRecipeDto>()
            val recipeNameToUuid = remoteRecipes.associate { it.nombre to it.id }

            var pushed = 0

            for (dish in allDishes) {
                try {
                    val dishDto = UserDishDto(
                        userId = userId,
                        nombre = dish.nombre,
                        descripcion = dish.descripcion,
                        margenPorcentaje = dish.margenPorcentaje,
                        precioVentaManual = dish.precioVentaManual,
                        activo = dish.activo
                    )

                    supabase.from("user_dishes").upsert(dishDto) {
                        onConflict = "user_id,nombre"
                    }

                    val insertedDishes = supabase.from("user_dishes")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("nombre", dish.nombre)
                            }
                        }
                        .decodeList<UserDishDto>()

                    val remoteDishId = insertedDishes.firstOrNull()?.id ?: continue

                    val components = platoComponenteDao.getAllOnce()
                        .filter { it.platoId == dish.id }

                    if (components.isNotEmpty()) {
                        supabase.from("user_dish_components")
                            .delete {
                                filter {
                                    eq("user_id", userId)
                                    eq("dish_id", remoteDishId)
                                }
                            }

                        val componentDtos = components.mapNotNull { comp ->
                            val globalProductId = comp.productoId?.let { productIdMap[it] }
                            val recipeUuid = comp.prefabricadoId?.let { prefId ->
                                val localRecipe = allRecipesLocal.find { it.id == prefId }
                                localRecipe?.let { recipeNameToUuid[it.nombre] }
                            }

                            if (globalProductId == null && recipeUuid == null) return@mapNotNull null

                            UserDishComponentDto(
                                userId = userId,
                                dishId = remoteDishId,
                                recipeId = recipeUuid,
                                productId = globalProductId,
                                cantidad = comp.cantidad,
                                notas = comp.notas
                            )
                        }

                        if (componentDtos.isNotEmpty()) {
                            supabase.from("user_dish_components").insert(componentDtos)
                        }
                    }

                    pushed++
                    Log.d(TAG, "Pushed dish '${dish.nombre}' with ${components.size} components")
                } catch (e: Exception) {
                    Log.w(TAG, "Error pushing dish '${dish.nombre}': ${e.message}")
                }
            }

            Log.d(TAG, "pushDishes: $pushed/${allDishes.size} pushed")
            SyncResult(success = true, pushedCount = pushed)
        } catch (e: Exception) {
            Log.e(TAG, "pushDishes failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error subiendo platos: ${e.message}"))
        }
    }

    /**
     * Pull user's recipes and their ingredients from Supabase into local Room.
     * Skips recipes that already exist locally (matched by name).
     */
    private suspend fun pullRecipes(userId: String): SyncResult {
        return try {
            val remoteRecipes = supabase.from("user_recipes")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserRecipeDto>()

            if (remoteRecipes.isEmpty()) {
                Log.d(TAG, "pullRecipes: no remote recipes found")
                return SyncResult(success = true)
            }

            val existingRecipes = prefabricadoDao.getAllOnce()
            val existingNames = existingRecipes.map { it.nombre.lowercase() }.toSet()
            val productUuidToLocal = buildProductUuidToLocalMap()

            var created = 0
            var ingredientsCreated = 0

            val remoteRecipeUuidToLocalId = mutableMapOf<String, Long>()

            for (remote in remoteRecipes) {
                try {
                    if (remote.nombre.lowercase() in existingNames) {
                        val existing = existingRecipes.first { it.nombre.equals(remote.nombre, ignoreCase = true) }
                        remote.id?.let { remoteRecipeUuidToLocalId[it] = existing.id }

                        if (!existing.activo && remote.activo) {
                            prefabricadoDao.restore(existing.id)
                        } else if (existing.activo && !remote.activo) {
                            prefabricadoDao.softDelete(existing.id)
                        }
                        continue
                    }

                    val localId = prefabricadoDao.insert(
                        Prefabricado(
                            nombre = remote.nombre,
                            descripcion = remote.descripcion,
                            costoFijo = remote.costoFijo,
                            rendimientoPorciones = remote.rendimientoPorciones,
                            activo = remote.activo
                        )
                    )

                    remote.id?.let { remoteRecipeUuidToLocalId[it] = localId }
                    created++
                    Log.d(TAG, "Created local recipe from cloud: ${remote.nombre}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error pulling recipe '${remote.nombre}': ${e.message}")
                }
            }

            for (remote in remoteRecipes) {
                val remoteId = remote.id ?: continue
                val localRecipeId = remoteRecipeUuidToLocalId[remoteId] ?: continue

                try {
                    val remoteIngredients = supabase.from("user_recipe_ingredients")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("recipe_id", remoteId)
                            }
                        }
                        .decodeList<UserRecipeIngredientDto>()

                    if (remoteIngredients.isEmpty()) continue

                    val existingIngredients = prefabricadoIngredienteDao.getAllOnce()
                        .filter { it.prefabricadoId == localRecipeId }
                    val existingProductIds = existingIngredients.map { it.productoId }.toSet()

                    val newIngredients = remoteIngredients.mapNotNull { ri ->
                        val localProductId = productUuidToLocal[ri.productId] ?: return@mapNotNull null
                        if (localProductId in existingProductIds) return@mapNotNull null

                        PrefabricadoIngrediente(
                            prefabricadoId = localRecipeId,
                            productoId = localProductId,
                            cantidadUsada = ri.cantidadUsada,
                            unidadUsada = ri.unidadUsada
                        )
                    }

                    if (newIngredients.isNotEmpty()) {
                        prefabricadoIngredienteDao.insertAll(newIngredients)
                        ingredientsCreated += newIngredients.size
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error pulling ingredients for recipe '$remoteId': ${e.message}")
                }
            }

            Log.d(TAG, "pullRecipes: $created recetas, $ingredientsCreated ingredientes")
            SyncResult(success = true, pulledCount = created + ingredientsCreated)
        } catch (e: Exception) {
            Log.e(TAG, "pullRecipes failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error descargando recetas: ${e.message}"))
        }
    }

    /**
     * Pull user's dishes and their components from Supabase into local Room.
     * Skips dishes that already exist locally (matched by name).
     * Must be called after pullRecipes so recipe mappings are available.
     */
    private suspend fun pullDishes(userId: String): SyncResult {
        return try {
            val remoteDishes = supabase.from("user_dishes")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserDishDto>()

            if (remoteDishes.isEmpty()) {
                Log.d(TAG, "pullDishes: no remote dishes found")
                return SyncResult(success = true)
            }

            val existingDishes = platoDao.getAllPlatos()
            val existingNames = existingDishes.map { it.nombre.lowercase() }.toSet()
            val productUuidToLocal = buildProductUuidToLocalMap()

            val remoteRecipes = supabase.from("user_recipes")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserRecipeDto>()
            val allLocalRecipes = prefabricadoDao.getAllOnce()
            val recipeUuidToLocalId = mutableMapOf<String, Long>()
            for (rr in remoteRecipes) {
                val localMatch = allLocalRecipes.find { it.nombre.equals(rr.nombre, ignoreCase = true) }
                if (localMatch != null && rr.id != null) {
                    recipeUuidToLocalId[rr.id] = localMatch.id
                }
            }

            var created = 0
            var componentsCreated = 0

            val remoteDishUuidToLocalId = mutableMapOf<String, Long>()

            for (remote in remoteDishes) {
                try {
                    if (remote.nombre.lowercase() in existingNames) {
                        val existing = existingDishes.first { it.nombre.equals(remote.nombre, ignoreCase = true) }
                        remote.id?.let { remoteDishUuidToLocalId[it] = existing.id }

                        if (!existing.activo && remote.activo) {
                            platoDao.restore(existing.id)
                        } else if (existing.activo && !remote.activo) {
                            platoDao.softDelete(existing.id)
                        }
                        continue
                    }

                    val localId = platoDao.insert(
                        Plato(
                            nombre = remote.nombre,
                            descripcion = remote.descripcion,
                            margenPorcentaje = remote.margenPorcentaje,
                            precioVentaManual = remote.precioVentaManual,
                            activo = remote.activo
                        )
                    )

                    remote.id?.let { remoteDishUuidToLocalId[it] = localId }
                    created++
                    Log.d(TAG, "Created local dish from cloud: ${remote.nombre}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error pulling dish '${remote.nombre}': ${e.message}")
                }
            }

            for (remote in remoteDishes) {
                val remoteId = remote.id ?: continue
                val localDishId = remoteDishUuidToLocalId[remoteId] ?: continue

                try {
                    val remoteComponents = supabase.from("user_dish_components")
                        .select {
                            filter {
                                eq("user_id", userId)
                                eq("dish_id", remoteId)
                            }
                        }
                        .decodeList<UserDishComponentDto>()

                    if (remoteComponents.isEmpty()) continue

                    val existingComponents = platoComponenteDao.getAllOnce()
                        .filter { it.platoId == localDishId }

                    if (existingComponents.isNotEmpty()) continue

                    val newComponents = remoteComponents.mapNotNull { rc ->
                        val localProductId = rc.productId?.let { productUuidToLocal[it] }
                        val localRecipeId = rc.recipeId?.let { recipeUuidToLocalId[it] }

                        if (localProductId == null && localRecipeId == null) return@mapNotNull null

                        PlatoComponente(
                            platoId = localDishId,
                            prefabricadoId = localRecipeId,
                            productoId = localProductId,
                            cantidad = rc.cantidad,
                            notas = rc.notas
                        )
                    }

                    if (newComponents.isNotEmpty()) {
                        platoComponenteDao.insertAll(newComponents)
                        componentsCreated += newComponents.size
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error pulling components for dish '$remoteId': ${e.message}")
                }
            }

            Log.d(TAG, "pullDishes: $created platos, $componentsCreated componentes")
            SyncResult(success = true, pulledCount = created + componentsCreated)
        } catch (e: Exception) {
            Log.e(TAG, "pullDishes failed: ${e.message}")
            SyncResult(success = false, errors = listOf("Error descargando platos: ${e.message}"))
        }
    }
}
