package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.core.database.dao.CarritoTemporalDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.CarritoTemporal
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.feature.inventario.ui.CarritoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompraManager @Inject constructor(
    private val carritoDao: CarritoTemporalDao,
    private val productoDao: ProductoDao,
    private val tiendaDao: TiendaDao
) {
    private val _tienda = MutableStateFlow<Tienda?>(null)
    val tienda: StateFlow<Tienda?> = _tienda.asStateFlow()

    private val _items = MutableStateFlow<List<CarritoItem>>(emptyList())
    val items: StateFlow<List<CarritoItem>> = _items.asStateFlow()

    val itemCount: Int get() = _items.value.size

    suspend fun restaurarDesdeDb() {
        val tiendaId = carritoDao.getTiendaId()
        if (tiendaId != null) {
            _tienda.value = tiendaDao.getById(tiendaId)
            val dbItems = carritoDao.getAll()
            _items.value = dbItems.mapNotNull { row ->
                val producto = productoDao.getById(row.productoId) ?: return@mapNotNull null
                CarritoItem(
                    producto = producto,
                    cantidad = row.cantidad,
                    precioUnitario = row.precioUnitario
                )
            }
        }
    }

    suspend fun iniciarCompra(tienda: Tienda) {
        _tienda.value = tienda
        _items.value = emptyList()
        carritoDao.deleteAll()
    }

    suspend fun agregarProducto(producto: Producto, cantidad: Double, precioUnitario: Long) {
        _items.update { currentItems ->
            val existingIndex = currentItems.indexOfFirst { it.producto.id == producto.id }
            if (existingIndex >= 0) {
                currentItems.toMutableList().apply {
                    this[existingIndex] = this[existingIndex].copy(
                        cantidad = this[existingIndex].cantidad + cantidad
                    )
                }
            } else {
                currentItems + CarritoItem(producto = producto, cantidad = cantidad, precioUnitario = precioUnitario)
            }
        }
        // Sync to DB
        val dbItem = carritoDao.getByProductoId(producto.id)
        if (dbItem != null) {
            val newItem = _items.value.find { it.producto.id == producto.id }
            newItem?.let { carritoDao.updateCantidad(dbItem.id, it.cantidad) }
        } else {
            carritoDao.insert(CarritoTemporal(
                productoId = producto.id,
                tiendaId = _tienda.value?.id ?: return,
                cantidad = cantidad,
                precioUnitario = precioUnitario
            ))
        }
    }

    suspend fun aumentarCantidad(index: Int) {
        val productoId = _items.value[index].producto.id
        _items.update { currentItems ->
            currentItems.toMutableList().apply {
                this[index] = this[index].copy(cantidad = this[index].cantidad + 1)
            }
        }
        carritoDao.getByProductoId(productoId)?.let {
            carritoDao.updateCantidad(it.id, _items.value[index].cantidad)
        }
    }

    suspend fun disminuirCantidad(index: Int): Boolean {
        if (_items.value[index].cantidad <= 1) return true
        val productoId = _items.value[index].producto.id
        _items.update { currentItems ->
            currentItems.toMutableList().apply {
                this[index] = this[index].copy(cantidad = this[index].cantidad - 1)
            }
        }
        carritoDao.getByProductoId(productoId)?.let {
            carritoDao.updateCantidad(it.id, _items.value[index].cantidad)
        }
        return false
    }

    suspend fun removerItem(index: Int) {
        val productoId = _items.value[index].producto.id
        _items.update { it.toMutableList().apply { removeAt(index) } }
        carritoDao.getByProductoId(productoId)?.let { carritoDao.delete(it.id) }
    }

    suspend fun limpiar() {
        _tienda.value = null
        _items.value = emptyList()
        carritoDao.deleteAll()
    }

    fun getItems(): List<CarritoItem> = _items.value
    fun getTienda(): Tienda? = _tienda.value
    fun hayCompraEnCurso(): Boolean = _items.value.isNotEmpty() || _tienda.value != null
}
