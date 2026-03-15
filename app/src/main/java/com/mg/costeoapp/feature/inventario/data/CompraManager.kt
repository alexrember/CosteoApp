package com.mg.costeoapp.feature.inventario.data

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
class CompraManager @Inject constructor() {

    private val _tienda = MutableStateFlow<Tienda?>(null)
    val tienda: StateFlow<Tienda?> = _tienda.asStateFlow()

    private val _items = MutableStateFlow<List<CarritoItem>>(emptyList())
    val items: StateFlow<List<CarritoItem>> = _items.asStateFlow()

    val itemCount: Int get() = _items.value.size

    fun iniciarCompra(tienda: Tienda) {
        _tienda.value = tienda
        _items.value = emptyList()
    }

    fun agregarProducto(producto: Producto, cantidad: Double, precioUnitario: Long) {
        _items.update { currentItems ->
            val existingIndex = currentItems.indexOfFirst { it.producto.id == producto.id }
            if (existingIndex >= 0) {
                // Ya existe → aumentar cantidad
                currentItems.toMutableList().apply {
                    val existing = this[existingIndex]
                    this[existingIndex] = existing.copy(cantidad = existing.cantidad + cantidad)
                }
            } else {
                currentItems + CarritoItem(
                    producto = producto,
                    cantidad = cantidad,
                    precioUnitario = precioUnitario
                )
            }
        }
    }

    fun removerItem(index: Int) {
        _items.update { it.toMutableList().apply { removeAt(index) } }
    }

    fun limpiar() {
        _tienda.value = null
        _items.value = emptyList()
    }

    fun getItems(): List<CarritoItem> = _items.value

    fun getTienda(): Tienda? = _tienda.value
}
