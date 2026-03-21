package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prefabricado_ingrediente",
    foreignKeys = [
        ForeignKey(
            entity = Prefabricado::class,
            parentColumns = ["id"],
            childColumns = ["prefabricado_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["prefabricado_id", "producto_id"], unique = true),
        Index("producto_id")
    ]
)
data class PrefabricadoIngrediente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "prefabricado_id")
    val prefabricadoId: Long,

    @ColumnInfo(name = "producto_id")
    val productoId: Long,

    @ColumnInfo(name = "cantidad_usada")
    val cantidadUsada: Double,

    @ColumnInfo(name = "unidad_usada")
    val unidadUsada: String,

    @ColumnInfo(name = "version", defaultValue = "1")
    val version: Int = 1
)
