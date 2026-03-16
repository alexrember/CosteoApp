package com.mg.costeoapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente
import com.mg.costeoapp.core.database.entity.Producto

data class IngredienteConProducto(
    @Embedded
    val ingrediente: PrefabricadoIngrediente,

    @Relation(
        parentColumn = "producto_id",
        entityColumn = "id"
    )
    val producto: Producto
)
