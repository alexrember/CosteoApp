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
        val currentItems = _items.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.producto.id == producto.id }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            val newCantidad = existing.cantidad + cantidad
            currentItems[existingIndex] = existing.copy(cantidad = newCantidad)
            _items.value = currentItems
            val dbItems = carritoDao.getAll()
            dbItems.find { it.productoId == producto.id }?.let {
                carritoDao.updateCantidad(it.id, newCantidad)
            }
        } else {
            currentItems.add(CarritoItem(producto = producto, cantidad = cantidad, precioUnitario = precioUnitario))
            _items.value = currentItems
            carritoDao.insert(CarritoTemporal(
                productoId = producto.id,
                tiendaId = _tienda.value?.id ?: 0,
                cantidad = cantidad,
                precioUnitario = precioUnitario
            ))
        }
    }

    suspend fun aumentarCantidad(index: Int) {
        val currentItems = _items.value.toMutableList()
        val item = currentItems[index]
        val newCantidad = item.cantidad + 1
        currentItems[index] = item.copy(cantidad = newCantidad)
        _items.value = currentItems
        val dbItems = carritoDao.getAll()
        dbItems.find { it.productoId == item.producto.id }?.let {
            carritoDao.updateCantidad(it.id, newCantidad)
        }
    }

    suspend fun disminuirCantidad(index: Int): Boolean {
        val item = _items.value[index]
        if (item.cantidad <= 1) return true

        val currentItems = _items.value.toMutableList()
        val newCantidad = item.cantidad - 1
        currentItems[index] = item.copy(cantidad = newCantidad)
        _items.value = currentItems
        val dbItems = carritoDao.getAll()
        dbItems.find { it.productoId == item.producto.id }?.let {
            carritoDao.updateCantidad(it.id, newCantidad)
        }
        return false
    }

    suspend fun removerItem(index: Int) {
        val productoId = _items.value[index].producto.id
        _items.value = _items.value.toMutableList().apply { removeAt(index) }
        val dbItems = carritoDao.getAll()
        dbItems.find { it.productoId == productoId }?.let {
            carritoDao.delete(it.id)
        }
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
