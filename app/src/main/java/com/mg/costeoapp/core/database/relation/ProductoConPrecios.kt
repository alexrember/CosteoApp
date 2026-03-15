package com.mg.costeoapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mg.costeoapp.core.database.entity.Producto
import com.mg.costeoapp.core.database.entity.ProductoTienda

data class ProductoConPrecios(
    @Embedded
    val producto: Producto,

    @Relation(
        parentColumn = "id",
        entityColumn = "producto_id"
    )
    val precios: List<ProductoTienda>
)
