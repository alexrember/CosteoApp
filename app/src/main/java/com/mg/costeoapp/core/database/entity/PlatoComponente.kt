package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plato_componente",
    foreignKeys = [
        ForeignKey(
            entity = Plato::class,
            parentColumns = ["id"],
            childColumns = ["plato_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Prefabricado::class,
            parentColumns = ["id"],
            childColumns = ["prefabricado_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("plato_id"),
        Index("prefabricado_id"),
        Index("producto_id")
    ]
)
data class PlatoComponente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "plato_id")
    val platoId: Long,

    @ColumnInfo(name = "prefabricado_id")
    val prefabricadoId: Long? = null,

    @ColumnInfo(name = "producto_id")
    val productoId: Long? = null,

    val cantidad: Double,

    val notas: String? = null
)
