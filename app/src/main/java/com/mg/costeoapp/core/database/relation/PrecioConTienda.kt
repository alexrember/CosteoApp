package com.mg.costeoapp.core.database.relation

import com.mg.costeoapp.core.database.entity.ProductoTienda

data class PrecioConTienda(
    val productoTienda: ProductoTienda,
    val tiendaNombre: String
)
