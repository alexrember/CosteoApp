package com.mg.costeoapp.feature.productos.data

import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda
import com.mg.costeoapp.core.database.relation.PrecioConTienda
import com.mg.costeoapp.core.database.relation.ProductoConPrecios
import kotlinx.coroutines.flow.Flow

interface ProductoRepository {
    fun getAll(): Flow<List<Producto>>
    suspend fun getById(id: Long): Producto?
    fun search(query: String): Flow<List<Producto>>
    suspend fun getByCodigoBarras(codigoBarras: String): Producto?
    suspend fun getProductoConPrecios(id: Long): ProductoConPrecios?
    fun getPreciosConTienda(productoId: Long): Flow<List<PrecioConTienda>>
    suspend fun getPrecioMasReciente(productoId: Long): ProductoTienda?
    suspend fun insert(producto: Producto): Result<Long>
    suspend fun update(producto: Producto): Result<Unit>
    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun insertPrecio(productoTienda: ProductoTienda): Result<Long>
    suspend fun updatePrecio(productoTienda: ProductoTienda): Result<Unit>
    suspend fun desactivarPrecios(productoId: Long, tiendaId: Long)
    suspend fun getFrequentProducts(limit: Int = 10): List<Producto>
    suspend fun getLastPrice(productoId: Long): Long?
    suspend fun searchSuggestions(query: String, limit: Int = 5): List<Producto>
}
