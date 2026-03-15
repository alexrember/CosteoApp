package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.relation.ProductoConPrecios
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

	@Query("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre ASC")
	fun getAll(): Flow<List<Producto>>

	@Query("SELECT * FROM productos WHERE id = :id")
	suspend fun getById(id: Long): Producto?

	@Query("SELECT * FROM productos WHERE codigo_barras = :codigoBarras AND activo = 1 LIMIT 1")
	suspend fun getByCodigoBarras(codigoBarras: String): Producto?

	@Query(
		""" 
        SELECT * FROM productos
        WHERE activo = 1 
        AND (LOWER(nombre) LIKE '%' || LOWER(:query) || '%'  
             OR codigo_barras LIKE '%' || :query || '%')
        ORDER BY nombre ASC
    """
	)
	fun search(query: String): Flow<List<Producto>>

	@Transaction
	@Query("SELECT * FROM productos WHERE id = :id")
	suspend fun getProductoConPrecios(id: Long): ProductoConPrecios?

	@Insert(onConflict = OnConflictStrategy.ABORT)
	suspend fun insert(producto: Producto): Long

	@Update
	suspend fun update(producto: Producto)

	@Query("UPDATE productos SET activo = 0, updated_at = :timestamp WHERE id = :id")
	suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

	@Query("UPDATE productos SET activo = 1, updated_at = :timestamp WHERE id = :id")
	suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis())
}
