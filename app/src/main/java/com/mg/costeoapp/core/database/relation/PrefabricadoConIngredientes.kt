package com.mg.costeoapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mg.costeoapp.core.database.entity.Prefabricado
import com.mg.costeoapp.core.database.entity.PrefabricadoIngrediente

data class PrefabricadoConIngredientes(
    @Embedded
    val prefabricado: Prefabricado,

    @Relation(
        entity = PrefabricadoIngrediente::class,
        parentColumn = "id",
        entityColumn = "prefabricado_id"
    )
    val ingredientes: List<IngredienteConProducto>
)
