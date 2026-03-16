package com.mg.costeoapp.feature.inventario.data

import com.mg.costeoapp.core.database.dao.CarritoTemporalDao
import com.mg.costeoapp.core.database.dao.ProductoDao
import com.mg.costeoapp.core.database.dao.TiendaDao
import com.mg.costeoapp.core.database.entity.CarritoTemporal
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.Tienda
import com.mg.costeoapp.feature.inventario.ui.CarritoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompraManager @Inject constructor(
    private val carritoDao: CarritoTemporalDao,
    private val productoDao: ProductoDao,
    private val tiendaDao: TiendaDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tienda = MutableStateFlow<Tienda?>(null)
    val tienda: StateFlow<Tienda?> = _tienda.asStateFlow()

    private val _items = MutableStateFlow<List<CarritoItem>>(emptyList())
    val items: StateFlow<List<CarritoItem>> = _items.asStateFlow()

    val itemCount: Int get() = _items.value.size

    init {
        scope.launch { restaurarDesdeDb() }
    }

    private suspend fun restaurarDesdeDb() {
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

    fun iniciarCompra(tienda: Tienda) {
        _tienda.value = tienda
        _items.value = emptyList()
        scope.launch { carritoDao.deleteAll() }
    }

    fun agregarProducto(producto: Producto, cantidad: Double, precioUnitario: Long) {
        val currentItems = _items.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.producto.id == producto.id }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            val newCantidad = existing.cantidad + cantidad
            currentItems[existingIndex] = existing.copy(cantidad = newCantidad)
            _items.value = currentItems
            scope.launch { syncItemToDb(producto.id, newCantidad) }
        } else {
            currentItems.add(CarritoItem(producto = producto, cantidad = cantidad, precioUnitario = precioUnitario))
            _items.value = currentItems
            scope.launch {
                carritoDao.insert(CarritoTemporal(
                    productoId = producto.id,
                    tiendaId = _tienda.value?.id ?: 0,
                    cantidad = cantidad,
                    precioUnitario = precioUnitario
                ))
            }
        }
    }

    fun aumentarCantidad(index: Int) {
        val currentItems = _items.value.toMutableList()
        val item = currentItems[index]
        val newCantidad = item.cantidad + 1
        currentItems[index] = item.copy(cantidad = newCantidad)
        _items.value = currentItems
        scope.launch { syncItemToDb(item.producto.id, newCantidad) }
    }

    fun disminuirCantidad(index: Int): Boolean {
        val item = _items.value[index]
        if (item.cantidad <= 1) return true

        val currentItems = _items.value.toMutableList()
        val newCantidad = item.cantidad - 1
        currentItems[index] = item.copy(cantidad = newCantidad)
        _items.value = currentItems
        scope.launch { syncItemToDb(item.producto.id, newCantidad) }
        return false
    }

    fun removerItem(index: Int) {
        val productoId = _items.value[index].producto.id
        _items.value = _items.value.toMutableList().apply { removeAt(index) }
        scope.launch { deleteItemFromDb(productoId) }
    }

    fun limpiar() {
        _tienda.value = null
        _items.value = emptyList()
        scope.launch { carritoDao.deleteAll() }
    }

    fun getItems(): List<CarritoItem> = _items.value
    fun getTienda(): Tienda? = _tienda.value
    fun hayCompraEnCurso(): Boolean = _items.value.isNotEmpty()

    private suspend fun syncItemToDb(productoId: Long, cantidad: Double) {
        val dbItems = carritoDao.getAll()
        val dbItem = dbItems.find { it.productoId == productoId }
        dbItem?.let { carritoDao.updateCantidad(it.id, cantidad) }
    }

    private suspend fun deleteItemFromDb(productoId: Long) {
        val dbItems = carritoDao.getAll()
        val dbItem = dbItems.find { it.productoId == productoId }
        dbItem?.let { carritoDao.delete(it.id) }
    }
}
